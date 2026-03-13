// Файл: ui/viewmodels/BluetoothViewModel.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels
/*
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.alexbar3073.bluetoothcar.data.models.BluetoothDevice
import com.alexbar3073.bluetoothcar.data.repository.BluetoothRepository
import com.alexbar3073.bluetoothcar.data.repository.BluetoothRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ==================== МОДЕЛЬ ДАННЫХ ====================

/**
 * Состояние UI для Bluetooth экранов
 * Содержит все данные, необходимые для отображения в интерфейсе
 */
data class BluetoothUiState(
    val pairedDevices: List<BluetoothDevice> = emptyList(),
    val availableDevices: List<BluetoothDevice> = emptyList(),
    val isScanning: Boolean = false,
    val selectedDevice: BluetoothDevice? = null,
    val connectedDevice: BluetoothDevice? = null,
    val errorMessage: String? = null,
    val bluetoothStatus: String = "Проверка..."
)

// ==================== ВРЕМЕННЫЙ РЕПОЗИТОРИЙ ====================

/**
 * Пустой репозиторий для превью и тестирования
 * Все методы возвращают пустые значения или false
 */
class EmptyBluetoothRepository : BluetoothRepository {

    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _availableDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    private val _connectedDevice = MutableStateFlow<BluetoothDevice?>(null)

    // Коллбек для совместимости с новым интерфейсом
    private var onDiscoveryFinishedCallback: () -> Unit = {}

    override fun getPairedDevices(): Flow<List<BluetoothDevice>> = _pairedDevices

    override fun getAvailableDevices(): Flow<List<BluetoothDevice>> = _availableDevices

    /**
     * Пустая реализация - ничего не делает
     */
    override suspend fun startDiscovery() {
        Log.d("EmptyBluetoothRepository", "startDiscovery вызван (пустая реализация)")
        // Ничего не делаем, сразу вызываем коллбек завершения
        onDiscoveryFinishedCallback()
    }

    /**
     * Пустая реализация - ничего не делает
     */
    override suspend fun stopDiscovery() {
        Log.d("EmptyBluetoothRepository", "stopDiscovery вызван (пустая реализация)")
    }

    /**
     * Устанавливает коллбек для уведомления о завершении поиска
     */
    override fun setOnDiscoveryFinishedCallback(callback: () -> Unit) {
        this.onDiscoveryFinishedCallback = callback
        Log.d("EmptyBluetoothRepository", "Установлен коллбек завершения поиска")
    }

    /**
     * Всегда возвращает false (не поддерживается в тестовом режиме)
     */
    override suspend fun pairDevice(device: BluetoothDevice): Boolean {
        Log.d("EmptyBluetoothRepository", "pairDevice вызван (возвращает false)")
        return false
    }

    /**
     * Всегда возвращает false (не поддерживается в тестовом режиме)
     */
    override suspend fun connectDevice(device: BluetoothDevice): Boolean {
        Log.d("EmptyBluetoothRepository", "connectDevice вызван (возвращает false)")
        return false
    }

    /**
     * Пустая реализация - ничего не делает
     */
    override suspend fun disconnectDevice() {
        Log.d("EmptyBluetoothRepository", "disconnectDevice вызван (пустая реализация)")
    }

    /**
     * Всегда возвращает null (нет подключенных устройств)
     */
    override suspend fun getConnectedDevice(): BluetoothDevice? {
        return null
    }

    /**
     * Пустая реализация - ничего не делает
     */
    override suspend fun saveConnectedDevice(device: BluetoothDevice?) {
        Log.d("EmptyBluetoothRepository", "saveConnectedDevice вызван (пустая реализация)")
    }

    /**
     * Пустая реализация - ничего не делает
     */
    override suspend fun clearConnectedDevice() {
        Log.d("EmptyBluetoothRepository", "clearConnectedDevice вызван (пустая реализация)")
    }
}

// ==================== VIEWMODEL ====================

/**
 * ViewModel для управления состоянием Bluetooth в приложении
 * Работает с реальным репозиторием BluetoothRepositoryImpl
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - [Дата создания] - Создание файла с базовой логикой Bluetooth управления
 * - [Сегодня] - Исправлено приведение типа в методе getBluetoothStatus()
 */
class BluetoothViewModel(
    private val repository: BluetoothRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BluetoothUiState())
    val uiState: StateFlow<BluetoothUiState> = _uiState.asStateFlow()

    init {
        Log.d("BluetoothViewModel", "ViewModel инициализирован")

        // Устанавливаем коллбек для получения уведомлений о завершении поиска
        repository.setOnDiscoveryFinishedCallback {
            Log.d("BluetoothViewModel", "Получен коллбек завершения поиска из репозитория")
            onDiscoveryFinished()
        }

        loadPairedDevices()
        loadConnectedDevice()
        checkBluetoothStatus()
    }

    /**
     * Загружает список сопряженных устройств
     */
    private fun loadPairedDevices() {
        viewModelScope.launch {
            repository.getPairedDevices().collect { devices ->
                _uiState.update { it.copy(pairedDevices = devices) }
                Log.d("BluetoothViewModel", "Загружено ${devices.size} сопряженных устройств")
            }
        }
    }

    /**
     * Проверяет статус Bluetooth
     */
    private fun checkBluetoothStatus() {
        viewModelScope.launch {
            val status = getBluetoothStatus()
            _uiState.update { it.copy(bluetoothStatus = status) }
            Log.d("BluetoothViewModel", "Статус Bluetooth: $status")
        }
    }

    /**
     * Запускает поиск Bluetooth устройств
     */
    fun startDiscovery() {
        viewModelScope.launch {
            try {
                Log.d("BluetoothViewModel", "Запуск поиска Bluetooth устройств")

                // Устанавливаем состояние "поиск идет"
                _uiState.update { it.copy(isScanning = true, errorMessage = null) }

                // Запускаем поиск через репозиторий
                repository.startDiscovery()

                // Подписываемся на обновления списка найденных устройств
                repository.getAvailableDevices().collect { devices ->
                    _uiState.update { it.copy(availableDevices = devices) }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isScanning = false,
                        errorMessage = "Ошибка поиска: ${e.message}"
                    )
                }
                Log.e("BluetoothViewModel", "Ошибка поиска: ${e.message}")
            }
        }
    }

    /**
     * Останавливает поиск Bluetooth устройств
     */
    fun stopDiscovery() {
        viewModelScope.launch {
            Log.d("BluetoothViewModel", "Остановка поиска Bluetooth устройств")
            repository.stopDiscovery()
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    /**
     * Обрабатывает завершение поиска из репозитория
     */
    private fun onDiscoveryFinished() {
        viewModelScope.launch {
            Log.d("BluetoothViewModel", "Обработка завершения поиска")
            if (_uiState.value.isScanning) {
                _uiState.update { it.copy(isScanning = false) }
                Log.d("BluetoothViewModel", "Состояние isScanning синхронизировано с системой")
            }
        }
    }

    /**
     * Выбирает устройство
     * @param device - выбранное устройство
     */
    fun selectDevice(device: BluetoothDevice) {
        Log.d("BluetoothViewModel", "Выбрано устройство: ${device.name}")
        _uiState.update { it.copy(selectedDevice = device) }
    }

    /**
     * Пытается сопрячь выбранное устройство
     */
    fun pairSelectedDevice() {
        val device = _uiState.value.selectedDevice ?: return

        viewModelScope.launch {
            Log.d("BluetoothViewModel", "Попытка сопряжения с устройством: ${device.name}")

            val success = repository.pairDevice(device)
            if (success) {
                // Перезагружаем список сопряженных устройств
                loadPairedDevices()
                // Сохраняем как подключенное
                saveConnectedDevice(device)
                Log.d("BluetoothViewModel", "Устройство успешно сопряжено")
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = "Не удалось сопрячь устройство"
                    )
                }
                Log.e("BluetoothViewModel", "Не удалось сопрячь устройство")
            }
        }
    }

    /**
     * Сохраняет подключенное устройство
     * @param device - устройство для сохранения или null
     */
    fun saveConnectedDevice(device: BluetoothDevice?) {
        viewModelScope.launch {
            Log.d(
                "BluetoothViewModel",
                "Сохранение подключенного устройства: ${device?.name ?: "null"}"
            )
            repository.saveConnectedDevice(device)
            _uiState.update { it.copy(connectedDevice = device) }
        }
    }

    /**
     * Очищает информацию о подключенном устройстве
     */
    fun clearConnectedDevice() {
        viewModelScope.launch {
            Log.d("BluetoothViewModel", "Очистка подключенного устройства")
            repository.clearConnectedDevice()
            _uiState.update { it.copy(connectedDevice = null) }
        }
    }

    /**
     * Загружает информацию о подключенном устройстве
     */
    private fun loadConnectedDevice() {
        viewModelScope.launch {
            repository.getConnectedDevice()?.let { device ->
                _uiState.update { it.copy(connectedDevice = device) }
                Log.d("BluetoothViewModel", "Загружено подключенное устройство: ${device.name}")
            }
        }
    }

    /**
     * Очищает сообщение об ошибке
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Получает статус Bluetooth
     * @return String - текстовое описание статуса
     */
    fun getBluetoothStatus(): String {
        return if (repository is BluetoothRepositoryImpl) {
            // ИСПРАВЛЕНИЕ: Убрано ненужное приведение типа
            // Было: (repository as BluetoothRepositoryImpl).getBluetoothStatus()
            // Стало: repository.getBluetoothStatus()
            repository.getBluetoothStatus()
        } else {
            "Тестовый режим"
        }
    }

    /**
     * Обновляет статус Bluetooth
     */
    fun refreshBluetoothStatus() {
        viewModelScope.launch {
            val status = getBluetoothStatus()
            _uiState.update { it.copy(bluetoothStatus = status) }
            Log.d("BluetoothViewModel", "Обновлен статус Bluetooth: $status")
        }
    }
}

// ==================== ФАБРИКА VIEWMODEL ====================

/**
 * Фабрика для создания BluetoothViewModel
 * Обеспечивает зависимость от репозитория через конструктор
 */
class BluetoothViewModelFactory(
    private val repository: BluetoothRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return if (modelClass.isAssignableFrom(BluetoothViewModel::class.java)) {
            BluetoothViewModel(repository) as T
        } else {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
*/