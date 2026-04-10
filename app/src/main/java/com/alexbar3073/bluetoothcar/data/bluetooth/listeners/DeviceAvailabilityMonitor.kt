// Файл: data/bluetooth/listeners/DeviceAvailabilityMonitor.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ФАЙЛ: data/bluetooth/listeners/DeviceAvailabilityMonitor.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * МОНИТОР ФИЗИЧЕСКОЙ ДОСТУПНОСТИ BLUETOOTH УСТРОЙСТВА. Выполняет поиск устройства
 * в Bluetooth эфире для проверки его физической доступности согласно ТЗ.
 *
 * ОТВЕТСТВЕННОСТЬ (СОГЛАСНО ТЗ):
 * 1. Поиск целевого устройства в окружении хост-устройства
 * 2. Выполнение до 3 попыток поиска с интервалом 1 секунда
 * 3. Уведомление о начале поиска
 * 4. Возврат результата: DEVICE_AVAILABLE, DEVICE_UNAVAILABLE или ERROR
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Работает ТОЛЬКО с доменными моделями (BluetoothDeviceData)
 * - Все операции выполняются асинхронно в корутинах
 * - Результаты возвращаются через единую функцию обратного вызова
 * - Не устанавливает статусы ConnectionState, только вызывает callback с соответствующим состоянием
 * - Предполагает что Bluetooth включен (гарантируется ConnectionFeasibilityChecker)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * - Монитор создает callback-функции и передает их в AppBluetoothService
 * - AppBluetoothService только инструмент, не содержит бизнес-логики
 * - Сравнение адресов и управление попытками выполняется в мониторе
 * - Реактивная архитектура без блокировок
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: AppBluetoothService.kt для поиска устройств в эфире
 * 2. Уведомляет: BluetoothConnectionManager.kt через единую функцию обратного вызова
 * 3. Взаимодействует: AppLogger.kt для логирования
 */

class DeviceAvailabilityMonitor(
    private val bluetoothService: AppBluetoothService,
    private val stateChangeCallback: (ConnectionState, String?) -> Unit,
    /** Диспетчер для выполнения операций ввода-вывода (IO) */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val TAG = "DeviceAvailabilityMonitor"

    // ========== КОНФИГУРАЦИЯ ПОИСКА ==========

    /** Максимальное количество попыток поиска согласно новым требованиям */
    private val MAX_SEARCH_ATTEMPTS = 3

    /** Интервал между попытками поиска */
    private val RETRY_INTERVAL_MS = 1000L

    // ========== СОСТОЯНИЕ ==========

    /** Флаг активности поиска */
    private var isActive = false

    /** MAC-адрес целевого устройства для поиска */
    private var targetAddress: String? = null

    /** Текущая попытка поиска */
    private var currentAttempt = 0

    /** Корутин скоуп для управления поиском */
    private var searchScope: CoroutineScope? = null

    /** Job для прослушивания потока обнаруженных устройств */
    private var discoveryJob: Job? = null

    /** Job для отслеживания состояния поиска (запущен/остановлен) */
    private var stateMonitoringJob: Job? = null

    /**
     * Начать поиск устройства в Bluetooth эфире.
     * Выполняет до 3 попыток поиска с интервалом 1 секунда согласно новым требованиям.
     * Вызывается из: BluetoothConnectionManager.kt
     *
     * @param targetDeviceData Устройство для поиска (должно быть сопряжено)
     */
    fun start(targetDeviceData: BluetoothDeviceData) {
        if (isActive) {
            // Хирургическая правка: безопасный перезапуск ресурсов для исключения утечек при повторных вызовах
            log("Предупреждение: Повторный запуск монитора. Сброс ресурсов.")
            stop()
        }

        targetAddress = targetDeviceData.address
        currentAttempt = 0
        isActive = true

        log("Начало поиска устройства: ${targetDeviceData.name} (${targetDeviceData.address})")

        // СОГЛАСНО ТЗ: уведомление о начале поиска
        stateChangeCallback(ConnectionState.SEARCHING_DEVICE, null)

        // Хирургическая правка: SupervisorJob обеспечивает устойчивость цикла поиска к локальным ошибкам
        searchScope = CoroutineScope(ioDispatcher + SupervisorJob())

        // Запускаем первую попытку поиска
        startDiscovery()
    }

    /**
     * Запустить системный поиск устройств через AppBluetoothService.
     * Внутренний метод, подписывается на discoveryFlow из AppBluetoothService.
     * Вызывается при старте и при перезапуске попыток.
     */
    private fun startDiscovery() {
        log("Запуск системного поиска (попытка ${currentAttempt + 1}/$MAX_SEARCH_ATTEMPTS)")

        // Отменяем предыдущие подписки
        discoveryJob?.cancel()
        stateMonitoringJob?.cancel()

        // 1. Подписываемся на поток обнаруженных устройств
        discoveryJob = searchScope?.launch {
            bluetoothService.discoveryFlow.collect { device ->
                if (isActive && device.address == targetAddress) {
                    log("✓ Найдено целевое устройство: ${device.name} (${device.address})")
                    bluetoothService.stopDiscovery()
                    stateChangeCallback(ConnectionState.DEVICE_AVAILABLE, null)
                    stop()
                }
            }
        }

        // 2. Подписываемся на состояние поиска для управления ретраями
        stateMonitoringJob = searchScope?.launch {
            bluetoothService.isDiscovering
                .drop(1) // Пропускаем начальное состояние StateFlow
                .collect { isNowDiscovering ->
                    // Если поиск перешел в состояние false — значит итерация завершена
                    if (!isNowDiscovering) {
                        log("Система сообщила о завершении сканирования")
                        handleIterationFinished()
                    }
                }
        }

        // 3. Запускаем сам поиск в сервисе
        val started = bluetoothService.startDiscovery()
        if (!started) {
            log("Ошибка: Не удалось запустить поиск в BluetoothService")
            stateChangeCallback(ConnectionState.ERROR, "Не удалось запустить поиск")
            stop()
            return
        }
    }

    /**
     * Обработка завершения итерации поиска.
     */
    private fun handleIterationFinished() {
        if (!isActive) return
        
        currentAttempt++
        log("Итерация поиска завершена ($currentAttempt/$MAX_SEARCH_ATTEMPTS)")

        if (currentAttempt < MAX_SEARCH_ATTEMPTS) {
            log("Ждем $RETRY_INTERVAL_MS мс перед следующей попыткой...")
            searchScope?.launch {
                delay(RETRY_INTERVAL_MS)
                if (isActive) startDiscovery()
            }
        } else {
            log("✗ Все $MAX_SEARCH_ATTEMPTS попыток поиска исчерпаны")
            stateChangeCallback(ConnectionState.DEVICE_UNAVAILABLE, "Устройство не найдено")
            stop()
        }
    }

    /**
     * Остановить поиск устройства.
     * Вызывается BluetoothConnectionManager при необходимости прервать поиск.
     */
    fun stop() {
        if (isActive) {
            log("Остановка монитора доступности")

            isActive = false
            bluetoothService.stopDiscovery()
            discoveryJob?.cancel()
            discoveryJob = null
            stateMonitoringJob?.cancel()
            stateMonitoringJob = null
            searchScope?.cancel()
            cleanup()
        }
    }

    /**
     * Проверить, активен ли поиск устройства.
     * Используется BluetoothConnectionManager для контроля состояния.
     *
     * @return true если поиск активен
     */
    fun isActive(): Boolean {
        return isActive
    }

    /**
     * Очистить ресурсы поиска.
     * Внутренний метод, вызывается после завершения или остановки поиска.
     */
    private fun cleanup() {
        isActive = false
        targetAddress = null
        currentAttempt = 0
        searchScope?.cancel()
        searchScope = null
        log("Ресурсы поиска очищены")
    }

    // ========== МЕТОД ЛОГИРОВАНИЯ ==========

    /**
     * Записать информационное сообщение в лог.
     *
     * @param message Текст сообщения
     */
    private fun log(message: String) {
        AppLogger.logInfo("[$TAG] $message", TAG)
    }
}
