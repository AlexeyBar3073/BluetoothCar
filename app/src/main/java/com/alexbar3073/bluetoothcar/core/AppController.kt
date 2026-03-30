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
 * 3. ПАРСИНГ входящих сырых данных от BCM и преобразование их в CarData.
 * 4. СИНХРОНИЗАЦИЯ настроек: прием изменений настроек от БК и обновление локального стейта.
 * 5. Хранение и дистрибуция настроек приложения.
 * 6. ФОРМИРОВАНИЕ финального статуса подключения для UI.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Разделение ответственности: Транспорт (DSH) и Оркестратор (BCM) не знают о бизнес-логике.
 * - Централизация: Вся логика «что и когда отправлять» сосредоточена здесь.
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
     * Формируется путем парсинга входящих сырых сообщений от BCM.
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

    private fun startInitialization() {
        appScope.launch {
            try {
                val settings = settingsRepository.getCurrentSettings()
                _appSettings.value = settings
                AppLogger.configure(verbose = true, timestamps = true, packetNumbers = true)

                ServiceLocator.bindBluetoothService(context) { service ->
                    _bluetoothConnectionManager = BluetoothConnectionManager(context, service)
                    _bluetoothConnectionManager?.updateSettings(settings)
                    
                    // Запускаем слушателей
                    startMessageListener()
                    startConnectionStateSychronizer()
                    
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
     * Слушает сырые сообщения от BCM и парсит их.
     */
    private fun startMessageListener() {
        appScope.launch {
            bluetoothConnectionManager.incomingMessagesFlow.collect { rawJson ->
                handleIncomingMessage(rawJson)
            }
        }
    }

    /**
     * Синхронизирует внутреннее состояние со статусом от BCM.
     * Также отслеживает переход в CONNECTED для запуска сценария.
     */
    private fun startConnectionStateSychronizer() {
        appScope.launch {
            bluetoothConnectionManager.connectionStateFlow.collect { state ->
                val newState = state ?: ConnectionState.UNDEFINED
                
                // Синхронизируем базовые статусы
                _internalConnectionState.value = newState
                
                if (newState == ConnectionState.CONNECTED) {
                    log("Состояние CONNECTED: Запуск сценария инициализации обмена")
                    executeInitialSequence()
                }
            }
        }
    }

    /**
     * Первичная очередь команд согласно бизнес-логике.
     */
    private suspend fun executeInitialSequence() {
        // Устанавливаем логический статус "Запрос данных"
        _internalConnectionState.value = ConnectionState.REQUESTING_DATA

        // 1. Запрос текущих настроек БК
        sendJsonCommand("""{"command":"GET_SETTINGS"}""")
        
        // 2. Запрос на начало стриминга данных
        sendJsonCommand("""{"command":"GET_DATA"}""")
    }

    /**
     * Разбор входящего сообщения.
     */
    private fun handleIncomingMessage(rawJson: String) {
        try {
            val jsonObject = json.parseToJsonElement(rawJson).jsonObject
            
            // 1. Пакет с данными автомобиля
            if (jsonObject.containsKey("data")) {
                // Если пошли данные - переключаем статус в "Стриминг" (вилка)
                if (_internalConnectionState.value != ConnectionState.LISTENING_DATA) {
                    _internalConnectionState.value = ConnectionState.LISTENING_DATA
                }

                val dataString = jsonObject["data"].toString()
                val carData = json.decodeFromString<CarData>(dataString)
                appScope.launch { _rawCarDataFlow.emit(carData) }
            }
            
            // 2. Пакет с настройками от БК
            if (jsonObject.containsKey("settings")) {
                val settingsFromRemote = jsonObject["settings"]?.jsonObject
                if (settingsFromRemote != null) {
                    syncSettingsFromRemote(settingsFromRemote)
                }
            }
            
            // 3. Ошибка от БК
            jsonObject["error"]?.jsonPrimitive?.content?.let { errorMsg ->
                AppLogger.logError("БК вернул ошибку: $errorMsg", TAG)
            }
            
        } catch (e: Exception) {
            AppLogger.logError("Ошибка парсинга: ${e.message}", TAG)
        }
    }

    /**
     * Синхронизирует настройки, полученные от БК.
     */
    private fun syncSettingsFromRemote(remoteJson: JsonObject) {
        val current = _appSettings.value
        val updated = current.mergeWithRemote(remoteJson)

        if (updated !== current) {
            log("Настройки от БК отличаются от локальных. Синхронизация...")
            updateSettings(updated, fromRemote = true)
        }
    }

    // ========== ПУБЛИЧНЫЙ API ==========

    fun getPairedDevices(): List<BluetoothDeviceData>? = 
        if (_isInitialized.value) bluetoothConnectionManager.getPairedDevices() else null

    fun isBluetoothEnabled(): Boolean = 
        if (_isInitialized.value) bluetoothConnectionManager.isBluetoothEnabled() else false

    fun getCurrentSettings(): AppSettings = _appSettings.value

    fun updateSettings(newSettings: AppSettings, fromRemote: Boolean = false) {
        appScope.launch {
            try {
                val current = _appSettings.value
                if (current == newSettings) return@launch

                settingsRepository.saveSettings(newSettings)
                _appSettings.value = newSettings
                
                if (_isInitialized.value && !fromRemote) {
                    // Если меняем настройки вручную - ставим статус SENDING_SETTINGS
                    _internalConnectionState.value = ConnectionState.SENDING_SETTINGS
                    bluetoothConnectionManager.updateSettings(newSettings)
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    fun disconnectFromDevice() {
        updateSettings(_appSettings.value.copy(selectedDevice = BluetoothDeviceData.empty()))
    }

    fun clearSelectedDevice() {
        updateSettings(_appSettings.value.copy(selectedDevice = BluetoothDeviceData.empty()))
    }

    fun retryConnection() {
        if (_isInitialized.value) bluetoothConnectionManager.startConnectionProcess()
    }

    fun getCurrentCarData(): CarData = carData.value

    fun getConnectionStatistics(): String = 
        if (_isInitialized.value) bluetoothConnectionManager.getConnectionStatistics() else "Инициализация..."

    fun resetConnectionStatistics() {
        if (_isInitialized.value) bluetoothConnectionManager.resetStatistics()
    }

    fun sendJsonCommand(jsonCommand: String) {
        if (_isInitialized.value) {
            AppLogger.logInfo("Отправка команды: $jsonCommand", TAG)
            bluetoothConnectionManager.sendJsonCommand(jsonCommand)
        }
    }

    private fun log(message: String) {
        AppLogger.logInfo(message, TAG)
    }

    // ========== УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ ==========

    fun cleanup() {
        _bluetoothConnectionManager?.cleanup()
        appScope.cancel()
        _isInitialized.value = false
    }

    fun reload() {
        cleanup()
        appScope.launch {
            delay(1000)
            startInitialization()
        }
    }
}
