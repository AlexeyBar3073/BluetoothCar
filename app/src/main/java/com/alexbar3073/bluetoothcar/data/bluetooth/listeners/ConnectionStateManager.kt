// Файл: data/bluetooth/listeners/ConnectionStateManager.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import kotlinx.coroutines.*

/**
 * ТЕГ: BLUETOOTH_CONNECTION_MANAGER_HELPER
 *
 * ФАЙЛ: data/bluetooth/listeners/ConnectionStateManager.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Управление процессом установки физического RFCOMM соединения. Реализует логику 
 * гарантированного подключения с механизмом повторных попыток и мониторингом статуса ACL.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Выполнение до 5 попыток подключения к выбранному устройству (согласно ТЗ).
 * 2. Управление таймаутами подключения и задержками между итерациями.
 * 3. Мониторинг состояния активного соединения через системные callback-и.
 * 4. Уведомление менеджера о переходах состояний (CONNECTING, CONNECTED, DISCONNECTED).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Использование Coroutines для неблокирующего ожидания установки сокета.
 * - Инкапсуляция логики ретраев: скрытие сложности повторных попыток от оркестратора.
 * - Реактивное уведомление о разрыве связи через механизмы обратного вызова.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Обеспечение максимальной вероятности успешного подключения за счет серии попыток 
 * и непрерывный контроль физического наличия связи после установления сокета.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Вызывается из: BluetoothConnectionManager.kt (Шаг 3 процесса подключения)
 * - Использует: AppBluetoothService.kt (Низкоуровневые сокет-операции)
 * - Взаимодействует: AppLogger.kt (Диагностика)
 */
class ConnectionStateManager(
    private val bluetoothService: AppBluetoothService,
    private val stateChangeCallback: (ConnectionState, String?) -> Unit,
    /** Диспетчер для выполнения операций ввода-вывода (IO) */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /** Тег для логирования компонента */
    private val TAG = "ConnectionStateManager"

    // ========== КОНФИГУРАЦИЯ ПОДКЛЮЧЕНИЯ ==========

    /** Максимальное количество попыток подключения согласно ТЗ */
    private val MAX_CONNECTION_ATTEMPTS = 5

    /** Таймаут одной попытки подключения в миллисекундах */
    private val CONNECTION_TIMEOUT_MS = 10000L

    /** Задержка между попытками подключения */
    private val RETRY_DELAY_MS = 2000L

    // ========== СОСТОЯНИЕ ==========

    /** Флаг активности попыток подключения */
    private var isAttemptingConnection = false

    /** Флаг активности мониторинга соединения */
    private var isMonitoringActive = false

    /** Корутин скоуп для управления подключением */
    private var connectionScope: CoroutineScope? = null

    /** Текущее устройство для подключения/мониторинга */
    private var currentDeviceData: BluetoothDeviceData? = null

    /** Job для попыток подключения */
    private var connectionAttemptJob: Job? = null

    // ========== CALLBACK-ФУНКЦИИ ДЛЯ BLUETOOTH SERVICE ==========

    /**
     * Callback-функция для обработки успешного подключения.
     * Передается в AppBluetoothService.connectToDevice()
     * Вызывается AppBluetoothService при успешном подключении к устройству.
     *
     * @param deviceData Подключенное устройство
     */
    private val onConnected: (BluetoothDeviceData) -> Unit = { deviceData ->
        log("✓ Успешное подключение к устройству: ${deviceData.name}")

        // Уведомляем о успешном подключении
        stateChangeCallback(ConnectionState.CONNECTED, null)

        // Автоматически запускаем мониторинг соединения
        startConnectionMonitoring(deviceData)
    }

    /**
     * Callback-функция для обработки отключения.
     * Передается в AppBluetoothService.connectToDevice()
     * Вызывается AppBluetoothService при отключении от устройства.
     *
     * @param deviceData Отключенное устройство
     */
    private val onDisconnected: (BluetoothDeviceData) -> Unit = { deviceData ->
        log("✗ Отключение от устройства: ${deviceData.name}")

        // Останавливаем мониторинг
        stopConnectionMonitoring()

        // Уведомляем об отключении
        stateChangeCallback(ConnectionState.DISCONNECTED, null)
    }

    /**
     * Callback-функция для мониторинга состояния соединения.
     * Передается в AppBluetoothService.startConnectionMonitoring()
     * Вызывается AppBluetoothService при изменении состояния соединения.
     *
     * @param isConnected Состояние соединения
     * @param eventDeviceData Устройство, к которому относится событие
     */
    private val connectionMonitoringCallback: (Boolean, BluetoothDeviceData?) -> Unit = { isConnected, eventDeviceData ->
        val currentDeviceAddress = currentDeviceData?.address

        // Обрабатываем только если событие для нашего устройства
        if (currentDeviceAddress == null || eventDeviceData?.address == currentDeviceAddress) {
            if (isConnected) {
                log("Системное событие: устройство подключено")
            } else {
                log("Системное событие: устройство отключено")

                // Уведомляем об отключении
                stateChangeCallback(ConnectionState.DISCONNECTED, null)

                // Останавливаем мониторинг
                stopConnectionMonitoring()
            }
        }
    }

    /**
     * Попытаться подключиться к устройству.
     * Выполняет до 5 попыток подключения согласно ТЗ.
     * Вызывается: BluetoothConnectionManager.kt после успешного поиска устройства.
     *
     * @param deviceData Устройство для подключения
     */
    fun start(deviceData: BluetoothDeviceData) {
        if (isAttemptingConnection) {
            stateChangeCallback(ConnectionState.ERROR, "Попытка подключения уже выполняется")
            return
        }

        isAttemptingConnection = true
        currentDeviceData = deviceData

        log("Начало попыток подключения к: ${deviceData.name} (${deviceData.address})")

        // СОГЛАСНО ТЗ: уведомление о начале подключения (CONNECTING)
        stateChangeCallback(ConnectionState.CONNECTING, null)

        connectionScope = CoroutineScope(ioDispatcher + Job())

        // Запускаем попытки подключения в отдельной корутине
        connectionAttemptJob = connectionScope?.launch {
            try {
                performConnectionAttempts(deviceData)
            } catch (e: CancellationException) {
                log("Попытки подключения отменены: ${e.message}")
                // Отмена - нормальное завершение, не уведомляем callback
            } catch (e: Exception) {
                log("Неожиданная ошибка в процессе подключения: ${e.message}")
                stateChangeCallback(ConnectionState.ERROR, "Неожиданная ошибка: ${e.message}")
            } finally {
                cleanupConnectionAttempt()
            }
        }
    }

    /**
     * Выполнить 5 попыток подключения с таймаутами.
     * Внутренний метод, вызывается из корутины.
     * Вызывается: из метода start().
     *
     * @param deviceData Устройство для подключения
     */
    private suspend fun performConnectionAttempts(deviceData: BluetoothDeviceData) {
        var attempts = 0
        var lastError: String? = null

        while (attempts < MAX_CONNECTION_ATTEMPTS && isAttemptingConnection) {
            attempts++
            log("Попытка подключения $attempts из $MAX_CONNECTION_ATTEMPTS")

            // Проверяем, не отменили ли подключение
            ensureActive()

            // Используем AppBluetoothService для подключения с callback-функциями
            val (success, errorMessage) = tryConnectToDevice(deviceData)

            if (success) {
                log("Успешное подключение на попытке $attempts")
                return
            }

            lastError = errorMessage
            log("Не удалось подключиться в попытке $attempts: $errorMessage")

            // Ждем перед следующей попыткой (кроме последней)
            if (attempts < MAX_CONNECTION_ATTEMPTS) {
                try {
                    delay(RETRY_DELAY_MS)
                } catch (e: CancellationException) {
                    log("Задержка между попытками отменена")
                    throw e
                }
            }
        }

        // Если дошли сюда - все 5 попыток неудачны
        log("$MAX_CONNECTION_ATTEMPTS попыток подключения неудачны")
        stateChangeCallback(ConnectionState.ERROR, "Не удалось подключиться к устройству ${deviceData.name} после $MAX_CONNECTION_ATTEMPTS попыток. Последняя ошибка: $lastError")
    }

    /**
     * Одна попытка подключения к устройству.
     * Внутренний метод с обработкой исключений.
     * Вызывается: из performConnectionAttempts().
     *
     * @param deviceData Устройство для подключения
     * @return Pair<Успешно, Сообщение об ошибке>
     */
    private suspend fun tryConnectToDevice(deviceData: BluetoothDeviceData): Pair<Boolean, String?> {
        return try {
            log("Попытка подключения к устройству: ${deviceData.name}")

            // Используем новый метод с callback-функциями
            val (success, errorMessage) = bluetoothService.connectToDevice(
                deviceData = deviceData,
                onConnected = onConnected,
                onDisconnected = onDisconnected,
                timeoutMs = CONNECTION_TIMEOUT_MS
            )

            if (success) {
                log("Подключение успешно установлено")
                Pair(true, null)
            } else {
                log("Ошибка подключения: $errorMessage")
                Pair(false, errorMessage)
            }
        } catch (e: TimeoutCancellationException) {
            log("Таймаут подключения к устройству: ${e.message}")
            Pair(false, "Таймаут подключения: ${CONNECTION_TIMEOUT_MS}ms")
        } catch (e: CancellationException) {
            log("Подключение отменено: ${e.message}")
            throw e  // Пробрасываем выше для обработки отмены
        } catch (e: Exception) {
            log("Исключение при подключении: ${e.message}")
            Pair(false, "Исключение: ${e.message}")
        }
    }

    /**
     * Проверить, что подключение еще активно.
     * Внутренний метод для проверки отмены.
     * Вызывается: из performConnectionAttempts().
     */
    private fun ensureActive() {
        if (!isAttemptingConnection) {
            throw CancellationException("Попытка подключения отменена")
        }
    }

    /**
     * Начать пассивный мониторинг соединения.
     * Вызывается автоматически после успешного подключения для отслеживания разрывов.
     *
     * @param deviceData Устройство для мониторинга
     */
    private fun startConnectionMonitoring(deviceData: BluetoothDeviceData) {
        if (isMonitoringActive) {
            log("Мониторинг уже выполняется")
            return
        }

        currentDeviceData = deviceData
        isMonitoringActive = true

        log("Начало пассивного мониторинга соединения: ${deviceData.name}")

        // Подписываемся на системные события через AppBluetoothService с callback-функцией
        bluetoothService.startConnectionMonitoring(deviceData, connectionMonitoringCallback)

        log("Подписка на системные события активирована")
    }

    /**
     * Остановить мониторинг соединения.
     * Вызывается BluetoothConnectionManager или при разрыве соединения.
     */
    fun stopConnectionMonitoring() {
        if (!isMonitoringActive) {
            return
        }

        log("Остановка мониторинга соединения")

        // Отписываемся от системных событий
        bluetoothService.stopConnectionMonitoring()

        isMonitoringActive = false
        log("Мониторинг соединения остановлен")
    }

    /**
     * Остановить попытки подключения.
     * Вызывается BluetoothConnectionManager при необходимости прервать подключение.
     * Вызывается: BluetoothConnectionManager.kt.
     */
    fun stopConnectionAttempt() {
        if (!isAttemptingConnection) {
            return
        }

        log("Остановка попыток подключения")
        isAttemptingConnection = false

        connectionAttemptJob?.cancel("Ручная отмена подключения")
        connectionAttemptJob = null

        cleanupConnectionAttempt()
        log("Попытки подключения остановлены")
    }

    /**
     * Отключиться от устройства.
     * Вызывается BluetoothConnectionManager для явного отключения.
     * Вызывается: BluetoothConnectionManager.kt.
     */
    fun disconnect() {
        log("Отключение от устройства")
        bluetoothService.disconnect()
    }

    /**
     * Очистить ресурсы после попыток подключения.
     * Внутренний метод для освобождения ресурсов.
     * Вызывается: из performConnectionAttempts() или stopConnectionAttempt().
     */
    private fun cleanupConnectionAttempt() {
        isAttemptingConnection = false
        connectionScope?.cancel()
        connectionScope = null
        connectionAttemptJob = null
    }

    /**
     * Остановить все процессы ConnectionStateManager.
     * Вызывается при остановке или перезапуске BluetoothConnectionManager.
     * Вызывается: BluetoothConnectionManager.kt.
     */
    fun stop() {
        log("Остановка всех процессов ConnectionStateManager")

        stopConnectionAttempt()
        stopConnectionMonitoring()
        log("Отключение от устройства")
        bluetoothService.disconnect()

        currentDeviceData = null

        log("Все процессы ConnectionStateManager остановлены")
    }

    // ========== МЕТОД ЛОГИРОВАНИЯ ==========

    /**
     * Записать информационное сообщение в лог.
     * Вызывается: из всех методов класса для логирования действий.
     *
     * @param message Текст сообщения
     */
    private fun log(message: String) {
        AppLogger.logInfo("[$TAG] $message", TAG)
    }
}
