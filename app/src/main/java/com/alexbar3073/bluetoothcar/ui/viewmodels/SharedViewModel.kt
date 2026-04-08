// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/viewmodels/SharedViewModel.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexbar3073.bluetoothcar.core.AppController
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
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
    /** Одиночная ошибка или найденная комбинация (теперь используют единую сущность) */
    data class Error(val error: EcuErrorEntity) : EcuDiagnosticItem()
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
            if (codes.isEmpty()) flowOf(emptyList())
            else appController.getEcuErrorsByCodes(codes)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Поток активных комбинаций ошибок на основе текущего набора кодов.
     * Реализует экспертный алгоритм:
     * 1. Получает из базы данных все экспертные правила (записи с isCombination = true).
     * 2. Для каждой комбинации извлекает список входящих в неё кодов (разделитель "+").
     * 3. Проверяет, что ВСЕ коды комбинации присутствуют в текущем наборе кодов телеметрии.
     * 4. Возвращает список только тех комбинаций, которые полностью подтверждены текущими данными.
     */
    open val activeCombinations: StateFlow<List<EcuErrorEntity>> = combine(
        activeEcuErrorCodes,
        appController.getAllEcuCombinations()
    ) { currentCodes, allCombinations ->
        // Если кодов в телеметрии нет — комбинации не ищем
        if (currentCodes.isEmpty()) return@combine emptyList()
        
        // Создаем Set для мгновенного поиска (O(1))
        val codesSet = currentCodes.toSet()
        
        // Фильтруем экспертную базу
        allCombinations.filter { combination ->
            // Извлекаем составляющие коды (например "P0135+P0141" -> ["P0135", "P0141"])
            val combinationCodes = combination.code.split("+")
                .map { it.trim().uppercase() }
                .filter { it.isNotEmpty() }
            
            // Проверяем полное вхождение набора кодов комбинации в текущий набор автомобиля
            combinationCodes.isNotEmpty() && codesSet.containsAll(combinationCodes)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Единый поток всех диагностических элементов.
     * Объединяет одиночные расшифровки и экспертные заключения в одну ленту.
     * 
     * ПРИНЦИП РАБОТЫ:
     * 1. Берет список одиночных ошибок (например, P0135 и P0141).
     * 2. Берет список найденных комбинаций (например, P0135+P0141).
     * 3. Объединяет их. За счет разных кодов (code) они сосуществуют в одном списке.
     * 4. Выполняет сортировку: Сначала всегда идут комбинированные ошибки для привлечения внимания эксперта.
     * 5. Внутри групп (комбинации/одиночные) сортирует по приоритету (критичности).
     */
    open val allDiagnosticItems: StateFlow<List<EcuDiagnosticItem>> = combine(
        activeEcuErrors,
        activeCombinations
    ) { errors, combinations ->
        // Сливаем два списка в один. Одиночные ошибки НЕ удаляются при наличии комбинации.
        val all = (errors + combinations)
            .distinctBy { it.code } // Защита от дублей по первичному ключу
            .sortedWith(
                // Первичная сортировка по флагу комбинации (сначала true)
                compareByDescending<EcuErrorEntity> { it.isCombination }
                    // Вторичная сортировка по важности (priority)
                    .thenByDescending { it.priority }
            )
        
        // Оборачиваем в UI-модель
        all.map { EcuDiagnosticItem.Error(it) }
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

    override fun onCleared() {
        super.onCleared()
        log("Очистка SharedViewModel")
    }
}
