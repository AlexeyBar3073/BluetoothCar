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
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ФАЙЛ: data/bluetooth/BluetoothConnectionManager.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ГЛАВНЫЙ КООРДИНАТОР BLUETOOTH ПОДКЛЮЧЕНИЯ согласно ТЗ.
 * Автономный контроллер, который самостоятельно анализирует полученные настройки
 * и принимает решения о необходимых действиях.
 *
 * ОТВЕТСТВЕННОСТЬ (СОГЛАСНО ТЗ):
 * 1. Получает ВСЕ данные для работы исключительно из объекта AppSettings
 * 2. Анализирует изменение настроек (устройство изменилось или нет)
 * 3. При изменении устройства → запускает процедуру подключения с первого шага
 * 4. При изменении других настроек и isConnected = true → отправляет их в DataStreamHandler
 * 5. Координирует работу 4 помощников (ConnectionFeasibilityChecker, DeviceAvailabilityMonitor,
 *    ConnectionStateManager, DataStreamHandler)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП (СОГЛАСНО ТЗ):
 * - BluetoothConnectionManager получает ВСЕ данные для работы исключительно из объекта AppSettings
 * - AppController НИКОГДА не передает BluetoothDeviceData отдельно от AppSettings
 * - Все помощники работают с BluetoothDeviceData, НЕ с AndroidBluetoothDevice
 * - AppBluetoothService скрывает Android API, предоставляет только Domain модели
 * - Автономная обработка состояний и принятие решений
 * - Все состояния и данные передаются через Flow, а не колбеки
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Получает данные от: AppController.kt (через метод updateSettings())
 * 2. Управляет: 4 помощниками в папке listeners/
 * 3. Использует: AppBluetoothService.kt (через помощников)
 * 4. Передает данные в: AppController.kt (через StateFlow и SharedFlow)
 */
class BluetoothConnectionManager(
    private val context: Context
) {
    /** Тег для логирования компонента */
    private val TAG = "BluetoothConnectionManager"

    // ========== ПОТОКИ ДАННЫХ ДЛЯ APPCONTROLLER ==========

    /** Поток состояний подключения для AppController */
    private val _connectionStateFlow = MutableStateFlow<ConnectionState?>(null)
    val connectionStateFlow: StateFlow<ConnectionState?> = _connectionStateFlow.asStateFlow()

    /** Поток данных от устройства для AppController */
    private val _carDataFlow = MutableSharedFlow<CarData>()
    val carDataFlow: SharedFlow<CarData> = _carDataFlow.asSharedFlow()

    // ========== ВСЕ 4 ПОМОЩНИКА ==========
    /** Сервис для работы с Bluetooth Android API */
    private val bluetoothService = AppBluetoothService().apply { setContext(context) }

    /** Проверяет возможность подключения (шаг 1 по ТЗ) */
    private lateinit var feasibilityChecker: ConnectionFeasibilityChecker
    /** Ищет устройство в эфире (шаг 2 по ТЗ) */
    private lateinit var deviceAvailabilityMonitor: DeviceAvailabilityMonitor
    /** Устанавливает физическое соединение (шаг 3 по ТЗ) */
    private lateinit var connectionStateManager: ConnectionStateManager
    /** Обрабатывает обмен данными (шаг 4 по ТЗ) */
    private lateinit var dataStreamHandler: DataStreamHandler

    // ========== КОРУТИНЫ И ОБЛАСТЬ ВИДИМОСТИ ==========
    /** Область видимости корутин для управления асинхронными операциями */
    private val managerScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ========== СОХРАНЕННЫЕ ДАННЫЕ ==========
    // СОГЛАСНО ТЗ: BluetoothConnectionManager хранит текущие данные из настроек
    /** Текущее выбранное Bluetooth устройство из настроек */
    private var currentBluetoothDeviceData: BluetoothDeviceData? = null
    /** Текущие настройки приложения из AppController */
    private var currentAppSettings: AppSettings? = null

    // ========== СТАТУС И ЛОГИРОВАНИЕ ==========

    /** Текущее состояние соединения (для упрощения контроля согласно ТЗ) */
    private var isConnected: Boolean = false

    /**
     * Записать информационное сообщение в лог.
     * Вызывается: из всех методов класса для логирования действий.
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
        startCollectingCarData()
        log("BluetoothConnectionManager инициализирован, 4 помощника созданы")
    }

    /**
     * Создать всех 4 помощников и передать им AppBluetoothService.
     * Вызывается: из init блока при инициализации класса.
     * СОГЛАСНО ТЗ: BluetoothConnectionManager инициализирует всех помощников.
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
        log("Все 4 помощника созданы с передачей AppBluetoothService")
    }

    /**
     * Запустить сбор данных от DataStreamHandler.
     * Подписывается на поток данных от DataStreamHandler и транзитом эмитит в свой поток.
     * Вызывается: из init блока после создания помощников.
     */
    private fun startCollectingCarData() {
        managerScope.launch {
            dataStreamHandler.carDataFlow.collect { carData ->
                // Транзитом эмитируем данные в свой поток
                _carDataFlow.emit(carData)
            }
        }
    }

    /**
     * Обработать изменение состояния подключения от помощников.
     * Эмитирует состояние в поток connectionStateFlow.
     * Вызывается: помощниками (DeviceAvailabilityMonitor, ConnectionFeasibilityChecker, ConnectionStateManager, DataStreamHandler).
     * @param state Новое состояние подключения
     * @param errorMessage Сообщение об ошибке или null если ошибки нет
     */
    private fun handleConnectionState(state: ConnectionState, errorMessage: String? = null) {

        // Эмитируем состояние в поток
        _connectionStateFlow.value = state
        log("Получено оповещение о новом состоянии подключения ${state.name}")

        // Обрабатываем состояние
        when (state) {
            // Получена ошибка
            ConnectionState.ERROR -> {
                // Шаг подключения завершился ошибкой
                log("Получено состояние от помощника: ${state.name}${errorMessage?.let { " с ошибкой: $it" } ?: ""}")
                errorMessage?.let {
                    managerScope.launch {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }
            }

            // Целевое устройство выбрано и проверено, блютуз включен
            ConnectionState.DEVICE_SELECTED -> {
                // Переходим к проверке физической доступности устройства
                deviceAvailabilityMonitor.start(currentBluetoothDeviceData ?: return)
            }

            // Целевое устройство найдена в окружении хост-устройства
            ConnectionState.DEVICE_AVAILABLE -> {
                // Переходим к подключению к устройству
                connectionStateManager.start(currentBluetoothDeviceData ?: return)
            }

            // Подключение к целевому устройству выполнено
            ConnectionState.CONNECTED -> {
                // Устанавливаем флаг наличия подключения
                isConnected = true
                // Переходим к обмену данными
                startDataStreamHandler()
            }

            // Произошел разрыв подключения к целевому устройству
            ConnectionState.DISCONNECTED -> {
                // Сбрасываем признак подключения
                isConnected = false
                // Возвращаемся к первому шагу
                managerScope.launch {
                    _connectionStateFlow.value = ConnectionState.UNDEFINED
                    startConnectionProcess()
                }
            }

            else -> {
                // Для остальных состояний показываем сообщение, если оно есть
                errorMessage?.let {
                    managerScope.launch {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // ========== ПУБЛИЧНЫЙ API ДЛЯ APPCONTROLLER ==========

    /**
     * Получить список сопряженных Bluetooth устройств.
     * Делегирует вызов AppBluetoothService.
     * Вызывается: AppController.kt для получения списка устройств для UI.
     * @return Список устройств или null при ошибке
     */
    fun getPairedDevices(): List<BluetoothDeviceData>? {
        return bluetoothService.getPairedDevices()
    }

    /**
     * Проверить, включен ли Bluetooth адаптер.
     * Делегирует вызов AppBluetoothService.
     * Вызывается: AppController.kt для проверки состояния Bluetooth в UI.
     * @return true если Bluetooth включен
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothService.bluetoothAdapterIsEnabled()
    }

    /**
     * Обработать новые настройки от AppController.
     * СОГЛАСНО ТЗ: BluetoothConnectionManager получает ВСЕ данные для работы исключительно из объекта AppSettings.
     * Вызывается: AppController.kt при изменении настроек пользователем.
     * @param newSettings Новые настройки приложения
     */
    fun updateSettings(newSettings: AppSettings) {
        log("Получены новые настройки от AppController")

        // СОГЛАСНО ТЗ: Сохраняет в своих свойствах полученные настройки
        this.currentAppSettings = newSettings

        // СОГЛАСНО ТЗ: Извлекает из настроек БК
        val newDeviceData = newSettings.selectedDevice

        // СОГЛАСНО ТЗ: Сравнивает полученное БК с текущим БК
        val isDeviceChanged = (currentBluetoothDeviceData?.address != newDeviceData?.address||newDeviceData==null)

        // СОГЛАСНО ТЗ: После сравнения сохраняет полученное устройство как текущее
        currentBluetoothDeviceData = newDeviceData

        // СОГЛАСНО ТЗ: По результатам сравнения текущего БК с полученным BluetoothConnectionManager выполняет действия
        when {
            // 1. Устройство изменилось: запускаем процедуру подключения с первого шага
            isDeviceChanged -> {
                log("Устройство изменилось, запускаем процедуру подключения с первого шага")
                startConnectionProcess()
            }

            // 2. Устройство НЕ изменилось и статус системы isConnected = true: вызывает метод отправки настроек у класса DataStreamHandler
            !isDeviceChanged && isConnected -> {
                log("Устройство не изменилось и isConnected = true, отправка настроек в DataStreamHandler")
                dataStreamHandler.updateAppSettings(newSettings)
            }

            // 3. При других вариантах - ничего не делает
            else -> {
                log("Устройство не изменилось, нет подключения, сохраняет настройки")
                // Настройки уже сохранены в currentAppSettings
            }
        }
    }

    /**
     * Стандартный метод для запуска процесса подключения.
     * СОГЛАСНО ТЗ (Поток 6): AppController вызывает этот метод при ручном переподключении.
     * Также используется внутри класса при изменении устройства в updateSettings().
     * Вызывается: AppController.kt при нажатии кнопки "Повторить" в UI.
     */
    fun startConnectionProcess() {
        log("Запуск стандартного процесса подключения")

        stopAllProcesses()
        feasibilityChecker.start(currentBluetoothDeviceData)
    }

    /**
     * Начать обмен данными с устройством (Шаг 4 по ТЗ).
     * Вызывает DataStreamHandler.
     * Вызывается: из handleConnectionState() при получении CONNECTED.
     */
    private fun startDataStreamHandler() {
        val deviceData = currentBluetoothDeviceData
        val settings = currentAppSettings

        if (deviceData == null || settings == null) {
            return
        }

        dataStreamHandler.start(
            deviceData = deviceData,
            settings = settings
        )
    }

    /**
     * Отправить произвольную JSON команду на устройство.
     * @param jsonCommand Строка в формате JSON
     */
    fun sendJsonCommand(jsonCommand: String) {
        log("Пересылка JSON команды в DataStreamHandler: $jsonCommand")
        dataStreamHandler.sendJsonCommand(jsonCommand)
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Очистить ресурсы BluetoothConnectionManager.
     * Используется при завершении работы приложения.
     * Вызывается: AppController.kt при уничтожении приложения.
     */
    fun cleanup() {
        log("Очистка ресурсов BluetoothConnectionManager")
        stopAllProcesses()
        managerScope.cancel()
        currentBluetoothDeviceData = null
        currentAppSettings = null
        isConnected = false
        log("BluetoothConnectionManager очищен")
    }

    /**
     * Получить текстовую статистику подключения.
     * Используется для отладки и логирования.
     * Вызывается: для отладки или отображения диагностической информации.
     * @return Строка со статистики подключения
     */
    fun getConnectionStatistics(): String {
        val currentState = _connectionStateFlow.value
        return "Статус: ${currentState?.name ?: "null"}, Устройство: ${currentBluetoothDeviceData?.name ?: "нет"}, isConnected: $isConnected"
    }

    /**
     * Сбросить статистику подключения.
     * TODO: Реализовать сбор статистики в помощниках.
     * Вызывается: для сброса диагностической информации.
     */
    fun resetStatistics() {
        log("Сброс статистики подключения")
        // TODO: Реализовать сбор статистики в помощниках
    }

    // ========== ВНУТРЕННИЕ МЕТОДЫ ==========

    /**
     * Остановить все процессы BluetoothConnectionManager.
     * Используется при cleanup() и при перезапуске.
     * Вызывается: из cleanup(), startConnectionProcess() и других методов при необходимости остановки.
     * ВНУТРЕННИЙ МЕТОД: не является частью публичного API.
     */
    private fun stopAllProcesses() {
        log("Остановка всех процессов BluetoothConnectionManager")
        _connectionStateFlow.value = ConnectionState.UNDEFINED
        feasibilityChecker.stop()
        deviceAvailabilityMonitor.stop()
        connectionStateManager.stop()
        dataStreamHandler.stop()
        log("Все процессы остановлены")
    }
}
