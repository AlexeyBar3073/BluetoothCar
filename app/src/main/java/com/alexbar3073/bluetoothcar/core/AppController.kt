// Файл: core/AppController.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context
import android.net.Uri
import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothConnectionManager
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.data.database.entities.EcuCombinationEntity
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.OutputStream

/**
 * ТЕГ: Главный координатор
 * ФАЙЛ: core/AppController.kt
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * ЦЕНТРАЛЬНЫЙ УЗЕЛ БИЗНЕС-ЛОГИКИ. Единственный «мозг» приложения, принимающий решения.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Управление жизненным циклом BluetoothConnectionManager.
 * 2. Реализация СЦЕНАРИЕВ обмена данными (инициализация после подключения).
 * 3. ПАРСИНГ входящих данных (JsonObject) от BCM и преобразование их в CarData.
 * 4. СИНХРОНИЗАЦИЯ настроек: прием изменений настроек от БК и обновление локального стейта.
 * 5. Хранение и дистрибуция настроек приложения.
 * 6. ФОРМИРОВАНИЕ финального статуса подключения для UI.
 * 7. Инициализация и наполнение справочника ошибок ЭБУ и их комбинаций из JSON.
 * 8. ОБНОВЛЕНИЕ баз данных из внешних пользовательских файлов.
 * 9. ЭКСПОРТ текущего состава баз данных в JSON файлы.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Разделение ответственности: Транспорт (DSH) и Оркестратор (BCM) не знают о бизнес-логике.
 * - Централизация: Вся логика «что и когда отправлять» сосредоточена здесь.
 * - Оптимизация: Получает уже распарсенный JsonObject, исключая двойную десериализацию.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Реактивность через StateFlow и обработка входящего потока сообщений.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        private const val TAG = "AppController"
        /** Путь к файлу со справочником ошибок в assets */
        private const val ECU_ERRORS_JSON_PATH = "ecu_errors.json"
        /** Путь к файлу со справочником комбинаций в assets */
        private const val ECU_COMBINATIONS_JSON_PATH = "ecu_combinations.json"
    }

    /** Область видимости корутин для работы приложения */
    private val appScope = CoroutineScope(Dispatchers.Main + Job())

    /** JSON парсер */
    private val json = Json { 
        ignoreUnknownKeys = true 
        coerceInputValues = true
        prettyPrint = true // Для удобства чтения экспортируемых файлов
    }

    // ========== СОСТОЯНИЯ ==========

    /** Состояние настроек приложения */
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    /** Состояние готовности контроллера */
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** 
     * Внутреннее состояние подключения, управляемое бизнес-логикой.
     * По умолчанию синхронизируется с BCM, но дополняется логическими статусах (REQUESTING_DATA, LISTENING_DATA).
     */
    private val _internalConnectionState = MutableStateFlow<ConnectionState>(ConnectionState.UNDEFINED)

    // ========== BLUETOOTH МЕНЕДЖЕР ==========

    private var _bluetoothConnectionManager: BluetoothConnectionManager? = null
    
    private val bluetoothConnectionManager: BluetoothConnectionManager
        get() = _bluetoothConnectionManager ?: throw IllegalStateException("BluetoothConnectionManager еще не инициализирован.")

    // ========== ПОТОКИ ДАННЫХ ==========

    /** 
     * Поток данных автомобиля.
     * Формируется путем обработки входящих JSON объектов от BCM.
     */
    private val _rawCarDataFlow = MutableSharedFlow<CarData>()
    
    val carData: StateFlow<CarData> = _isInitialized
        .filter { it }
        .flatMapLatest {
            combine(
                _rawCarDataFlow,
                _appSettings,
                connectionStatusInfo
            ) { carDataValue, settings, connectionStatus ->
                if (!connectionStatus.isActive) {
                    CarData() 
                } else {
                    val minFuel = settings.minFuelLevel
                    carDataValue.copy(isFuelLow = carDataValue.fuel < minFuel)
                }
            }
        }
        .stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarData() 
        )

    /** 
     * Поток информации о состоянии подключения для UI.
     * Базируется на внутреннем состоянии _internalConnectionState и текущей теме оформления.
     */
    val connectionStatusInfo: StateFlow<ConnectionStatusInfo> = combine(
        _internalConnectionState,
        _appSettings
    ) { state, settings ->
        // Определяем, используется ли темная тема для адаптации цветов статуса
        val isDark = settings.selectedTheme != "light"
        state.toStatusInfo(isDark)
    }.stateIn(
        scope = appScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ConnectionState.UNDEFINED.toStatusInfo(isDarkTheme = true)
    )

    // ========== ИНИЦИАЛИЗАЦИЯ ==========

    init {
        AppLogger.logInfo("Инициализация AppController", TAG)
        startInitialization()
    }

    /**
     * Выполняет процедуру асинхронной инициализации контроллера.
     * Процесс включает загрузку настроек, конфигурацию логгера и связывание с Bluetooth сервисом.
     */
    private fun startInitialization() {
        appScope.launch {
            try {
                // 1. Загружаем сохраненные настройки из репозитория (DataStore)
                val settings = settingsRepository.getCurrentSettings()
                _appSettings.value = settings
                
                // 2. Настраиваем глобальный логгер согласно требованиям отладки
                AppLogger.configure(verbose = true, timestamps = true, packetNumbers = true)

                // 3. Инициализируем базу данных ошибок ЭБУ и комбинаций
                initEcuErrorDatabase()
                initEcuCombinationDatabase()

                // 4. Выполняем привязку к Android-сервису Bluetooth через ServiceLocator
                ServiceLocator.bindBluetoothService(context) { service ->
                    // 5. Создаем экземпляр оркестратора подключений (BCM)
                    _bluetoothConnectionManager = BluetoothConnectionManager(context, service)
                    
                    // 6. Передаем в BCM данные целевого устройства из загруженных настроек
                    _bluetoothConnectionManager?.updateSelectedDevice(settings.selectedDevice)
                    
                    // 7. Активируем фоновых слушателей для обработки данных и состояний
                    startMessageListener()
                    startConnectionStateSychronizer()
                    
                    // 8. Сигнализируем системе о завершении инициализации
                    _isInitialized.value = true
                    AppLogger.logInfo("AppController полностью инициализирован", TAG)
                }

            } catch (e: Exception) {
                // В случае критического сбоя логируем ошибку и блокируем работу контроллера
                AppLogger.logError("Ошибка инициализации: ${e.message}", TAG)
                _isInitialized.value = false
            }
        }
    }

    /**
     * Выполняет проверку и наполнение базы данных ошибок ЭБУ из JSON файла.
     * Выполняется только если база данных пуста.
     */
    private suspend fun initEcuErrorDatabase() {
        withContext(Dispatchers.IO) {
            try {
                val dao = ServiceLocator.getDatabase().ecuErrorDao()
                
                // Проверяем количество записей
                val count = dao.getCount()
                AppLogger.logInfo("Текущее количество ошибок в БД: $count", TAG)

                if (count == 0) {
                    AppLogger.logInfo("База данных ошибок пуста. Начинаю импорт из $ECU_ERRORS_JSON_PATH", TAG)
                    
                    // Читаем JSON из assets
                    val jsonString = context.assets.open(ECU_ERRORS_JSON_PATH).bufferedReader().use { it.readText() }
                    
                    // Десериализуем список сущностей
                    val errors = json.decodeFromString<List<EcuErrorEntity>>(jsonString)
                    
                    // Массовая вставка в БД
                    dao.insertAll(errors)
                    
                    AppLogger.logInfo("Импорт завершен успешно. Добавлено ${errors.size} записей.", TAG)
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка при инициализации БД ошибок: ${e.message}", TAG)
            }
        }
    }

    /**
     * Выполняет проверку и наполнение базы данных комбинаций ошибок из JSON файла.
     */
    private suspend fun initEcuCombinationDatabase() {
        withContext(Dispatchers.IO) {
            try {
                val dao = ServiceLocator.getDatabase().ecuCombinationDao()
                val count = dao.getCount()
                
                if (count == 0) {
                    AppLogger.logInfo("База данных комбинаций пуста. Импорт из $ECU_COMBINATIONS_JSON_PATH", TAG)
                    
                    // Читаем JSON из assets
                    val jsonString = context.assets.open(ECU_COMBINATIONS_JSON_PATH).bufferedReader().use { it.readText() }
                    
                    // Десериализуем список сущностей
                    val combinations = json.decodeFromString<List<EcuCombinationEntity>>(jsonString)
                    
                    // Массовая вставка в БД
                    dao.insertAll(combinations)
                    
                    AppLogger.logInfo("Импорт комбинаций завершен. Добавлено ${combinations.size} записей.", TAG)
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка при инициализации БД комбинаций: ${e.message}", TAG)
            }
        }
    }

    /**
     * Импортирует базу данных ошибок ЭБУ из внешнего JSON файла по URI.
     * Выполняет УМНОЕ ОБНОВЛЕНИЕ: заменяет существующие и добавляет новые записи.
     * 
     * @param uri URI выбранного пользователем файла
     * @return Результат операции (количество записей или ошибка)
     */
    suspend fun importEcuErrorsFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            // 1. Читаем содержимое файла
            val jsonString = context.contentResolver.openInputStream(uri)?.use { 
                it.bufferedReader().readText() 
            } ?: throw Exception("Не удалось открыть файл")

            // 2. Валидация структуры данных
            val errors = try {
                json.decodeFromString<List<EcuErrorEntity>>(jsonString)
            } catch (e: SerializationException) {
                throw Exception("Формат данных в файле не соответствует ожидаемой структуре базы ошибок")
            }

            if (errors.isEmpty()) throw Exception("Файл пуст")

            // 3. Выполняем обновление базы данных (без предварительного удаления)
            val dao = ServiceLocator.getDatabase().ecuErrorDao()
            dao.insertAll(errors)
            
            AppLogger.logInfo("Обновление базы ошибок завершено: ${errors.size} записей обработано", TAG)
            Result.success(errors.size)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка импорта базы ошибок: ${e.message}", TAG)
            Result.failure(e)
        }
    }

    /**
     * Импортирует базу данных комбинаций из внешнего JSON файла по URI.
     * Выполняет УМНОЕ ОБНОВЛЕНИЕ: заменяет существующие и добавляет новые записи.
     */
    suspend fun importEcuCombinationsFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { 
                it.bufferedReader().readText() 
            } ?: throw Exception("Не удалось открыть файл")

            val combinations = try {
                json.decodeFromString<List<EcuCombinationEntity>>(jsonString)
            } catch (e: SerializationException) {
                throw Exception("Формат данных в файле не соответствует ожидаемой структуре базы комбинаций")
            }

            if (combinations.isEmpty()) throw Exception("Файл пуст")

            val dao = ServiceLocator.getDatabase().ecuCombinationDao()
            dao.insertAll(combinations)
            
            AppLogger.logInfo("Обновление базы комбинаций завершено: ${combinations.size} записей обработано", TAG)
            Result.success(combinations.size)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка импорта базы комбинаций: ${e.message}", TAG)
            Result.failure(e)
        }
    }

    /**
     * Экспортирует текущую базу данных ошибок ЭБУ в JSON файл по предоставленному URI.
     * 
     * @param uri URI для сохранения файла (SAF)
     * @return Результат операции
     */
    suspend fun exportEcuErrorsToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Получаем все данные из БД
            val dao = ServiceLocator.getDatabase().ecuErrorDao()
            val errors = dao.getAllErrorsList()

            // 2. Сериализуем в JSON строку
            val jsonString = json.encodeToString(errors)

            // 3. Записываем в файл через ContentResolver
            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(jsonString.toByteArray()) 
            } ?: throw Exception("Не удалось открыть файл для записи")

            AppLogger.logInfo("Экспорт базы ошибок завершен: ${errors.size} записей", TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка экспорта базы ошибок: ${e.message}", TAG)
            Result.failure(e)
        }
    }

    /**
     * Экспортирует текущую базу данных комбинаций в JSON файл по предоставленному URI.
     * 
     * @param uri URI для сохранения файла (SAF)
     * @return Результат операции
     */
    suspend fun exportEcuCombinationsToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dao = ServiceLocator.getDatabase().ecuCombinationDao()
            val combinations = dao.getAllCombinationsList()

            val jsonString = json.encodeToString(combinations)

            context.contentResolver.openOutputStream(uri)?.use { 
                it.write(jsonString.toByteArray()) 
            } ?: throw Exception("Не удалось открыть файл для записи")

            AppLogger.logInfo("Экспорт базы комбинаций завершен: ${combinations.size} записей", TAG)
            Result.success(Unit)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка экспорта базы комбинаций: ${e.message}", TAG)
            Result.failure(e)
        }
    }

    /**
     * Запускает корутину-слушатель для обработки входящих JSON-сообщений.
     * Получает распарсенные объекты из BluetoothConnectionManager и передает их в обработчик.
     */
    private fun startMessageListener() {
        appScope.launch {
            bluetoothConnectionManager.incomingMessagesFlow.collect { jsonObject ->
                // Перенаправляем каждый входящий JSON-пакет в метод бизнес-логики разбора
                handleIncomingMessage(jsonObject)
            }
        }
    }

    /**
     * Синхронизирует внутреннее состояние подключения со статусом из BCM.
     * Также отслеживает момент установления физической связи (CONNECTED) 
     * для запуска сценария начального обмена данными.
     */
    private fun startConnectionStateSychronizer() {
        appScope.launch {
            bluetoothConnectionManager.connectionStateFlow.collect { state ->
                // Нормализуем состояние (null заменяем на UNDEFINED)
                val newState = state ?: ConnectionState.UNDEFINED
                
                // 1. Обновляем внутренний Flow, на который подписан UI
                _internalConnectionState.value = newState
                
                // 2. Если достигнуто состояние CONNECTED, инициируем бизнес-сценарий
                if (newState == ConnectionState.CONNECTED) {
                    log("Состояние CONNECTED: Запуск сценария инициализации обмена")
                    executeInitialSequence()
                }
            }
        }
    }

    /**
     * Реализует последовательность команд начальной инициализации при подключении.
     * Устанавливает статус REQUESTING_DATA и запрашивает конфигурацию/поток данных.
     * 
     * ОБНОВЛЕНИЕ ПРОТОКОЛА: Используются унифицированные команды в нижнем регистре.
     */
    private suspend fun executeInitialSequence() {
        // 1. Устанавливаем логический статус "Запрос данных" для информирования пользователя
        _internalConnectionState.value = ConnectionState.REQUESTING_DATA

        // 2. Запрашиваем текущие настройки БК
        sendJsonCommand("""{"command":"get_settings"}""")
        
        // 3. Запускаем стриминг телеметрии
        sendJsonCommand("""{"command":"start_telemetry"}""")
    }

    /**
     * Центральный метод разбора входящих JSON-сообщений.
     * Классифицирует пакеты по ключам (telemetry, settings, error, ack_id) и распределяет их по подсистемам.
     * 
     * ОБНОВЛЕНИЕ ПРОТОКОЛА: 
     * - Ключ данных изменен с "data" на "telemetry".
     * - Добавлена обработка ack_id для подтверждения получения команд.
     * 
     * @param jsonObject Объект, полученный из транспортного слоя.
     */
    private fun handleIncomingMessage(jsonObject: JsonObject) {
        try {
            // ЛОГИРОВАНИЕ ПОДТВЕРЖДЕНИЯ (ACK)
            jsonObject["ack_id"]?.jsonPrimitive?.intOrNull?.let { ackId ->
                AppLogger.logInfo("БК подтвердил получение сообщения: $ackId", TAG)
            }

            // КЕЙС 1: Пакет содержит оперативные данные автомобиля (telemetry)
            // ОБНОВЛЕНИЕ: Ключ теперь "telemetry"
            if (jsonObject.containsKey("telemetry")) {
                // Если это первый пакет данных - переключаем статус в "Прослушивание" (визуальная индикация)
                if (_internalConnectionState.value != ConnectionState.LISTENING_DATA) {
                    _internalConnectionState.value = ConnectionState.LISTENING_DATA
                }

                // Извлекаем вложенный объект данных
                val telemetryElement = jsonObject["telemetry"]
                if (telemetryElement != null) {
                    // Десериализуем JSON в доменную модель CarData
                    val carData = json.decodeFromJsonElement<CarData>(telemetryElement)
                    // Эмитим обновленные данные в поток для UI
                    appScope.launch { _rawCarDataFlow.emit(carData) }
                }
            }
            
            // КЕЙС 2: Пакет содержит технические настройки, присланные бортовым компьютером
            if (jsonObject.containsKey("settings")) {
                val settingsFromRemote = jsonObject["settings"]?.jsonObject
                if (settingsFromRemote != null) {
                    // Инициируем процедуру синхронизации удаленных настроек с локальными
                    syncSettingsFromRemote(settingsFromRemote)
                }
            }
            
            // КЕЙС 3: Пакет содержит описание ошибки, возникшей на стороне БК
            jsonObject["error"]?.jsonPrimitive?.content?.let { errorMsg ->
                AppLogger.logError("БК вернул ошибку выполнения: $errorMsg", TAG)
            }
            
        } catch (e: Exception) {
            // Логируем ошибки десериализации без прерывания основного цикла слушателя
            AppLogger.logError("Ошибка обработки пакета: ${e.message}", TAG)
        }
    }

    /**
     * Выполняет сравнение и слияние настроек, полученных от устройства, с локальными.
     * Если данные отличаются, локальные настройки обновляются.
     * 
     * @param remoteJson JSON-объект с актуальными параметрами от БК.
     */
    private fun syncSettingsFromRemote(remoteJson: JsonObject) {
        val current = _appSettings.value
        // Используем метод модели для логического слияния (merge)
        val updated = current.mergeWithRemote(remoteJson)

        // Если объект изменился (поля не идентичны)
        if (updated !== current) {
            log("Настройки от БК отличаются от локальных. Синхронизация...")
            // Обновляем локальное хранилище без обратной отправки на устройство (флаг fromRemote)
            updateSettings(updated, fromRemote = true)
        }
    }

    // ========== ПУБЛИЧНЫЙ API ДЛЯ UI И ДРУГИХ СЛОЕВ ==========

    /**
     * Предоставляет реактивный поток данных для конкретной ошибки по её коду.
     * Используется для отображения деталей ошибки (включая связанные).
     * 
     * @param code Код ошибки (напр. P0300)
     * @return Flow с объектом ошибки или null
     */
    fun getEcuErrorByCode(code: String): Flow<EcuErrorEntity?> {
        return ServiceLocator.getDatabase().ecuErrorDao().getErrorByCode(code)
    }

    /**
     * Предоставляет поток данных об ошибках ЭБУ на основе списка кодов.
     * @param codes Список строк с кодами ошибок
     * @return Flow со списком сущностей из БД
     */
    fun getEcuErrorsByCodes(codes: List<String>): Flow<List<EcuErrorEntity>> {
        return ServiceLocator.getDatabase().ecuErrorDao().getErrorsByCodes(codes)
    }

    /**
     * Предоставляет поток всех возможных комбинаций ошибок.
     */
    fun getAllCombinations(): Flow<List<EcuCombinationEntity>> {
        return ServiceLocator.getDatabase().ecuCombinationDao().getAllCombinations()
    }

    /**
     * Предоставляет список сопряженных устройств через проксирование к менеджеру.
     * @return Список BluetoothDeviceData или null, если система не готова.
     */
    fun getPairedDevices(): List<BluetoothDeviceData>? = 
        if (_isInitialized.value) bluetoothConnectionManager.getPairedDevices() else null

    /**
     * Проверяет статус Bluetooth-адаптера на устройстве пользователя.
     * @return true если Bluetooth включен.
     */
    fun isBluetoothEnabled(): Boolean = 
        if (_isInitialized.value) bluetoothConnectionManager.isBluetoothEnabled() else false

    /**
     * Возвращает текущий слепок настроек приложения.
     */
    fun getCurrentSettings(): AppSettings = _appSettings.value

    /**
     * Главный метод обновления конфигурации приложения.
     * Выполняет сохранение в DataStore и уведомляет менеджер подключений о смене устройства.
     * 
     * @param newSettings Новый объект настроек.
     * @param fromRemote Флаг, указывающий что настройки пришли от БК (исключает циклическую отправку).
     */
    fun updateSettings(newSettings: AppSettings, fromRemote: Boolean = false) {
        appScope.launch {
            try {
                val current = _appSettings.value
                // Оптимизация: если данные не изменились, игнорируем запрос
                if (current == newSettings) return@launch

                // 1. Сохраняем настройки в постоянное хранилище
                settingsRepository.saveSettings(newSettings)
                
                // 2. Обновляем реактивное состояние для подписчиков
                _appSettings.value = newSettings
                
                // 3. Если изменение инициировано пользователем и контроллер готов
                if (_isInitialized.value && !fromRemote) {
                    // 3.1. Уведомляем менеджер о смене устройства (если оно изменилось)
                    bluetoothConnectionManager.updateSelectedDevice(newSettings.selectedDevice)
                    
                    // 3.2. Отправляем технические параметры на БК для синхронизации
                    // ИСПРАВЛЕНО СОГЛАСНО ТАБЛИЦЕ ПРОТОКОЛА: Команда "set_settings", ключ "data"
                    val settingsPayload = newSettings.toDeviceSettingsJson()
                    sendJsonCommand("""{"command":"set_settings","data":$settingsPayload}""")
                    AppLogger.logInfo("Настройки отправлены на БК (set_settings): $settingsPayload", TAG)
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    /**
     * Разрывает текущее соединение путем сброса выбранного устройства.
     * ПЕРЕД разрывом отправляет команду остановки телеметрии.
     */
    fun disconnectFromDevice() {
        // Остановка стриминга перед разрывом связи
        stopTelemetry()
        // Сброс устройства в настройках инициирует процедуру отключения в BCM
        updateSettings(_appSettings.value.copy(selectedDevice = BluetoothDeviceData.empty()))
    }

    /**
     * Очищает выбор устройства в настройках.
     */
    fun clearSelectedDevice() {
        updateSettings(_appSettings.value.copy(selectedDevice = BluetoothDeviceData.empty()))
    }

    /**
     * Инициирует повторную попытку подключения с текущими параметрами.
     */
    fun retryConnection() {
        if (_isInitialized.value) bluetoothConnectionManager.startConnectionProcess()
    }

    /**
     * Возвращает текущее значение данных автомобиля.
     */
    fun getCurrentCarData(): CarData = carData.value

    /**
     * Предоставляет отладочную информацию о состоянии канала связи.
     */
    fun getConnectionStatistics(): String = 
        if (_isInitialized.value) bluetoothConnectionManager.getConnectionStatistics() else "Инициализация..."

    /**
     * Сбрасывает счетчики и статистику в менеджере подключений.
     */
    fun resetConnectionStatistics() {
        if (_isInitialized.value) bluetoothConnectionManager.resetStatistics()
    }

    /**
     * Прокси-метод для отправки произвольных JSON-команд на устройство.
     * ПРИМЕЧАНИЕ: Транспортный слой (DataStreamHandler) сам добавит msg_id к этим командам.
     *
     * @param jsonCommand Строка в формате JSON.
     */
    fun sendJsonCommand(jsonCommand: String) {
        if (_isInitialized.value) {
            AppLogger.logInfo("Отправка команды: $jsonCommand", TAG)
            bluetoothConnectionManager.sendJsonCommand(jsonCommand)
        }
    }

    /**
     * Отправляет зарезервированную команду на остановку стриминга телеметрии.
     * Полезно при отключении от устройства или закрытии приложения.
     */
    fun stopTelemetry() {
        if (_isInitialized.value) {
            log("Запрос остановки телеметрии (stop_telemetry)")
            sendJsonCommand("""{"command":"stop_telemetry"}""")
        }
    }

    /**
     * Внутренняя обертка для логирования с тегом контроллера.
     */
    private fun log(message: String) {
        AppLogger.logInfo(message, TAG)
    }

    // ========== УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ (LIFECYCLE) ==========

    /**
     * Выполняет полную очистку ресурсов контроллера перед уничтожением.
     */
    fun cleanup() {
        // 1. Пытаемся остановить стриминг данных перед закрытием канала
        stopTelemetry()
        // 2. Останавливаем все процессы в менеджере подключений
        _bluetoothConnectionManager?.cleanup()
        // 3. Отменяем все активные корутины в области видимости приложения
        appScope.cancel()
        // 4. Сбрасываем флаг готовности
        _isInitialized.value = false
    }

    /**
     * Выполняет "горячую" перезагрузку контроллера.
     * Полезно при критических сбоях или необходимости переинициализации сервисов.
     */
    fun reload() {
        cleanup()
        appScope.launch {
            // Небольшая задержка для освобождения системных ресурсов
            delay(1000)
            startInitialization()
        }
    }
}
