//ФАЙЛ: core/AppController.kt
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

/**
 * ТЕГ: Главный координатор
 * ФАЙЛ: core/AppController.kt
 * МЕСТОНАХОЖДЕНИЕ: core/
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Центральный узел бизнес-логики приложения. Координирует работу UI 
 * и BluetoothConnectionManager.
 * ОТВЕТСТВЕННОСТЬ: Управление жизненным циклом компонентов, хранение настроек, агрегация данных.
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Single Responsibility.
 * КЛЮЧЕВОЙ ПРИНЦИП: Реактивность через StateFlow.
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: SharedViewModel, BluetoothConnectionManager, SettingsRepository.
 */
class AppController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        private const val TAG = "AppController"
    }

    /** Область видимости корутин для работы приложения */
    private val appScope = CoroutineScope(Dispatchers.Main + Job())

    // ========== СОСТОЯНИЯ ==========

    /** Состояние настроек приложения */
    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    /** Состояние готовности контроллера */
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ========== BLUETOOTH МЕНЕДЖЕР ==========

    /** Менеджер управления Bluetooth-соединением */
    private val bluetoothConnectionManager: BluetoothConnectionManager by lazy {
        BluetoothConnectionManager(context)
    }

    // ========== ПОТОКИ ДАННЫХ ==========

    /**
     * Поток данных автомобиля.
     * Объединяет сырые данные из менеджера с текущими настройками.
     */
    val carData: StateFlow<CarData> by lazy {
        combine(
            bluetoothConnectionManager.carDataFlow,
            _appSettings,
            connectionStatusInfo
        ) { carDataValue, settings, connectionStatus ->
            // Если соединение не активно, данные обнуляются
            if (!connectionStatus.isActive) {
                CarData() 
            } else {
                // Применение порога низкого уровня топлива на основе настроек
                val minFuel = settings.minFuelLevel
                carDataValue.copy(isFuelLow = carDataValue.fuel < minFuel)
            }
        }.stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarData() 
        )
    }

    /** Поток информации о состоянии подключения */
    val connectionStatusInfo: StateFlow<ConnectionStatusInfo> by lazy {
        bluetoothConnectionManager.connectionStateFlow
            .map { it?.toStatusInfo() ?: ConnectionState.UNDEFINED.toStatusInfo() }
            .stateIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ConnectionState.UNDEFINED.toStatusInfo()
            )
    }

    // ========== ИНИЦИАЛИЗАЦИЯ ==========

    init {
        AppLogger.logInfo("Инициализация AppController", TAG)
        initialize()
    }

    /**
     * Выполняет инициализацию контроллера: загрузку настроек и запуск процессов.
     */
    private fun initialize() {
        appScope.launch {
            try {
                // 1. Загружаем настройки
                val settings = settingsRepository.getCurrentSettings()
                _appSettings.value = settings

                // Настройка параметров логгера
                AppLogger.configure(verbose = true, timestamps = true, packetNumbers = true)

                // 2. Инициализируем менеджер настроек в BCM
                // BCM сам определит необходимость запуска процесса подключения
                bluetoothConnectionManager.updateSettings(settings)
                
                // Установка флага завершения инициализации
                _isInitialized.value = true

            } catch (e: Exception) {
                AppLogger.logError("Критическая ошибка инициализации: ${e.message}", TAG)
                _isInitialized.value = false
            }
        }
    }

    // ========== ПУБЛИЧНЫЙ API ==========

    /** Получает список сопряженных устройств из Bluetooth адаптера */
    fun getPairedDevices(): List<BluetoothDeviceData>? = bluetoothConnectionManager.getPairedDevices()

    /** Проверяет статус Bluetooth адаптера */
    fun isBluetoothEnabled(): Boolean = bluetoothConnectionManager.isBluetoothEnabled()

    /** Возвращает текущий снимок настроек */
    fun getCurrentSettings(): AppSettings = _appSettings.value

    /** 
     * Сохраняет новые настройки и уведомляет все заинтересованные компоненты. 
     */
    fun updateSettings(newSettings: AppSettings) {
        appScope.launch {
            try {
                // Сохранение в постоянное хранилище
                settingsRepository.saveSettings(newSettings)
                _appSettings.value = newSettings
                // Передача настроек в менеджер соединений
                bluetoothConnectionManager.updateSettings(newSettings)
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    /** Отключает соединение, очищая выбранное устройство */
    fun disconnectFromDevice() {
        val updatedSettings = _appSettings.value.copy(selectedDevice = null)
        updateSettings(updatedSettings)
    }

    /** Удаляет выбранное устройство из настроек */
    fun clearSelectedDevice() {
        val updatedSettings = _appSettings.value.copy(selectedDevice = null)
        updateSettings(updatedSettings)
    }

    /** Принудительно перезапускает процесс установки соединения */
    fun retryConnection() {
        bluetoothConnectionManager.startConnectionProcess()
    }

    /** Получает текущее состояние данных автомобиля */
    fun getCurrentCarData(): CarData = carData.value

    /** Получает строковое представление статистики соединения */
    fun getConnectionStatistics(): String = bluetoothConnectionManager.getConnectionStatistics()

    /** Обнуляет накопленную статистику пакетов и ошибок */
    fun resetConnectionStatistics() {
        bluetoothConnectionManager.resetStatistics()
    }

    /** Отправляет JSON команду на внешнее устройство */
    fun sendJsonCommand(jsonCommand: String) {
        bluetoothConnectionManager.sendJsonCommand(jsonCommand)
    }

    // ========== УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ ==========

    /** 
     * Завершает все фоновые процессы и очищает ресурсы. 
     */
    fun cleanup() {
        bluetoothConnectionManager.cleanup()
        appScope.cancel()
        _isInitialized.value = false
    }

    /** 
     * Выполняет процедуру перезагрузки контроллера. 
     */
    fun reload() {
        cleanup()
        appScope.launch {
            // Задержка перед инициализацией для завершения очистки
            delay(1000)
            initialize()
        }
    }
}
