// Файл: core/AppController.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context
import android.net.Uri
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothConnectionManager
import com.alexbar3073.bluetoothcar.data.bluetooth.OtaManager
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.data.models.CarPacket
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*

/**
 * ТЕГ: Главный координатор
 *
 * ФАЙЛ: core/AppController.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * ЦЕНТРАЛЬНЫЙ УЗЕЛ БИЗНЕС-ЛОГИКИ. Единственный «мозг» приложения, принимающий решения.
 * Управляет всеми ключевыми потоками данных и жизненным циклом компонентов связи.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Управление жизненным циклом BluetoothConnectionManager.
 * 2. Реализация СЦЕНАРИЕВ обмена данными (инициализация после подключения).
 * 3. ПАРСИНГ входящих данных (JsonObject) от BCM и преобразование их в CarData.
 * 4. СИНХРОНИЗАЦИЯ настроек: прием изменений настроек от БК и обновление локального стейта.
 * 5. Хранение и дистрибуция настроек приложения.
 * 6. ФОРМИРОВАНИЕ финального статуса подключения для UI.
 * 7. Инициализация и наполнение справочника ошибок ЭБУ из JSON.
 * 8. ОБНОВЛЕНИЕ базы данных из внешних пользовательских файлов.
 * 9. ЭКСПОРТ текущего состава базы данных в JSON файлы.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Разделение ответственности: Транспорт (DSH) и Оркестратор (BCM) не знают о бизнес-логике.
 * - Централизация: Вся логика «что и когда отправлять» сосредоточена здесь.
 * - Оптимизация: Использует CarPacket для частичного обновления CarData, исключая "моргание".
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Реактивность через StateFlow и обработка входящего потока сообщений.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: AppBluetoothService, BluetoothConnectionManager, SettingsRepository, EcuErrorDao.
 * - Вызывается из: SharedViewModel, MainActivity.
 * - Взаимодействует: ServiceLocator (для получения зависимостей).
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

    /** Менеджер OTA обновления прошивки */
    private val otaManager = OtaManager()
    
    /** Поток состояния процесса OTA для UI */
    val otaState: StateFlow<OtaManager.OtaState> = otaManager.state

    // ========== BLUETOOTH МЕНЕДЖЕР ==========

    private var _bluetoothConnectionManager: BluetoothConnectionManager? = null
    
    private val bluetoothConnectionManager: BluetoothConnectionManager
        get() = _bluetoothConnectionManager ?: throw IllegalStateException("BluetoothConnectionManager еще не инициализирован.")

    // ========== ПОТОКИ ДАННЫХ ==========

    /** 
     * Поток данных автомобиля.
     * Использует StateFlow для обеспечения наличия начального значения (CarData()),
     * что предотвращает блокировку оператора combine при инициализации.
     */
    private val _rawCarDataFlow = MutableStateFlow(CarData())
    
    val carData: StateFlow<CarData> = _isInitialized
        .filter { it }
        .flatMapLatest {
            combine(
                _rawCarDataFlow,
                _appSettings,
                connectionStatusInfo
            ) { carDataValue, settings, connectionStatus ->
                if (!connectionStatus.isActive) {
                    // Если соединение не активно, возвращаем пустой объект, 
                    // но сохраняем одометр или другие важные данные если нужно (сейчас сброс)
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
     * Возвращает контекст приложения для работы с ресурсами.
     */
    fun getApplicationContext(): Context = context

    /**
     * Выполняет процедуру асинхронной инициализации контроллера.
     */
    private fun startInitialization() {
        appScope.launch {
            try {
                // 1. Загружаем сохраненные настройки из репозитория (DataStore)
                val settings = settingsRepository.getCurrentSettings()
                _appSettings.value = settings
                
                // 2. Настраиваем глобальный логгер
                AppLogger.configure(isDebug = true, verbose = true, packetNumbers = true)

                // 3. Инициализируем базу данных ошибок ЭБУ
                initEcuErrorDatabase()

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
                AppLogger.logError("Ошибка инициализации: ${e.message}", TAG)
                _isInitialized.value = false
            }
        }
    }

    /**
     * Выполняет проверку и наполнение базы данных ошибок ЭБУ из JSON файла.
     */
    private suspend fun initEcuErrorDatabase() {
        withContext(Dispatchers.IO) {
            try {
                val dao = ServiceLocator.getDatabase().ecuErrorDao()
                val count = dao.getCount()
                if (count == 0) {
                    val jsonString = context.assets.open(ECU_ERRORS_JSON_PATH).bufferedReader().use { it.readText() }
                    val allItems = json.decodeFromString<List<EcuErrorEntity>>(jsonString)
                    // Теперь импортируем ВСЕ записи, включая комбинации
                    dao.insertAll(allItems)
                    AppLogger.logInfo("Импорт ошибок завершен: ${allItems.size} записей.", TAG)
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка при инициализации БД ошибок: ${e.message}", TAG)
            }
        }
    }

    /**
     * Импортирует базу данных ошибок ЭБУ из внешнего JSON файла по URI.
     */
    suspend fun importEcuErrorsFromUri(uri: Uri): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: throw Exception("Не удалось открыть файл")
            val allItems = json.decodeFromString<List<EcuErrorEntity>>(jsonString)
            if (allItems.isEmpty()) throw Exception("Файл пуст")
            
            val dao = ServiceLocator.getDatabase().ecuErrorDao()
            dao.insertAll(allItems)
            Result.success(allItems.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Экспортирует текущую базу данных ошибок ЭБУ в JSON файл.
     */
    suspend fun exportEcuErrorsToUri(uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dao = ServiceLocator.getDatabase().ecuErrorDao()
            val errors = dao.getAllErrorsList()
            val jsonString = json.encodeToString(errors)
            context.contentResolver.openOutputStream(uri)?.use { it.write(jsonString.toByteArray()) } ?: throw Exception("Не удалось открыть файл для записи")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Запускает корутину-слушатель для обработки входящих JSON-сообщений.
     */
    private fun startMessageListener() {
        appScope.launch {
            bluetoothConnectionManager.incomingMessagesFlow.collect { jsonObject ->
                handleIncomingMessage(jsonObject)
            }
        }
    }

    /**
     * Синхронизирует внутреннее состояние подключения со статусом из BCM.
     */
    private fun startConnectionStateSychronizer() {
        appScope.launch {
            bluetoothConnectionManager.connectionStateFlow.collect { state ->
                val newState = state ?: ConnectionState.UNDEFINED
                _internalConnectionState.value = newState
                if (newState == ConnectionState.CONNECTED) {
                    executeInitialSequence()
                }
            }
        }
    }

    /**
     * Реализует последовательность команд начальной инициализации при подключении.
     */
    private suspend fun executeInitialSequence() {
        _internalConnectionState.value = ConnectionState.REQUESTING_DATA
        sendJsonCommand("""{"command":"get_cfg"}""")
        sendJsonCommand("""{"command":"start_telemetry"}""")
    }

    /**
     * Центральный метод разбора входящих JSON-сообщений.
     * Реализует логику частичного обновления (Partial Update) для CarData.
     * 
     * @param jsonObject Объект, полученный из транспортного слоя.
     */
    private fun handleIncomingMessage(jsonObject: JsonObject) {
        try {
            // ЛОГИРОВАНИЕ ПОДТВЕРЖДЕНИЯ (ACK)
            jsonObject["ack_id"]?.jsonPrimitive?.content?.let { ackIdStr ->
                val ackId = ackIdStr.toLongOrNull()
                AppLogger.logInfo("БК подтвердил получение сообщения: $ackId", TAG)
            }

            // КЕЙС 0: Проверка на подтверждение OTA пакета (ota_read)
            jsonObject["ota_read"]?.jsonPrimitive?.intOrNull?.let { confirmedPacket ->
                if (confirmedPacket > 0) {
                    // Извлекаем общее количество пакетов из текущего состояния OTA
                    val currentState = otaManager.state.value
                    if (currentState is OtaManager.OtaState.Sending) {
                        otaManager.updateProgress(confirmedPacket, currentState.totalPackets)
                    }
                }
            }

            // КЕЙС 0.1: Проверка на ошибку OTA от БК
            jsonObject["ota"]?.jsonPrimitive?.content?.let { otaStatus ->
                if (otaStatus == "error") {
                    AppLogger.logError("БК вернул критическую ошибку OTA! Прерывание процесса.", TAG)
                    
                    // 1. Очищаем очередь команд, чтобы остановить отправку пакетов
                    bluetoothConnectionManager.clearCommandQueue()
                    
                    // 2. Устанавливаем состояние ошибки в менеджере OTA для UI
                    otaManager.setError("Критическая ошибка БК при обновлении")
                    
                    // 3. Сбрасываем флаги (если есть)
                    // ... в данной реализации всё управляется состоянием otaManager.state
                }
            }

            // КЕЙС 1: Пакет содержит оперативные данные автомобиля (tel)
            if (jsonObject.containsKey("tel")) {
                if (_internalConnectionState.value != ConnectionState.LISTENING_DATA) {
                    _internalConnectionState.value = ConnectionState.LISTENING_DATA
                }

                val telemetryElement = jsonObject["tel"]
                if (telemetryElement != null) {
                    // ЛОГИРОВАНИЕ ТЕЛЕМЕТРИИ
                    AppLogger.logReceive("Получены данные телеметрии (tel)", telemetryElement.toString())

                    // 1. Десериализуем входящий JSON в промежуточный ТРАНСПОРТНЫЙ пакет (CarPacket)
                    val packet = json.decodeFromJsonElement<CarPacket>(telemetryElement)
                    
                    // 2. Получаем текущее состояние данных
                    val currentData = _rawCarDataFlow.value
                    
                    // 3. Выполняем СЛИЯНИЕ (Partial Update)
                    val updatedData = currentData.updateWith(packet)
                    
                    // 4. Обновляем поток данных (StateFlow автоматически уведомит подписчиков)
                    _rawCarDataFlow.value = updatedData
                }
            }
            
            // КЕЙС 2: Пакет содержит технические настройки, присланные бортовым компьютером (cfg)
            if (jsonObject.containsKey("cfg")) {
                val settingsFromRemote = jsonObject["cfg"]?.jsonObject
                if (settingsFromRemote != null) {
                    syncSettingsFromRemote(settingsFromRemote)
                }
            }
            
            // КЕЙС 3: Пакет содержит описание ошибки от БК
            jsonObject["error"]?.jsonPrimitive?.content?.let { errorMsg ->
                AppLogger.logError("БК вернул ошибку выполнения: $errorMsg", TAG)
            }
            
        } catch (e: Exception) {
            AppLogger.logError("Ошибка обработки пакета: ${e.message}", TAG)
        }
    }

    /**
     * Выполняет сравнение и слияние настроек.
     */
    private fun syncSettingsFromRemote(remoteJson: JsonObject) {
        val current = _appSettings.value
        val updated = current.mergeWithRemote(remoteJson)
        if (updated !== current) {
            updateSettings(updated, fromRemote = true)
        }
    }

    /**
     * Предоставляет реактивный поток данных для конкретной ошибки.
     */
    fun getEcuErrorByCode(code: String): Flow<EcuErrorEntity?> {
        return ServiceLocator.getDatabase().ecuErrorDao().getErrorByCode(code)
    }

    /**
     * Предоставляет поток данных об ошибках ЭБУ на основе списка кодов.
     */
    fun getEcuErrorsByCodes(codes: List<String>): Flow<List<EcuErrorEntity>> {
        return ServiceLocator.getDatabase().ecuErrorDao().getErrorsByCodes(codes)
    }

    /**
     * Предоставляет поток всех существующих в базе комбинаций ошибок.
     * Используется для экспертной диагностики.
     */
    fun getAllEcuCombinations(): Flow<List<EcuErrorEntity>> {
        return ServiceLocator.getDatabase().ecuErrorDao().getAllCombinations()
    }

    /**
     * Предоставляет список сопряженных устройств.
     */
    fun getPairedDevices(): List<BluetoothDeviceData>? = 
        if (_isInitialized.value) bluetoothConnectionManager.getPairedDevices() else null

    /** Поток найденных в эфире устройств (Discovery) */
    val discoveryFlow: Flow<BluetoothDeviceData>
        get() = AppBluetoothService.getInstance()?.discoveryFlow ?: emptyFlow()

    /** Поток состояния процесса поиска */
    val isDiscovering: StateFlow<Boolean>
        get() = AppBluetoothService.getInstance()?.isDiscovering ?: MutableStateFlow(false).asStateFlow()

    /**
     * Начать поиск новых устройств.
     * Результаты транслируются в discoveryFlow.
     */
    fun startDiscovery(): Boolean {
        return AppBluetoothService.getInstance()?.startDiscovery() ?: false
    }

    /**
     * Остановить поиск устройств.
     */
    fun stopDiscovery() {
        AppBluetoothService.getInstance()?.stopDiscovery()
    }

    /**
     * Поток изменений состояния сопряжения из системы.
     */
    val bondStateFlow: Flow<BluetoothDeviceData> = flow {
        AppBluetoothService.getInstance()?.bondStateFlow?.let { emitAll(it) }
    }.flowOn(Dispatchers.Main)

    /**
     * Выполнить сопряжение с устройством.
     * 
     * @param address MAC-адрес устройства.
     * @return true если процесс запущен.
     */
    fun pairDevice(address: String): Boolean {
        return AppBluetoothService.getInstance()?.pairDevice(address) ?: false
    }

    /**
     * Подписаться на изменения состояния сопряжения.
     */
    @Deprecated("Используйте bondStateFlow", ReplaceWith("bondStateFlow"))
    fun monitorBondState(callback: (BluetoothDeviceData) -> Unit) {
        appScope.launch {
            bondStateFlow.collect { callback(it) }
        }
    }

    /**
     * Проверяет статус Bluetooth-адаптера.
     */
    fun isBluetoothEnabled(): Boolean = 
        if (_isInitialized.value) bluetoothConnectionManager.isBluetoothEnabled() else false

    /**
     * Возвращает текущий слепок настроек приложения.
     */
    fun getCurrentSettings(): AppSettings = _appSettings.value

    /**
     * Главный метод обновления конфигурации приложения.
     */
    fun updateSettings(newSettings: AppSettings, fromRemote: Boolean = false) {
        appScope.launch {
            try {
                val current = _appSettings.value
                if (current == newSettings) return@launch
                settingsRepository.saveSettings(newSettings)
                _appSettings.value = newSettings
                if (_isInitialized.value && !fromRemote) {
                    bluetoothConnectionManager.updateSelectedDevice(newSettings.selectedDevice)
                    val settingsPayload = newSettings.toDeviceSettingsJson()
                    sendJsonCommand("""{"command":"set_cfg","data":$settingsPayload}""")
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    /**
     * Разрывает текущее соединение.
     */
    fun disconnectFromDevice() {
        stopTelemetry()
        updateSettings(_appSettings.value.copy(selectedDevice = BluetoothDeviceData.empty()))
    }

    /**
     * Очищает выбор устройства в настройках.
     */
    fun clearSelectedDevice() {
        updateSettings(_appSettings.value.copy(selectedDevice = BluetoothDeviceData.empty()))
    }

    /**
     * Инициирует повторную попытку подключения.
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
     * Прокси-метод для отправки произвольных JSON-команд.
     */
    fun sendJsonCommand(jsonCommand: String) {
        if (_isInitialized.value) {
            AppLogger.logInfo("Отправка команды: $jsonCommand", TAG)
            bluetoothConnectionManager.sendJsonCommand(jsonCommand)
        }
    }

    /**
     * Отправляет команду на остановку стриминга телеметрии.
     */
    fun stopTelemetry() {
        if (_isInitialized.value) {
            sendJsonCommand("""{"command":"stop_telemetry"}""")
        }
    }

    /**
     * Сценарий: Запуск OTA обновления прошивки БК.
     * 
     * @param fileBytes Бинарные данные файла прошивки.
     * @param fileName Имя файла для валидации.
     */
    fun startOtaUpdate(fileBytes: ByteArray, fileName: String) {
        appScope.launch {
            // 1. Проверка состояния двигателя (должен быть остановлен)
            if (_rawCarDataFlow.value.engineStatus) {
                otaManager.setError("Ошибка: Двигатель запущен! Остановите двигатель перед обновлением.")
                return@launch
            }

            // 2. Валидация файла
            if (!otaManager.validateFile(fileName)) {
                otaManager.setError("Ошибка: Недопустимый файл прошивки (проверьте имя и расширение).")
                return@launch
            }

            // 3. Подготовка пакетов
            val commands = otaManager.prepareOtaCommands(fileBytes)
            
            // 4. Остановка телеметрии (режим "тишины" в канале для OTA)
            stopTelemetry()
            delay(500) // Пауза для очистки канала от остаточных пакетов БК
            
            val totalPackets = commands.size
            AppLogger.logInfo("OTA: Начало процесса передачи ${totalPackets} пакетов", TAG)
            
            // 5. Установка начального состояния в менеджере
            otaManager.updateProgress(0, totalPackets)

            // 6. Последовательная постановка команд в очередь. 
            // Благодаря DataStreamHandler, каждая команда будет ждать Ack прежде чем уйдет следующая.
            commands.forEach { command ->
                sendJsonCommand(command)
            }
            
            AppLogger.logInfo("OTA: Все пакеты переданы в очередь транспорта", TAG)
        }
    }

    /**
     * Сбросить состояние OTA.
     */
    fun resetOtaState() {
        otaManager.reset()
    }

    /**
     * Сбрасывает текущие активные процессы без отмены основной области видимости.
     * Используется для перезагрузки контроллера.
     */
    private fun reset() {
        AppLogger.logInfo("Сброс активных процессов контроллера", TAG)
        // 1. Останавливаем стриминг телеметрии, чтобы не нагружать канал
        stopTelemetry()
        // 2. Очищаем ресурсы менеджера соединений (отписка от Bluetooth-событий)
        _bluetoothConnectionManager?.cleanup()
        // 3. Сбрасываем флаг инициализации для блокировки доступа к неактивным компонентам
        _isInitialized.value = false
    }

    /**
     * Очистка всех ресурсов и завершение работы контроллера.
     */
    fun cleanup() {
        AppLogger.logInfo("Полная очистка ресурсов AppController (Shutdown)", TAG)
        // 1. Сбрасываем активные компоненты
        reset()
        // 2. Отменяем основную область корутин (дальнейшие запуски через appScope будут невозможны)
        appScope.cancel()
    }

    /**
     * "Горячая" перезагрузка контроллера.
     * Очищает текущее состояние и запускает инициализацию заново.
     */
    fun reload() {
        AppLogger.logInfo("Выполнение горячей перезагрузки контроллера", TAG)
        // 1. Сбрасываем старое состояние
        reset()
        // 2. Запускаем инициализацию немедленно, так как appScope не был отменен
        appScope.launch {
            startInitialization()
        }
    }
}
