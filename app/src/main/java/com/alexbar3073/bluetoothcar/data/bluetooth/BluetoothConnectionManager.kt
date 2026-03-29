// Файл: data/bluetooth/BluetoothConnectionManager.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import android.content.Context
import android.widget.Toast
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.ConnectionFeasibilityChecker
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.DeviceAvailabilityMonitor
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.ConnectionStateManager
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.DataStreamHandler
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ФАЙЛ: data/bluetooth/BluetoothConnectionManager.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ГЛАВНЫЙ КООРДИНАТОР BLUETOOTH ПОДКЛЮЧЕНИЯ.
 * Оркестратор, управляющий жизненным циклом канала связи и состоянием помощников.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Координация работы 4 помощников (Checker, Monitor, StateManager, DataStreamHandler).
 * 2. Управление состояниями подключения (от поиска до установленного соединения).
 * 3. Транзит «сырых» данных из транспортного шлюза (DSH) в сторону AppController.
 * 4. Предоставление интерфейса для отправки команд в очередь транспорта.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Оркестрация: BCM знает КАК установить связь, но не знает ЧТО по ней передается.
 * - Сквозная передача: Входящие сообщения от DSH передаются в AppController без парсинга.
 * - Отсутствие бизнес-логики: BCM не анализирует содержимое пакетов (например, CarData).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Получает команды от: AppController.kt.
 * 2. Управляет: помощниками в папке listeners/.
 * 3. Транслирует данные в: AppController.kt (через incomingMessagesFlow).
 */
class BluetoothConnectionManager(
    private val context: Context,
    private val bluetoothService: AppBluetoothService
) {
    /** Тег для логирования компонента */
    private val TAG = "BluetoothConnectionManager"

    // ========== ПОТОКИ ДАННЫХ ДЛЯ APPCONTROLLER ==========

    /** Поток состояний подключения для AppController */
    private val _connectionStateFlow = MutableStateFlow<ConnectionState?>(null)
    val connectionStateFlow: StateFlow<ConnectionState?> = _connectionStateFlow.asStateFlow()

    /** Поток входящих сообщений (сырой JSON) от устройства для AppController */
    private val _incomingMessagesFlow = MutableSharedFlow<String>()
    val incomingMessagesFlow: SharedFlow<String> = _incomingMessagesFlow.asSharedFlow()

    // ========== ВСЕ 4 ПОМОЩНИКА ==========

    /** Проверяет возможность подключения (шаг 1) */
    private lateinit var feasibilityChecker: ConnectionFeasibilityChecker
    /** Ищет устройство в эфире (шаг 2) */
    private lateinit var deviceAvailabilityMonitor: DeviceAvailabilityMonitor
    /** Устанавливает физическое соединение (шаг 3) */
    private lateinit var connectionStateManager: ConnectionStateManager
    /** Обрабатывает транспортный уровень обмена данными (шаг 4) */
    private lateinit var dataStreamHandler: DataStreamHandler

    // ========== КОРУТИНЫ И ОБЛАСТЬ ВИДИМОСТИ ==========
    /** Область видимости корутин для управления асинхронными операциями */
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ========== СОХРАНЕННЫЕ ДАННЫЕ ==========
    /** Текущее выбранное Bluetooth устройство из настроек */
    private var currentBluetoothDeviceData: BluetoothDeviceData? = null
    /** Текущие настройки приложения (нужны для формирования JSON при обновлении) */
    private var currentAppSettings: AppSettings? = null

    // ========== СТАТУС И ЛОГИРОВАНИЕ ==========

    /** Текущее состояние соединения (активен ли транспорт) */
    private var isConnected: Boolean = false

    /**
     * Записать информационное сообщение в лог.
     * @param message Текст сообщения для логирования
     */
    private fun log(message: String) {
        AppLogger.logInfo(message, TAG)
    }

    // ========== ИНИЦИАЛИЗАЦИЯ ==========
    init {
        log("Инициализация BluetoothConnectionManager")
        _connectionStateFlow.value = ConnectionState.UNDEFINED
        createAllHelpers()
        startCollectingIncomingMessages()
        log("BluetoothConnectionManager инициализирован, 4 помощника созданы")
    }

    /**
     * Создать всех 4 помощников.
     */
    private fun createAllHelpers() {
        log("Создание всех 4 помощников")

        feasibilityChecker = ConnectionFeasibilityChecker(
            bluetoothService = bluetoothService,
            stateChangeCallback = ::handleConnectionState
        )

        deviceAvailabilityMonitor = DeviceAvailabilityMonitor(
            bluetoothService = bluetoothService,
            stateChangeCallback = ::handleConnectionState
        )

        connectionStateManager = ConnectionStateManager(
            bluetoothService = bluetoothService,
            stateChangeCallback = ::handleConnectionState
        )

        dataStreamHandler = DataStreamHandler(
            bluetoothService = bluetoothService,
            coroutineScope = managerScope,
            stateChangeCallback = ::handleConnectionState
        )
        log("Все 4 помощника созданы")
    }

    /**
     * Запустить сбор сырых сообщений от DataStreamHandler.
     * Транслирует входящий поток строк в свой поток для AppController.
     */
    private fun startCollectingIncomingMessages() {
        managerScope.launch {
            dataStreamHandler.incomingMessagesFlow.collect { message ->
                _incomingMessagesFlow.emit(message)
            }
        }
    }

    /**
     * Обработать изменение состояния подключения от помощников.
     * @param state Новое состояние подключения
     * @param errorMessage Сообщение об ошибке или null если ошибки нет
     */
    private fun handleConnectionState(state: ConnectionState, errorMessage: String? = null) {

        _connectionStateFlow.value = state
        log("Получено оповещение о новом состоянии подключения ${state.name}")

        when (state) {
            ConnectionState.ERROR -> {
                log("Ошибка от помощника: ${state.name}${errorMessage?.let { ": $it" } ?: ""}")
                errorMessage?.let {
                    managerScope.launch {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }
            }

            ConnectionState.DEVICE_SELECTED -> {
                deviceAvailabilityMonitor.start(currentBluetoothDeviceData ?: return)
            }

            ConnectionState.DEVICE_AVAILABLE -> {
                connectionStateManager.start(currentBluetoothDeviceData ?: return)
            }

            ConnectionState.CONNECTED -> {
                isConnected = true
                startDataStreamHandler()
            }

            ConnectionState.DISCONNECTED -> {
                isConnected = false
                managerScope.launch {
                    _connectionStateFlow.value = ConnectionState.UNDEFINED
                    startConnectionProcess()
                }
            }

            else -> {
                errorMessage?.let {
                    managerScope.launch {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // ========== ПУБЛИЧНЫЙ API ДЛЯ APPCONTROLLER ==========

    /** Получает список сопряженных Bluetooth устройств */
    fun getPairedDevices(): List<BluetoothDeviceData>? {
        return bluetoothService.getPairedDevices()
    }

    /** Проверяет статус Bluetooth адаптера */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothService.bluetoothAdapterIsEnabled()
    }

    /**
     * Обновить настройки. 
     * Если устройство изменилось — перезапуск. 
     * Если только настройки при живом соединении — отправка в очередь.
     */
    fun updateSettings(newSettings: AppSettings) {
        log("Получены новые настройки")
        this.currentAppSettings = newSettings
        val newDeviceData = newSettings.selectedDevice
        val isDeviceChanged = (currentBluetoothDeviceData?.address != newDeviceData?.address || newDeviceData == null)
        currentBluetoothDeviceData = newDeviceData

        when {
            isDeviceChanged -> {
                log("Устройство изменилось, перезапуск процесса подключения")
                startConnectionProcess()
            }
            !isDeviceChanged && isConnected -> {
                log("Обновление настроек при активном соединении")
                sendJsonCommand("""{"settings":${newSettings.toDeviceSettingsJson()}}""")
            }
        }
    }

    /** Запуск процесса подключения с первого шага */
    fun startConnectionProcess() {
        log("Запуск стандартного процесса подключения")
        stopAllProcesses()
        feasibilityChecker.start(currentBluetoothDeviceData)
    }

    /**
     * Запустить транспортный шлюз.
     */
    private fun startDataStreamHandler() {
        dataStreamHandler.start()
    }

    /**
     * Отправить JSON команду на устройство через транспортную очередь.
     * @param jsonCommand Строка в формате JSON
     */
    fun sendJsonCommand(jsonCommand: String) {
        dataStreamHandler.sendJsonCommand(jsonCommand)
    }

    // ========== УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ ==========

    /** Очистить ресурсы */
    fun cleanup() {
        log("Очистка ресурсов BluetoothConnectionManager")
        stopAllProcesses()
        managerScope.cancel()
        currentBluetoothDeviceData = null
        currentAppSettings = null
        isConnected = false
    }

    /** Статистика для отладки */
    fun getConnectionStatistics(): String {
        val currentState = _connectionStateFlow.value
        return "Статус: ${currentState?.name ?: "null"}, Устройство: ${currentBluetoothDeviceData?.name ?: "нет"}, isConnected: $isConnected"
    }

    fun resetStatistics() {
        log("Сброс статистики подключения")
    }

    private fun stopAllProcesses() {
        log("Остановка всех процессов")
        _connectionStateFlow.value = ConnectionState.UNDEFINED
        feasibilityChecker.stop()
        deviceAvailabilityMonitor.stop()
        connectionStateManager.stop()
        dataStreamHandler.stop()
    }
}
