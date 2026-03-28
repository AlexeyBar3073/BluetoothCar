// Файл: data/bluetooth/listeners/ConnectionFeasibilityChecker.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import kotlinx.coroutines.*
import java.io.Closeable

/**
 * ФАЙЛ: data/bluetooth/listeners/ConnectionFeasibilityChecker.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ПРОВЕРЩИК ВОЗМОЖНОСТИ ПОДКЛЮЧЕНИЯ К BLUETOOTH УСТРОЙСТВУ. Выполняет последовательные
 * проверки согласно принципу "выход при первой ошибке". Мониторит состояние Bluetooth
 * и уведомляет о включении/отключении адаптера.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Проверка доступности Bluetooth адаптера на устройстве
 * 2. Проверка что целевое устройство выбрано (не null)
 * 3. Проверка валидности адреса устройства
 * 4. Проверка что устройство сопряжено с хост-устройством
 * 5. Проверка что Bluetooth адаптер включен
 * 6. Мониторинг включения и отключения Bluetooth
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Работает ТОЛЬКО с доменными моделями (BluetoothDeviceData)
 * - Все проверки выполняются асинхронно в корутинах
 * - Результаты возвращаются через единую функцию обратного вызова
 * - Принцип "ВЫХОД ПРИ ПЕРВОЙ ОШИБКЕ"
 * - Мониторинг Bluetooth запускается всегда при валидном устройстве
 * - При включении Bluetooth вызывает DEVICE_SELECTED
 * - При отключении Bluetooth вызывает DISCONNECTED
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: AppBluetoothService.kt для доступа к Bluetooth API и мониторинга
 * 2. Уведомляет: BluetoothConnectionManager.kt через единую функцию обратного вызова
 * 3. Взаимодействует: AppLogger.kt для логирования
 */

class ConnectionFeasibilityChecker(
    private val bluetoothService: AppBluetoothService,
    private val stateChangeCallback: (ConnectionState, String?) -> Unit
) {
    private val TAG = "ConnectionFeasibilityChecker"

    /** Закрываемый объект для отмены подписки на мониторинг Bluetooth */
    private var bluetoothMonitoringDisposable: Closeable? = null

    /**
     * Начать проверку возможности подключения к устройству.
     * Выполняет последовательные проверки согласно принципу "выход при первой ошибке".
     *
     * @param deviceData BluetoothDeviceData для проверки (может быть null)
     */
    fun start(deviceData: BluetoothDeviceData?) {
        log("Начало проверки возможности подключения")

        val scope = CoroutineScope(Dispatchers.IO + Job())

        scope.launch {
            try {
                performChecks(deviceData)
            } catch (e: Exception) {
                log("Ошибка проверки: ${e.message}")
                stateChangeCallback(ConnectionState.ERROR, "Ошибка проверки: ${e.message}")
            } finally {
                scope.cancel()
            }
        }
    }

    /**
     * Выполнить все проверки возможности подключения.
     * Последовательная проверка с принципом "выход при первой ошибке".
     * Мониторинг Bluetooth запускается всегда при валидном устройстве.
     *
     * @param deviceData Устройство для проверки
     */
    private suspend fun performChecks(deviceData: BluetoothDeviceData?) {
        // ПРОВЕРКА 1: Устройство указано
        log("Проверка 1: Наличие и валидность целевого устройства")
        if (deviceData == null||deviceData.address.isBlank()) {
            log("Устройство не указано (null)")
            stateChangeCallback(ConnectionState.NO_DEVICE_SELECTED, "Устройство не выбрано")
            return // Выход при первой ошибке
        }
        log("Проверка 1 пройдена: Устройство указано: ${deviceData.name} (${deviceData.address})")

        // ПРОВЕРКА 2: Bluetooth адаптер доступен
        log("Проверка 2: Доступность адаптера Bluetooth")
        if (!bluetoothService.bluetoothAdapterIsAvailable()) {
            log("Bluetooth адаптер недоступен")
            stateChangeCallback(ConnectionState.ERROR, "Адаптер Bluetooth недоступен")
            return // Выход при первой ошибке
        }
        log("Проверка 2 пройдена: Bluetooth адаптер доступен")

        // ЗАПУСКАЕМ МОНИТОРИНГ BLUETOOTH В ЛЮБОМ СЛУЧАЕ (если адаптер есть и устройство валидно)
        // Запускаем мониторинг включения/отключения Bluetooth
        startBluetoothMonitoring()

        // ПРОВЕРКА 3: Bluetooth включен
        log("Проверка 3: Проверка активности адаптера Bluetooth")
        if (!bluetoothService.bluetoothAdapterIsEnabled()) {
            log("Bluetooth выключен")
            stateChangeCallback(ConnectionState.BLUETOOTH_DISABLED, "Bluetooth выключен")
            return // Выход при первой ошибке, но мониторинг уже запущен!
        }
        log("Проверка 3 пройдена: Адаптер Bluetooth включен")

        // ПРОВЕРКА 4: Устройство сопряжено
        log("Проверка 4: Проверка сопряжения целевого устройства")
        if (!isDevicePaired(deviceData)) {
            log("Устройство не сопряжено: ${deviceData.address}")
            stateChangeCallback(ConnectionState.NO_DEVICE_SELECTED, "Устройство не сопряжено")
            return
        }
        log("Проверка 4 пройдена: Устройство ${deviceData.address} сопряжено")

        // Все проверки пройдены успешно
        log("Все проверки пройдены успешно, оповещаем о доступности устройства ${deviceData.name} (${deviceData.address})")
        stateChangeCallback(ConnectionState.DEVICE_SELECTED, null)
    }

    /**
     * Начать мониторинг состояния Bluetooth (включение/выключение).
     * Вызывается всегда при валидном устройстве и наличии адаптера.
     * При включении Bluetooth вызывает DEVICE_SELECTED.
     * При отключении Bluetooth вызывает DISCONNECTED.
     */
    private fun startBluetoothMonitoring() {
        // Сначала останавливаем предыдущий мониторинг (если был)
        stopBluetoothMonitoring()

        log("Запуск мониторинга состояния Bluetooth (включение и отключение)")

        // Подписываемся на события изменения состояния Bluetooth
        bluetoothMonitoringDisposable = bluetoothService.monitorBluetoothState { isEnabled ->
            log("Мониторинг: Состояние Bluetooth изменилось: $isEnabled")

            try {
                if (isEnabled) {
                    log("Мониторинг: Bluetooth включен, вызываем DEVICE_SELECTED")
                    // Bluetooth включился - проверяем заново
                    stateChangeCallback(ConnectionState.DEVICE_SELECTED, null)
                    // Мониторинг продолжает работать для отслеживания отключения
                } else {
                    log("Мониторинг: Bluetooth отключен, вызываем DISCONNECTED")
                    // Bluetooth отключился - уведомляем о разрыве соединения
                    stateChangeCallback(ConnectionState.DISCONNECTED, "Bluetooth отключен")
                    // Мониторинг продолжает работать для отслеживания повторного включения
                }
            } catch (e: Exception) {
                log("Ошибка при вызове stateChangeCallback: ${e.message}")
            }
        }

        log("Мониторинг Bluetooth активирован")
    }

    /**
     * Остановить мониторинг состояния Bluetooth.
     * Вызывается при остановке проверки или перезапуске мониторинга.
     */
    private fun stopBluetoothMonitoring() {
        if (bluetoothMonitoringDisposable == null) {
            return
        }

        try {
            // Закрываем disposable для отмены подписки
            bluetoothMonitoringDisposable?.close()
            bluetoothMonitoringDisposable = null

            log("Мониторинг Bluetooth остановлен")
        } catch (e: Exception) {
            log("Ошибка остановки мониторинга Bluetooth: ${e.message}")
            bluetoothMonitoringDisposable = null
        }
    }

    /**
     * Проверить, сопряжено ли устройство с хост-устройством.
     * Использует AppBluetoothService для проверки сопряжения.
     *
     * @param deviceData Доменная модель устройства для проверки
     * @return true if device is paired
     */
    private fun isDevicePaired(deviceData: BluetoothDeviceData): Boolean {
        return try {
            bluetoothService.isDevicePaired(deviceData.address)
        } catch (e: Exception) {
            log("Ошибка проверки сопряжения: ${e.message}")
            false
        }
    }

    /**
     * Остановить все процессы проверщика.
     * Вызывается BluetoothConnectionManager при остановке или перезапуске.
     */
    fun stop() {
        log("Остановка ConnectionFeasibilityChecker")

        // Останавливаем мониторинг Bluetooth
        stopBluetoothMonitoring()

        log("ConnectionFeasibilityChecker остановлен")
    }

    // ========== МЕТОДЫ ЛОГИРОВАНИЯ ==========

    /**
     * Записать информационное сообщение в лог.
     *
     * @param message Текст сообщения
     */
    private fun log(message: String) {
        AppLogger.logInfo("[$TAG] $message", TAG)
    }
}
