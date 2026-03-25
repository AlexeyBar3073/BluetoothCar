package com.alexbar3073.bluetoothcar.core

import android.content.Context
import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothConnectionManager
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ФАЙЛ: core/AppController.kt
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ГЛАВНЫЙ КООРДИНАТОР СИСТЕМЫ. Отвечает за всю бизнес-логику приложения.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Инициализируется при запуске программы
 * 2. Отвечает за всю бизнес-логику программы
 * 3. Имеет помощников: репозиторий, настройки, обмен данными, логирование
 * 4. При инициализации получает настройки из репозитория и хранит у себя
 * 5. Связывается с UI через SharedViewModel (посредством потоков данных)
 * 6. Сохраняет настройки в репозитории при их изменении
 * 7. Инициализирует BluetoothConnectionManager после получения настроек
 * 8. Координирует работу всех модулей
 *
 * АРХИТЕКТУРНАЯ РОЛЬ В СИСТЕМЕ:
 * - Единая точка входа для бизнес-логики
 * - Координатор между модулями данных и UI
 * - Источник истины для состояния приложения
 *
 * ПОТОКОБЕЗОПАСНОСТЬ:
 * - Использует корутины (Dispatchers.Main)
 * - Все состояния защищены StateFlow/MutableStateFlow
 * - Публичный API потокобезопасен
 *
 * ЖИЗНЕННЫЙ ЦИКЛ РЕСУРСОВ:
 * - Создается при запуске приложения
 * - Уничтожается при завершении работы приложения через метод cleanup()
 * - BluetoothConnectionManager создается/уничтожается вместе с AppController
 *
 * ОБРАБОТКА ИСКЛЮЧЕНИЙ:
 * - Критические ошибки логируются через AppLogger
 * - Некритические ошибки обрабатываются с fallback значениями
 * - Инициализация защищена try-catch блоком
 */

class AppController(
    private val context: Context,
    private val settingsRepository: SettingsRepository
) {

    companion object {
        /** Тег для логирования */
        private const val TAG = "AppController"
    }

    // ========== КОРУТИНЫ И ОБЛАСТИ ВИДИМОСТИ ==========
    /** Область видимости корутин для работы AppController */
    private val appScope = CoroutineScope(Dispatchers.Main + Job())

    // ========== СОСТОЯНИЯ И ДАННЫЕ ==========
    /** Текущие настройки приложения */
    private val _appSettings = MutableStateFlow<AppSettings?>(null)

    /** Публичный поток настроек приложения */
    val appSettings: StateFlow<AppSettings?> = _appSettings.asStateFlow()

    /** Флаг инициализации AppController */
    private val _isInitialized = MutableStateFlow(false)

    /** Публичный поток флага инициализации */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    // ========== ПОМОЩНИКИ ==========
    /** Координатор Bluetooth подключения (создается после загрузки настроек) */
    private var bluetoothConnectionManager: BluetoothConnectionManager? = null

    // ========== ПРОИЗВОДНЫЕ ПОТОКИ ДАННЫХ ==========
    /** Поток данных от БК с автоматическим сбросом при отключении */
    val carData: StateFlow<CarData> by lazy {
        // Комбинируем поток данных с потоком состояния подключения и настройками
        combine(
            (bluetoothConnectionManager?.carDataFlow ?: emptyFlow()),
            _appSettings,
            connectionStatusInfo
        ) { carDataValue, settings, connectionStatus ->
            // Если не подключены или данные устарели, возвращаем пустой объект
            if (!connectionStatus.isActive) {
                CarData() // Пустой объект с нулевыми значениями
            } else {
                val baseData = carDataValue ?: CarData()
                val minFuel = settings?.minFuelLevel ?: 5f
                
                // Расчет производных полей, которые не приходят напрямую от БК
                baseData.copy(
                    isFuelLow = baseData.fuel < minFuel
                )
            }
        }.stateIn(
            scope = appScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CarData() // Пустой объект при инициализации
        )
    }

    /** Поток статуса подключения (преобразование ConnectionState → ConnectionStatusInfo).
     *  Гарантированно не null - всегда содержит актуальный статус подключения. */
    val connectionStatusInfo: StateFlow<ConnectionStatusInfo> by lazy {
        bluetoothConnectionManager
            ?.connectionStateFlow
            ?.map { connectionState ->
                val state = connectionState ?: ConnectionState.UNDEFINED
                val statusInfo = state.toStatusInfo()
                AppLogger.logInfo(
                    "Статус подключения преобразован: ${statusInfo.displayName}",
                    TAG
                )
                statusInfo
            }
            ?.stateIn(
                scope = appScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = ConnectionState.UNDEFINED.toStatusInfo()
            )
            ?: MutableStateFlow(ConnectionState.UNDEFINED.toStatusInfo()).asStateFlow()
    }

    // ========== ИНИЦИАЛИЗАЦИЯ ==========
    /** Конструктор AppController - запускает инициализацию */
    init {
        AppLogger.logInfo("Инициализация AppController - главного координатора системы", TAG)
        initialize()
    }

    /**
     * Основная инициализация AppController.
     * Выполняется асинхронно в корутине.
     * Вызывается автоматически при создании AppController.
     */
    private fun initialize() {
        appScope.launch {
            try {
                AppLogger.logInfo("Начало инициализации AppController", TAG)

                // 1. Загружаем настройки из репозитория
                AppLogger.logInfo("Загрузка настроек из SettingsRepository...", TAG)
                val settings = settingsRepository.getCurrentSettings()
                _appSettings.value = settings
                AppLogger.logInfo("Настройки загружены", TAG)

                // 2. Настраиваем AppLogger
                AppLogger.logInfo("Настройка AppLogger...", TAG)
                AppLogger.configure(
                    verbose = true,
                    timestamps = true,
                    packetNumbers = true
                )

                // 3. Создаем BluetoothConnectionManager (если не существует)
                if (bluetoothConnectionManager == null) {
                    AppLogger.logInfo(
                        "BluetoothConnectionManager не существует, создаем новый",
                        TAG
                    )
                    bluetoothConnectionManager = BluetoothConnectionManager(
                        context = context
                    )
                    AppLogger.logInfo("BluetoothConnectionManager создан успешно", TAG)
                } else {
                    AppLogger.logInfo(
                        "BluetoothConnectionManager уже существует (повторная инициализация)",
                        TAG
                    )
                }

                // 4. Передаем начальные настройки в BluetoothConnectionManager
                AppLogger.logInfo("Передача начальных настроек в BluetoothConnectionManager", TAG)
                bluetoothConnectionManager?.updateSettings(settings)

                // 5. AppController готов к работе
                _isInitialized.value = true
                AppLogger.logInfo("AppController успешно инициализирован и готов к работе", TAG)

            } catch (e: Exception) {
                AppLogger.logError(
                    "Критическая ошибка инициализации AppController: ${e.message}",
                    TAG
                )
                _isInitialized.value = false
            }
        }
    }

    // ========== ПУБЛИЧНЫЙ API ДЛЯ SHAREDVIEWMODEL ==========

    /**
     * Получить список сопряженных Bluetooth устройств.
     * Делегирует вызов BluetoothConnectionManager.
     * Вызывается из SharedViewModel при отображении списка устройств.
     */
    fun getPairedDevices(): List<BluetoothDeviceData>? {
        return bluetoothConnectionManager?.getPairedDevices()
    }

    /**
     * Проверить, включен ли Bluetooth адаптер.
     * Делегирует вызов BluetoothConnectionManager.
     * Вызывается из SharedViewModel при проверке состояния Bluetooth.
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothConnectionManager?.isBluetoothEnabled() ?: false
    }

    /**
     * Получить текущие настройки приложения.
     * Вызывается из SharedViewModel для получения настроек вне потока.
     */
    fun getCurrentSettings(): AppSettings? {
        return _appSettings.value
    }

    /**
     * Получить поток данных от БК.
     * Используется SharedViewModel для передачи данных в UI.
     * Вызывается при создании SharedViewModel.
     */
    fun getCarDataFlow(): StateFlow<CarData> {
        return carData
    }

    /**
     * Получить поток статуса подключения в виде полной структуры.
     * Гарантированно не null - всегда содержит актуальный статус.
     * Используется SharedViewModel для отображения статуса в UI.
     */
    fun getConnectionStatusInfoFlow(): StateFlow<ConnectionStatusInfo> {
        return connectionStatusInfo
    }

    /**
     * Обновить настройки приложения.
     * AppController сохраняет настройки в репозитории при их изменении.
     * Все изменения настроек проходят через этот метод.
     * Вызывается из SharedViewModel при изменении настроек пользователем.
     */
    fun updateSettings(newSettings: AppSettings) {
        appScope.launch {
            try {
                AppLogger.logInfo("Обновление настроек приложения", TAG)

                // 1. Сохраняем в репозитории
                settingsRepository.saveSettings(newSettings)

                // 2. Обновляем локальное состояние
                _appSettings.value = newSettings

                // 3. Передаем в BluetoothConnectionManager
                bluetoothConnectionManager?.updateSettings(newSettings)

                AppLogger.logInfo("Настройки обновлены и переданы в BCM", TAG)

            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
            }
        }
    }

    /**
     * Отключиться от устройства.
     * Осуществляется через обновление настроек (установка selectedDevice = null).
     * Вызывается из SharedViewModel при нажатии кнопки отключения в UI.
     */
    fun disconnectFromDevice() {
        AppLogger.logInfo("Запрос на отключение от устройства", TAG)

        val currentSettings = _appSettings.value
        if (currentSettings != null) {
            val updatedSettings = currentSettings.copy(selectedDevice = null)
            updateSettings(updatedSettings)
        } else {
            AppLogger.logError("Нельзя отключиться: настройки не загружены", TAG)
        }
    }

    /**
     * Очистить выбранное устройство.
     * Осуществляется через обновление настроек (установка selectedDevice = null).
     * Вызывается из SharedViewModel при очистке выбора устройства в UI.
     */
    fun clearSelectedDevice() {
        AppLogger.logInfo("Запрос на очистку выбранного устройства", TAG)

        val currentSettings = _appSettings.value
        if (currentSettings != null) {
            val updatedSettings = currentSettings.copy(selectedDevice = null)
            updateSettings(updatedSettings)
        } else {
            AppLogger.logError("Нельзя очистить устройство: настройки не загружены", TAG)
        }
    }

    /**
     * Ручное переподключение к устройству.
     * AppController вызывает стандартный метод startConnectionProcess() в BluetoothConnectionManager.
     * Вызывается из SharedViewModel при нажатии кнопки "Повторить подключение" в UI.
     */
    fun retryConnection() {
        AppLogger.logInfo("Запрос на ручное переподключение к устройству", TAG)
        bluetoothConnectionManager?.startConnectionProcess()
    }

    /**
     * Получить текущие данные от БК.
     * Вызывается из SharedViewModel для получения данных вне потока.
     */
    fun getCurrentCarData(): CarData {
        return carData.value
    }

    /**
     * Получить статистику подключения (делегирует BluetoothConnectionManager).
     * Вызывается из SharedViewModel для отображения статистики в UI.
     */
    fun getConnectionStatistics(): String {
        return bluetoothConnectionManager?.getConnectionStatistics() ?: "Нет данных"
    }

    /**
     * Сбросить статистику подключения (делегирует BluetoothConnectionManager).
     * Вызывается из SharedViewModel при сбросе статистики в UI.
     */
    fun resetConnectionStatistics() {
        AppLogger.logInfo("Сброс статистики подключения", TAG)
        bluetoothConnectionManager?.resetStatistics()
    }

    /**
     * Отправить произвольную JSON команду на устройство.
     * Делегирует вызов BluetoothConnectionManager.
     * Вызывается из SharedViewModel.
     */
    fun sendJsonCommand(jsonCommand: String) {
        AppLogger.logInfo("Отправка JSON команды: $jsonCommand", TAG)
        bluetoothConnectionManager?.sendJsonCommand(jsonCommand)
    }

    // ========== УПРАВЛЕНИЕ ЖИЗНЕННЫМ ЦИКЛОМ ==========

    /**
     * Очистить ресурсы AppController.
     * Вызывается при завершении работы приложения.
     * Отменяет все корутины и освобождает BluetoothConnectionManager.
     */
    fun cleanup() {
        AppLogger.logInfo("Очистка ресурсов AppController", TAG)

        // 1. Останавливаем BluetoothConnectionManager
        bluetoothConnectionManager?.cleanup()
        bluetoothConnectionManager = null

        // 2. Отменяем корутины
        appScope.cancel()

        // 3. Сбрасываем состояния
        _appSettings.value = null
        _isInitialized.value = false

        AppLogger.logInfo("AppController очищен", TAG)
    }

    /**
     * Перезагрузить AppController.
     * Полезно при критических ошибках или изменении конфигурации.
     * Вызывается при необходимости полного перезапуска системы.
     */
    fun reload() {
        AppLogger.logInfo("Перезагрузка AppController", TAG)

        // 1. Очищаем текущее состояние
        cleanup()

        // 2. Даем время на завершение операций
        appScope.launch {
            delay(1000)

            // 3. Повторно инициализируем
            initialize()

            AppLogger.logInfo("AppController перезагружен", TAG)
        }
    }
}
