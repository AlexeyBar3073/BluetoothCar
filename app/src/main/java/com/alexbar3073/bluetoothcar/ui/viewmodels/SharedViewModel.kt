// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/viewmodels/SharedViewModel.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.alexbar3073.bluetoothcar.core.AppController
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

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
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Все данные получает из AppController
 * - Все команды передает в AppController
 * - Не имеет собственной бизнес-логики
 * - ТОЛЬКО методы, которые есть в AppController
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Получает данные от: AppController.kt
 * 2. Предоставляет данные для: HomeScreen.kt, SettingsScreen.kt, PermissionsScreen.kt
 * 3. Использует: ConnectionStatusInfo из data/bluetooth/
 */
open class SharedViewModel(
    private val appController: AppController
) : ViewModel() {

    companion object {
        /** Тег для логирования взаимодействия UI с ViewModel */
        private const val TAG = "SharedViewModel"

        /**
         * Вспомогательная функция для логирования сообщений ViewModel.
         * Используется для отслеживания взаимодействий UI с ViewModel.
         */
        private fun log(message: String) {
            AppLogger.logInfo("[$TAG] $message", TAG)
        }
    }

    // ========== ДАННЫЕ ИЗ APPCONTROLLER (БЕЗ ПРЕОБРАЗОВАНИЙ) ==========

    /**
     * Состояние инициализации приложения.
     * Позволяет UI знать, когда AppController готов предоставлять данные (Bluetooth и т.д.).
     */
    open val isInitialized: StateFlow<Boolean> = appController.isInitialized
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    /**
     * Полная структура статуса подключения, полученная из AppController.
     * Используется UI для отображения всей информации о состоянии подключения.
     * Гарантированно не null - AppController всегда возвращает актуальный статус.
     */
    open val connectionStatusInfo: StateFlow<ConnectionStatusInfo> = appController.connectionStatusInfo
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = ConnectionState.UNDEFINED.toStatusInfo()
        )

    /**
     * Данные от БК, полученные из AppController.
     * Содержат текущие параметры автомобиля (скорость, топливо, температуры).
     * Гарантированно не null — AppController возвращает CarData() при отсутствии связи.
     */
    open val carData: StateFlow<CarData> = appController.carData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = CarData() 
        )

    /**
     * Настройки приложения, полученные из AppController.
     * Используется UI для отображения и изменения конфигурации.
     * Гарантированно не null — AppController возвращает объект настроек (AppSettings) по умолчанию.
     */
    open val appSettings: StateFlow<AppSettings> = appController.appSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = AppSettings()
        )

    // ========== ПРОИЗВОДНЫЕ ПОТОКИ ДЛЯ УДОБСТВА (МИНИМАЛЬНЫЕ) ==========

    /**
     * Состояние подключения как ConnectionState enum.
     * Используется для логики отображения элементов в UI и обратной совместимости.
     */
    open val connectionState: StateFlow<ConnectionState> = connectionStatusInfo
        .map { it.state }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = ConnectionState.UNDEFINED
        )

    /** 
     * Выбранное Bluetooth устройство из настроек.
     * Теперь возвращает не-nullable тип BluetoothDeviceData.
     */
    open val selectedDevice: StateFlow<BluetoothDeviceData> = appSettings
        .map { it.selectedDevice }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = BluetoothDeviceData.empty()
        )

    /**
     * Флаг подключения, вычисленный из статуса.
     * Используется UI для быстрой индикации активного соединения (например, для свечения иконок).
     */
    open val isConnected: StateFlow<Boolean> = connectionStatusInfo
        .map { it.isActive }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = false
        )

    // ========== МЕТОДЫ (ТОЧНО КАК В APPCONTROLLER) ==========

    /**
     * Получить список сопряженных Bluetooth устройств.
     * Делегирует вызов AppController.
     * Используется в: SettingsScreen для отображения списка устройств.
     */
    open fun getPairedDevices(): List<BluetoothDeviceData>? = appController.getPairedDevices()

    /**
     * Проверить, включен ли Bluetooth адаптер.
     * Делегирует вызов AppController.
     * Используется в: PermissionsScreen, SettingsScreen для отображения состояния Bluetooth.
     */
    open fun isBluetoothEnabled(): Boolean = appController.isBluetoothEnabled()

    /**
     * Выбрать Bluetooth устройство для подключения.
     * Обновляет настройки приложения с новым устройством.
     * Вызывается из: DevicesScreen / SettingsScreen при выборе устройства.
     */
    open fun selectBluetoothDevice(deviceData: BluetoothDeviceData) {
        log("UI: Выбор устройства ${deviceData.name}")
        val currentSettings = getCurrentSettings()
        val updatedSettings = currentSettings.copy(selectedDevice = deviceData)
        updateSettings(updatedSettings)
    }

    /**
     * Обновить настройки приложения.
     * Делегирует вызов AppController для сохранения и уведомления менеджера связи.
     * Вызывается из: SettingsScreen при изменении любых настроек.
     */
    open fun updateSettings(settings: AppSettings) {
        log("UI: Обновление настроек")
        appController.updateSettings(settings)
    }

    /**
     * Отключиться от текущего устройства.
     * Делегирует вызов AppController.
     * Вызывается из: UI при нажатии кнопки отключения.
     */
    open fun disconnectFromDevice() {
        log("UI: Отключение от устройства")
        appController.disconnectFromDevice()
    }

    /**
     * Очистить выбранное устройство (забыть устройство).
     * Делегирует вызов AppController.
     * Вызывается из: SettingsScreen при очистке выбора устройства.
     */
    open fun clearSelectedDevice() {
        log("UI: Очистка выбранного устройства")
        appController.clearSelectedDevice()
    }

    /**
     * Ручное инициирование цикла переподключения.
     * Делегирует вызов AppController.
     * Вызывается из: UI при нажатии кнопки "Повторить подключение".
     */
    open fun retryConnection() {
        log("UI: Ручное переподключение")
        appController.retryConnection()
    }

    /**
     * Получить текстовую статистику работы BT-менеджера.
     * Делегирует вызов AppController.
     * Используется в: Диалогах диагностики и информации.
     */
    open fun getConnectionStatistics(): String = appController.getConnectionStatistics()

    /**
     * Сбросить накопленную статистику ошибок и переданных данных.
     * Делегирует вызов AppController.
     * Используется в: Настройках для очистки статистики.
     */
    open fun resetConnectionStatistics() {
        log("UI: Сброс статистики подключения")
        appController.resetConnectionStatistics()
    }

    /**
     * Отправить произвольную JSON команду на устройство.
     * Делегирует вызов AppController.
     * Вызывается из: Тестовых кнопок или расширенных панелей управления.
     */
    open fun sendJsonCommand(jsonCommand: String) {
        log("UI: Отправка JSON команды: $jsonCommand")
        appController.sendJsonCommand(jsonCommand)
    }

    /**
     * Получить текущий снимок данных от БК.
     * Делегирует вызов AppController.
     * Используется в: Логике, не поддерживающей реактивные потоки.
     */
    open fun getCurrentCarData(): CarData = appController.getCurrentCarData()

    /**
     * Получить текущий снимок настроек приложения.
     * Делегирует вызов AppController. Гарантированно не null.
     * Используется в: UI перед обновлением полей или в диалогах.
     */
    open fun getCurrentSettings(): AppSettings = appController.getCurrentSettings()

    /**
     * Очистка ресурсов ViewModel при завершении её жизненного цикла.
     */
    override fun onCleared() {
        super.onCleared()
        log("Очистка SharedViewModel")
    }
}
