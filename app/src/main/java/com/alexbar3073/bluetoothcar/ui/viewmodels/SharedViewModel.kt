// Файл: ui/viewmodels/SharedViewModel.kt
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
import com.alexbar3073.bluetoothcar.ui.screens.devices.dialogs.PairingState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
 * ФАЙЛ: ui/viewmodels/SharedViewModel.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/viewmodels/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * ViewModel для UI согласно архитектуре MVVM. Является посредником между UI-слоем (Compose) 
 * и бизнес-логикой (AppController). Обеспечивает реактивное обновление интерфейса.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Получает данные из AppController и транслирует их в UI через StateFlow.
 * 2. Передает команды пользователя из UI в AppController.
 * 3. Управляет состоянием UI-элементов (диалоги, ошибки, статусы).
 * 4. Преобразует сложные модели данных в удобный для отображения вид.
 * 5. Управляет жизненным циклом процесса сопряжения (pairingJob).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - MVVM: View (Compose) подписывается на StateFlow во ViewModel.
 * - Reactive: Потоки данных из AppController автоматически пробрасываются в UI.
 * - Stateless View: UI не хранит состояние, оно полностью определяется ViewModel.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * ViewModel — это "тонкий" слой. Вся бизнес-логика находится в AppController.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: AppController.kt (источник истины).
 * - Вызывается из: MainActivity.kt (через viewModelFactory), Compose Screens.
 * - Взаимодействует: BluetoothDeviceData, CarData, AppSettings.
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

    /**
     * Поток списка сопряженных устройств. Обновляется автоматически при изменении в системе.
     */
    val pairedDevices: StateFlow<List<BluetoothDeviceData>> = appController.bondStateFlow
        .onStart { emit(BluetoothDeviceData.empty()) } // Trigger первого получения
        .map { appController.getPairedDevices() ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Поток списка найденных устройств при поиске.
     * Накапливает уникальные устройства, исключая уже сопряженные.
     */
    val discoveredDevices: StateFlow<List<BluetoothDeviceData>> = appController.discoveryFlow
        .scan(emptyList<BluetoothDeviceData>()) { accumulator, newDevice ->
            // Если поиск только начался (через триггер очистки), возвращаем пустой список
            if (newDevice.address == "CLEAR") emptyList()
            else {
                val pairedAddresses = pairedDevices.value.map { it.address }.toSet()
                if (accumulator.none { it.address == newDevice.address } && !pairedAddresses.contains(newDevice.address)) {
                    accumulator + newDevice
                } else accumulator
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Флаг активного процесса поиска устройств.
     */
    val isDiscovering: StateFlow<Boolean> = appController.isDiscovering

    /**
     * Состояние процесса сопряжения для UI.
     */
    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    /**
     * Имя устройства, с которым происходит сопряжение.
     */
    private val _pairingDeviceName = MutableStateFlow("")
    val pairingDeviceName: StateFlow<String> = _pairingDeviceName.asStateFlow()

    /**
     * Job для управления жизненным циклом процесса сопряжения.
     */
    private var pairingJob: Job? = null

    /**
     * Начать поиск Bluetooth устройств в эфире.
     */
    fun startDiscovery() {
        log("Запрос запуска поиска устройств")
        // Делегируем запуск поиска контроллеру, который управляет системным сервисом
        appController.startDiscovery()
    }

    /**
     * Остановить текущий процесс поиска устройств.
     */
    fun stopDiscovery() {
        log("Запрос остановки поиска устройств")
        // Делегируем остановку контроллеру
        appController.stopDiscovery()
    }

    /**
     * Начать процесс сопряжения с выбранным устройством.
     * Реализует реактивное отслеживание статуса BOND_BONDED через Flow.
     * 
     * @param device Данные устройства для сопряжения.
     */
    fun pairDevice(device: BluetoothDeviceData) {
        log("Запрос сопряжения с: ${device.name}")
        
        // 1. Сбрасываем предыдущий процесс сопряжения, если он был активен
        pairingJob?.cancel()
        
        _pairingDeviceName.value = device.name
        _pairingState.value = PairingState.Pairing
        
        // 2. Инициируем системный процесс сопряжения через контроллер
        val success = appController.pairDevice(device.address)
        if (!success) {
            log("Система отклонила запрос на сопряжение для ${device.address}")
            _pairingState.value = PairingState.Failed("Не удалось запустить процесс сопряжения")
            return
        }

        // 3. Подписываемся на изменения состояния сопряжения в рамках отдельного Job
        pairingJob = viewModelScope.launch {
            appController.bondStateFlow
                // Фильтруем события только для целевого устройства
                .filter { it.address == device.address }
                // Работаем с потоком до тех пор, пока не достигнем финального состояния (BONDED или NONE)
                .takeWhile { 
                    it.bondState != BluetoothDeviceData.BOND_BONDED && 
                    it.bondState != BluetoothDeviceData.BOND_NONE 
                }
                .collect { updatedDevice ->
                    // Внутри collect оказываются только промежуточные состояния (например, BONDING)
                    if (updatedDevice.bondState == BluetoothDeviceData.BOND_BONDING) {
                        log("Устройство ${device.name} в процессе сопряжения (BONDING...)")
                    }
                }
            
            // 4. После завершения takeWhile (когда пришло конечное состояние BONDED или BOND_NONE)
            // Нам нужно получить финальное состояние устройства для подтверждения успеха
            val finalDevice = appController.getPairedDevices()?.find { it.address == device.address }
                ?: appController.bondStateFlow.first { it.address == device.address }

            // 5. Обновляем состояние UI в зависимости от результата
            if (finalDevice.bondState == BluetoothDeviceData.BOND_BONDED) {
                log("Устройство ${device.name} УСПЕШНО сопряжено")
                _pairingState.value = PairingState.Connected
                // После успешного сопряжения останавливаем поиск, чтобы не тратить ресурсы
                stopDiscovery()
            } else {
                log("Состояние сопряжения для ${device.name} — ОТКАЗ или ОШИБКА (BOND_NONE)")
                _pairingState.value = PairingState.Failed("Процесс сопряжения прерван или отклонен")
            }
        }
    }

    /**
     * Сбросить состояние сопряжения.
     */
    fun resetPairingState() {
        _pairingState.value = PairingState.Idle
        _pairingDeviceName.value = ""
    }

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
