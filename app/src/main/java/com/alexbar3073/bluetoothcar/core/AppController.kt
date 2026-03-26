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

/**
 * ТЕГ: Главный координатор
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Центральный узел бизнес-логики приложения. Координирует работу UI 
 * и BluetoothConnectionManager.
 */
class AppController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        private const val TAG = "AppController"
    }

    private val appScope = CoroutineScope(Dispatchers.Main + Job())

    // ========== СОСТОЯНИЯ ==========

    private val _appSettings = MutableStateFlow(AppSettings())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ========== BLUETOOTH МЕНЕДЖЕР ==========

    private val bluetoothConnectionManager: BluetoothConnectionManager by lazy {
        BluetoothConnectionManager(context)
    }

    // ========== ПОТОКИ ДАННЫХ ==========

    val carData: StateFlow<CarData> by lazy {
        combine(
            bluetoothConnectionManager.carDataFlow,
            _appSettings,
            connectionStatusInfo
        ) { carDataValue, settings, connectionStatus ->
            if (!connectionStatus.isActive) {
                CarData() 
            } else {
                val minFuel = settings.minFuelLevel
                carDataValue.copy(isFuelLow = carDataValue.fuel < minFuel)
            }
        }.stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarData() 
        )
    }

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

    private fun initialize() {
        appScope.launch {
            try {
                // 1. Загружаем настройки
                val settings = settingsRepository.getCurrentSettings()
                _appSettings.value = settings

                AppLogger.configure(verbose = true, timestamps = true, packetNumbers = true)

                // 2. Инициализируем менеджер настроек в BCM
                bluetoothConnectionManager.updateSettings(settings)
                
                // 3. Запускаем процесс подключения (BCM сам поднимет сервис через BluetoothService)
                bluetoothConnectionManager.startConnectionProcess()

                _isInitialized.value = true

            } catch (e: Exception) {
                AppLogger.logError("Критическая ошибка инициализации: ${e.message}", TAG)
                _isInitialized.value = false
            }
        }
    }

    // ========== ПУБЛИЧНЫЙ API ==========

    fun getPairedDevices(): List<BluetoothDeviceData>? = bluetoothConnectionManager.getPairedDevices()

    fun isBluetoothEnabled(): Boolean = bluetoothConnectionManager.isBluetoothEnabled()

    fun getCurrentSettings(): AppSettings = _appSettings.value

    fun updateSettings(newSettings: AppSettings) {
        appScope.launch {
            try {
                settingsRepository.saveSettings(newSettings)
                _appSettings.value = newSettings
                bluetoothConnectionManager.updateSettings(newSettings)
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    fun disconnectFromDevice() {
        val updatedSettings = _appSettings.value.copy(selectedDevice = null)
        updateSettings(updatedSettings)
    }

    fun clearSelectedDevice() {
        val updatedSettings = _appSettings.value.copy(selectedDevice = null)
        updateSettings(updatedSettings)
    }

    fun retryConnection() {
        bluetoothConnectionManager.startConnectionProcess()
    }

    fun getCurrentCarData(): CarData = carData.value

    fun getConnectionStatistics(): String = bluetoothConnectionManager.getConnectionStatistics()

    fun resetConnectionStatistics() {
        bluetoothConnectionManager.resetStatistics()
    }

    fun sendJsonCommand(jsonCommand: String) {
        bluetoothConnectionManager.sendJsonCommand(jsonCommand)
    }

    // ========== УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ ==========

    fun cleanup() {
        bluetoothConnectionManager.cleanup()
        appScope.cancel()
        _isInitialized.value = false
    }

    fun reload() {
        cleanup()
        appScope.launch {
            delay(1000)
            initialize()
        }
    }
}
