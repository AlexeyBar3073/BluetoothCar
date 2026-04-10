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
import kotlinx.serialization.json.JsonObject

/**
 * ТЕГ: BLUETOOTH_CONNECTION_ORCHESTRATOR
 *
 * ФАЙЛ: data/bluetooth/BluetoothConnectionManager.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Главный координатор Bluetooth соединения, реализующий State Machine процесса подключения.
 * Оркестрирует работу четырех специализированных помощников для обеспечения надежной
 * и реактивной связи с устройством.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Оркестрация 4-х специализированных помощников (Checker, Monitor, StateManager, DataStreamHandler).
 * 2. Управление жизненным циклом соединения: от проверки прав до обмена данными.
 * 3. Транзит входящих JSON-сообщений от транспортного слоя к [AppController].
 * 4. Обеспечение реактивного обновления состояния подключения через [connectionStateFlow].
 * 5. Обработка ошибок и уведомление пользователя через Toast.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Паттерн "Оркестратор": разделение сложной логики подключения на атомарные шаги-помощники.
 * - Reactive Data Flow: использование Kotlin SharedFlow/StateFlow для передачи данных и состояний.
 * - Принцип единственной ответственности: менеджер только координирует, не зная деталей реализации каждого шага.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Изоляция бизнес-логики управления Bluetooth-сессией от UI и низкоуровневых системных вызовов 
 * через четко определенную последовательность состояний (State Machine).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: AppBluetoothService.kt (Низкоуровневый транспорт)
 * - Использует: AppLogger.kt (Диагностика)
 * - Вызывается из: AppController.kt (Бизнес-логика приложения)
 * - Взаимодействует: SharedViewModel.kt (Отображение состояний в UI)
 * - Управляет: Checker, Monitor, StateManager, DataStreamHandler (в data/bluetooth/listeners/)
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

    /** Поток входящих сообщений (JsonObject) от устройства для AppController */
    private val _incomingMessagesFlow = MutableSharedFlow<JsonObject>()
    val incomingMessagesFlow: SharedFlow<JsonObject> = _incomingMessagesFlow.asSharedFlow()

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
    /** Текущее выбранное Bluetooth устройство из настроек. Всегда non-null. */
    private var currentBluetoothDeviceData: BluetoothDeviceData = BluetoothDeviceData.empty()

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
     * Запустить сбор сообщений от DataStreamHandler.
     * Транслирует входящий поток объектов в свой поток для AppController.
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
     * Является центральным узлом управления переходами (State Machine).
     *
     * @param state Новое состояние подключения от одного из 4-х помощников.
     * @param errorMessage Опциональное описание ошибки для уведомления пользователя.
     */
    private fun handleConnectionState(state: ConnectionState, errorMessage: String? = null) {
        // 1. Реагируем на изменение состояния согласно бизнес-логике переходов
        // ВАЖНО: Сначала готовим компоненты (транспорт), потом уведомляем об изменении состояния,
        // чтобы избежать гонок (race conditions), когда подписчики (AppController) начинают слать 
        // команды в еще не запущенный транспортный слой.
        when (state) {
            ConnectionState.CONNECTED -> {
                // Шаг 4: Соединение установлено, активируем транспортный шлюз данных
                isConnected = true
                startDataStreamHandler()
            }

            ConnectionState.DEVICE_SELECTED -> {
                // Шаг 2: После выбора устройства начинаем мониторинг его присутствия в эфире
                deviceAvailabilityMonitor.start(currentBluetoothDeviceData)
            }

            ConnectionState.DEVICE_AVAILABLE -> {
                // Шаг 3: Устройство найдено, пытаемся установить физическое соединение
                connectionStateManager.start(currentBluetoothDeviceData)
            }

            ConnectionState.DISCONNECTED -> {
                // Обработка разрыва соединения: сброс флагов и инициация авто-переподключения
                isConnected = false
                managerScope.launch {
                    startConnectionProcess()
                }
            }

            ConnectionState.ERROR -> {
                log("Ошибка от помощника: ${state.name}${errorMessage?.let { ": $it" } ?: ""}")
                errorMessage?.let {
                    managerScope.launch {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
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

        // 2. Обновляем поток состояния для подписчиков (UI и AppController)
        _connectionStateFlow.value = state
        log("Получено оповещение о новом состоянии подключения ${state.name}")
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
     * Обновить выбранное Bluetooth устройство.
     * Если адрес устройства изменился — инициирует перезапуск процесса подключения.
     *
     * @param device Данные нового выбранного устройства (не null).
     */
    fun updateSelectedDevice(device: BluetoothDeviceData) {
        log("Получено обновление выбранного устройства: ${device.name}")

        val isDeviceChanged = currentBluetoothDeviceData.address != device.address
        currentBluetoothDeviceData = device

        if (isDeviceChanged) {
            log("Устройство изменилось, перезапуск процесса подключения")
            startConnectionProcess()
        } else {
            log("Устройство не изменилось, действие не требуется")
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
        currentBluetoothDeviceData = BluetoothDeviceData.empty()
        isConnected = false
    }

    /** Статистика для отладки */
    fun getConnectionStatistics(): String {
        val currentState = _connectionStateFlow.value
        val deviceName = if (currentBluetoothDeviceData.address.isEmpty()) "не выбрано" else currentBluetoothDeviceData.name
        return "Статус: ${currentState?.name ?: "null"}, Устройство: $deviceName, isConnected: $isConnected"
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
