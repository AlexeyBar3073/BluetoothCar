// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/viewmodels/SharedViewModel.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexbar3073.bluetoothcar.core.AppController
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.data.database.entities.EcuCombinationEntity
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ТЕГ: Общая ViewModel
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/viewmodels/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ViewModel для UI согласно MVVM. Является ТОЛЬКО посредником между UI и AppController.
 * НЕ содержит бизнес-логики, только передачу данных для отображения.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Получает ВСЕ данные исключительно из AppController
 * 2. Передает ВСЕ команды исключительно в AppController
 * 3. Преобразует данные для удобства UI (минимальные преобразования)
 * 4. Предоставляет потоки данных для Compose UI
 */
@OptIn(ExperimentalCoroutinesApi::class)
open class SharedViewModel(
    private val appController: AppController
) : ViewModel() {

    companion object {
        /** Тег для логирования взаимодействия UI с ViewModel */
        private const val TAG = "SharedViewModel"

        /**
         * Вспомогательная функция для логирования сообщений ViewModel.
         */
        private fun log(message: String) {
            AppLogger.logInfo("[$TAG] $message", TAG)
        }
    }

    // ========== ДАННЫЕ ИЗ APPCONTROLLER (БЕЗ ПРЕОБРАЗОВАНИЙ) ==========

    /** Состояние инициализации приложения */
    open val isInitialized: StateFlow<Boolean> = appController.isInitialized
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    /** Статус подключения */
    open val connectionStatusInfo: StateFlow<ConnectionStatusInfo> = appController.connectionStatusInfo
        .stateIn(viewModelScope, SharingStarted.Lazily, ConnectionState.UNDEFINED.toStatusInfo())

    /** Данные от БК */
    open val carData: StateFlow<CarData> = appController.carData
        .stateIn(viewModelScope, SharingStarted.Lazily, CarData())

    /** Настройки приложения */
    open val appSettings: StateFlow<AppSettings> = appController.appSettings
        .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())

    // ========== ОШИБКИ И КОМБИНАЦИИ ==========

    /**
     * Поток активных расшифрованных ошибок ЭБУ.
     */
    open val activeEcuErrors: StateFlow<List<EcuErrorEntity>> = carData
        .map { it.ecuErrors }
        .distinctUntilChanged()
        .map { errorsString ->
            errorsString.split(";")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }
        .flatMapLatest { codes ->
            if (codes.isEmpty()) flowOf(emptyList())
            else appController.getEcuErrorsByCodes(codes)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Поток активных комбинаций ошибок.
     */
    open val activeCombinations: StateFlow<List<EcuCombinationEntity>> = activeEcuErrors
        .map { errors -> errors.map { it.code }.toSet() }
        .distinctUntilChanged()
        .flatMapLatest { activeCodes ->
            if (activeCodes.isEmpty()) {
                flowOf(emptyList())
            } else {
                appController.getAllCombinations().map { allCombinations ->
                    allCombinations.filter { combination ->
                        combination.codes.isNotEmpty() && activeCodes.containsAll(combination.codes)
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ========== ПРОИЗВОДНЫЕ ПОТОКИ ДЛЯ УДОБСТВА ==========

    /** Состояние подключения как ConnectionState */
    open val connectionState: StateFlow<ConnectionState> = connectionStatusInfo
        .map { it.state }
        .stateIn(viewModelScope, SharingStarted.Lazily, ConnectionState.UNDEFINED)

    /** Выбранное Bluetooth устройство */
    open val selectedDevice: StateFlow<BluetoothDeviceData> = appSettings
        .map { it.selectedDevice }
        .stateIn(viewModelScope, SharingStarted.Lazily, BluetoothDeviceData.empty())

    /** Флаг активного соединения */
    open val isConnected: StateFlow<Boolean> = connectionStatusInfo
        .map { it.isActive }
        .stateIn(viewModelScope, SharingStarted.Lazily, false)

    // ========== МЕТОДЫ ==========

    open fun getPairedDevices(): List<BluetoothDeviceData>? = appController.getPairedDevices()
    open fun isBluetoothEnabled(): Boolean = appController.isBluetoothEnabled()
    
    open fun selectBluetoothDevice(deviceData: BluetoothDeviceData) {
        val currentSettings = getCurrentSettings()
        updateSettings(currentSettings.copy(selectedDevice = deviceData))
    }

    open fun updateSettings(settings: AppSettings) = appController.updateSettings(settings)
    open fun disconnectFromDevice() = appController.disconnectFromDevice()
    open fun clearSelectedDevice() = appController.clearSelectedDevice()
    open fun retryConnection() = appController.retryConnection()
    open fun getConnectionStatistics(): String = appController.getConnectionStatistics()
    open fun resetConnectionStatistics() = appController.resetConnectionStatistics()
    open fun sendJsonCommand(jsonCommand: String) = appController.sendJsonCommand(jsonCommand)
    open fun getCurrentCarData(): CarData = appController.getCurrentCarData()
    open fun getCurrentSettings(): AppSettings = appController.getCurrentSettings()

    // ========== МЕТОДЫ ПОЛУЧЕНИЯ ДАННЫХ ИЗ БД ==========

    /**
     * Предоставляет реактивный поток данных для конкретной ошибки по её коду.
     * Используется для отображения деталей ошибки (включая связанные).
     * 
     * @param code Код ошибки (напр. P0300)
     * @return Flow с объектом ошибки или null
     */
    fun getEcuErrorByCode(code: String): Flow<EcuErrorEntity?> {
        return appController.getEcuErrorByCode(code)
    }

    // ========== МЕТОДЫ ИМПОРТА И ЭКСПОРТА БАЗ ДАННЫХ ==========

    /**
     * Инициирует импорт базы данных ошибок из URI.
     * 
     * @param uri URI выбранного файла
     * @param onResult Коллбэк с результатом (String сообщение)
     */
    fun importEcuErrors(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = appController.importEcuErrorsFromUri(uri)
            result.onSuccess { count ->
                onResult("База ошибок успешно обновлена: $count записей")
            }.onFailure { error ->
                onResult("Ошибка импорта базы ошибок: ${error.message}")
            }
        }
    }

    /**
     * Инициирует импорт базы данных комбинаций из URI.
     * 
     * @param uri URI выбранного файла
     * @param onResult Коллбэк с результатом
     */
    fun importEcuCombinations(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = appController.importEcuCombinationsFromUri(uri)
            result.onSuccess { count ->
                onResult("База комбинаций успешно обновлена: $count записей")
            }.onFailure { error ->
                onResult("Ошибка импорта базы комбинаций: ${error.message}")
            }
        }
    }

    /**
     * Инициирует экспорт базы данных ошибок ЭБУ.
     * 
     * @param uri URI для сохранения файла
     * @param onResult Коллбэк с результатом
     */
    fun exportEcuErrors(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = appController.exportEcuErrorsToUri(uri)
            result.onSuccess {
                onResult("База ошибок успешно выгружена")
            }.onFailure { error ->
                onResult("Ошибка выгрузки базы ошибок: ${error.message}")
            }
        }
    }

    /**
     * Инициирует экспорт базы данных комбинаций.
     * 
     * @param uri URI для сохранения файла
     * @param onResult Коллбэк с результатом
     */
    fun exportEcuCombinations(uri: Uri, onResult: (String) -> Unit) {
        viewModelScope.launch {
            val result = appController.exportEcuCombinationsToUri(uri)
            result.onSuccess {
                onResult("База комбинаций успешно выгружена")
            }.onFailure { error ->
                onResult("Ошибка выгрузки базы комбинаций: ${error.message}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        log("Очистка SharedViewModel")
    }
}
