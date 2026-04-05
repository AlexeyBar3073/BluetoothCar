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
 * Обертка для элементов списка ошибок, позволяющая объединять одиночные ошибки и комбинации.
 */
sealed class EcuDiagnosticItem {
    data class SingleError(val error: EcuErrorEntity) : EcuDiagnosticItem()
    data class Combination(val combination: EcuCombinationEntity) : EcuDiagnosticItem()
}

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
     * Поток распарсенных кодов ошибок из данных автомобиля.
     * Выступает единым источником кодов для поиска как одиночных описаний, так и сложных комбинаций.
     */
    private val activeEcuErrorCodes: Flow<List<String>> = carData
        .map { it.ecuErrors }
        .distinctUntilChanged()
        .map { errorsString ->
            // Разбиваем строку по разделителю, очищаем от пробелов и пустых значений.
            // Принудительно переводим в верхний регистр для надежного сопоставления с БД.
            errorsString.split(";")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
        }

    /**
     * Поток активных расшифрованных одиночных ошибок ЭБУ.
     * Запрашивает детальную информацию по каждому коду из базы данных.
     */
    open val activeEcuErrors: StateFlow<List<EcuErrorEntity>> = activeEcuErrorCodes
        .flatMapLatest { codes ->
            // Если кодов нет - возвращаем пустой список, иначе запрашиваем из контроллера
            if (codes.isEmpty()) flowOf(emptyList())
            else appController.getEcuErrorsByCodes(codes)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Поток активных комбинаций ошибок.
     * Анализирует весь набор входящих кодов и ищет совпадения с экспертной базой комбинаций.
     * Реализует интеллектуальное ранжирование для больших наборов ошибок.
     */
    open val activeCombinations: StateFlow<List<EcuCombinationEntity>> = activeEcuErrorCodes
        .flatMapLatest { codes ->
            // Если список кодов пуст - комбинаций нет
            if (codes.isEmpty()) {
                flowOf(emptyList())
            } else {
                // Создаем набор уникальных кодов в верхнем регистре для быстрого поиска
                val codesSet = codes.toSet()
                
                // Запрашиваем все доступные в системе комбинации
                appController.getAllCombinations().map { allCombinations ->
                    allCombinations
                        .filter { combination ->
                            // 1. Нормализуем коды комбинации к верхнему регистру
                            val normalizedComboCodes = combination.codes.map { it.trim().uppercase() }
                            
                            // 2. ФИЛЬТРАЦИЯ: Комбинация активна, если все её коды присутствуют в текущем пакете от БК.
                            // Порядок кодов в пакете не важен, так как мы используем containsAll для множества.
                            normalizedComboCodes.isNotEmpty() && codesSet.containsAll(normalizedComboCodes)
                        }
                        .sortedWith(
                            // 3. РАНЖИРОВАНИЕ (для случаев, когда подходит несколько комбинаций):
                            compareByDescending<EcuCombinationEntity> { 
                                // Приоритет 1: Количество кодов в комбинации (чем больше кодов совпало, тем точнее диагноз)
                                it.codes.size 
                            }.thenByDescending { 
                                // Приоритет 2: Экспертный приоритет важности из базы данных
                                it.priority 
                            }
                        )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Единый поток всех диагностических сообщений (ошибки + комбинации).
     * СОГЛАСНО ТРЕБОВАНИЮ: Одиночные ошибки идут в первую очередь, комбинации — во вторую.
     */
    open val allDiagnosticItems: StateFlow<List<EcuDiagnosticItem>> = combine(
        activeEcuErrors,
        activeCombinations
    ) { errors, combinations ->
        // Создаем результирующий список
        val items = mutableListOf<EcuDiagnosticItem>()
        
        // 1. Сначала добавляем одиночные ошибки (как первичный источник информации)
        errors.forEach { items.add(EcuDiagnosticItem.SingleError(it)) }
        
        // 2. Затем добавляем найденные и отранжированные комбинации (как экспертный анализ)
        combinations.forEach { items.add(EcuDiagnosticItem.Combination(it)) }

        items
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
     */
    fun getEcuErrorByCode(code: String): Flow<EcuErrorEntity?> {
        return appController.getEcuErrorByCode(code)
    }

    /**
     * Предоставляет поток данных для конкретной комбинации по её ID.
     */
    fun getEcuCombinationById(id: Int): Flow<EcuCombinationEntity?> {
        return appController.getAllCombinations().map { list ->
            list.find { it.id == id }
        }
    }

    // ========== МЕТОДЫ ИМПОРТА И ЭКСПОРТА БАЗ ДАННЫХ ==========

    /**
     * Инициирует импорт базы данных ошибок из URI.
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
