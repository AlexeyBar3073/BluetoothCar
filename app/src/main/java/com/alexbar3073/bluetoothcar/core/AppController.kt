// Файл: core/AppController.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context
import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothConnectionManager
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*

/**
 * ТЕГ: Главный координатор
 * ФАЙЛ: core/AppController.kt
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ЦЕНТРАЛЬНЫЙ УЗЕЛ БИЗНЕС-ЛОГИКИ. Единственный «мозг» приложения, принимающий решения.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Управление жизненным циклом BluetoothConnectionManager.
 * 2. Реализация СЦЕНАРИЕВ обмена данными (инициализация после подключения).
 * 3. ПАРСИНГ входящих данных (JsonObject) от BCM и преобразование их в CarData.
 * 4. СИНХРОНИЗАЦИЯ настроек: прием изменений настроек от БК и обновление локального стейта.
 * 5. Хранение и дистрибуция настроек приложения.
 * 6. ФОРМИРОВАНИЕ финального статуса подключения для UI.
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
    }

    /** Область видимости корутин для работы приложения */
    private val appScope = CoroutineScope(Dispatchers.Main + Job())

    /** JSON парсер */
    private val json = Json { ignoreUnknownKeys = true }

    // ========== СОСТОЯНИЯ ==========

    /** Состояние настроек приложения */
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    /** Состояние готовности контроллера */
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** 
     * Внутреннее состояние подключения, управляемое бизнес-логикой.
     * По умолчанию синхронизируется с BCM, но дополняется логическими статусами (REQUESTING_DATA, LISTENING_DATA).
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
     * Базируется на внутреннем состоянии _internalConnectionState.
     */
    val connectionStatusInfo: StateFlow<ConnectionStatusInfo> = _internalConnectionState
        .map { it.toStatusInfo() }
        .stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ConnectionState.UNDEFINED.toStatusInfo()
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

                // 3. Выполняем привязку к Android-сервису Bluetooth через ServiceLocator
                ServiceLocator.bindBluetoothService(context) { service ->
                    // 4. Создаем экземпляр оркестратора подключений (BCM)
                    _bluetoothConnectionManager = BluetoothConnectionManager(context, service)
                    
                    // 5. Передаем в BCM данные целевого устройства из загруженных настроек
                    _bluetoothConnectionManager?.updateSelectedDevice(settings.selectedDevice)
                    
                    // 6. Активируем фоновых слушателей для обработки данных и состояний
                    startMessageListener()
                    startConnectionStateSychronizer()
                    
                    // 7. Сигнализируем системе о завершении инициализации
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
     */
    private suspend fun executeInitialSequence() {
        // 1. Устанавливаем логический статус "Запрос данных" для информирования пользователя
        _internalConnectionState.value = ConnectionState.REQUESTING_DATA

        // 2. Отправляем команду на получение текущих настроек, хранящихся в бортовом компьютере
        sendJsonCommand("""{"command":"GET_SETTINGS"}""")
        
        // 3. Отправляем команду на активацию циклической передачи данных автомобиля (стриминг)
        sendJsonCommand("""{"command":"GET_DATA"}""")
    }

    /**
     * Центральный метод разбора входящих JSON-сообщений.
     * Классифицирует пакеты по ключам (data, settings, error) и распределяет их по подсистемам.
     * 
     * @param jsonObject Объект, полученный из транспортного слоя.
     */
    private fun handleIncomingMessage(jsonObject: JsonObject) {
        try {
            // КЕЙС 1: Пакет содержит оперативные данные автомобиля (телеметрия)
            if (jsonObject.containsKey("data")) {
                // Если это первый пакет данных - переключаем статус в "Прослушивание" (визуальная индикация)
                if (_internalConnectionState.value != ConnectionState.LISTENING_DATA) {
                    _internalConnectionState.value = ConnectionState.LISTENING_DATA
                }

                // Извлекаем вложенный объект данных
                val dataElement = jsonObject["data"]
                if (dataElement != null) {
                    // Десериализуем JSON в доменную модель CarData
                    val carData = json.decodeFromJsonElement<CarData>(dataElement)
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
                AppLogger.logError("БК вернул ошибку: $errorMsg", TAG)
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
                    // Передаем команду в менеджер подключений для анализа смены устройства
                    bluetoothConnectionManager.updateSelectedDevice(newSettings.selectedDevice)
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    /**
     * Разрывает текущее соединение путем сброса выбранного устройства.
     */
    fun disconnectFromDevice() {
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
     * @param jsonCommand Строка в формате JSON.
     */
    fun sendJsonCommand(jsonCommand: String) {
        if (_isInitialized.value) {
            AppLogger.logInfo("Отправка команды: $jsonCommand", TAG)
            bluetoothConnectionManager.sendJsonCommand(jsonCommand)
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
        // Останавливаем все процессы в менеджере подключений
        _bluetoothConnectionManager?.cleanup()
        // Отменяем все активные корутины в области видимости приложения
        appScope.cancel()
        // Сбрасываем флаг готовности
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
