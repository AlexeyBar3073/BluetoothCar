// Файл: data/bluetooth/listeners/DeviceAvailabilityMonitor.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * ТЕГ: BLUETOOTH_CONNECTION_MANAGER_HELPER
 *
 * ФАЙЛ: data/bluetooth/listeners/DeviceAvailabilityMonitor.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Монитор физической доступности Bluetooth устройства. Выполняет активный поиск устройства
 * в эфире для подтверждения возможности подключения согласно алгоритму "Сначала найди, потом подключай".
 *
 * ОТВЕТСТВЕННОСТЬ (СОГЛАСНО ТЗ):
 * 1. Поиск целевого устройства в окружении хост-устройства.
 * 2. Выполнение до 3 попыток поиска с интервалом 1 секунда (согласно регламенту).
 * 3. Уведомление системы о начале и стадиях поиска через callback.
 * 4. Возврат статусов: DEVICE_AVAILABLE (найдено), DEVICE_UNAVAILABLE (не найдено) или ERROR.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Работает исключительно с доменными моделями (BluetoothDeviceData).
 * - Все операции выполняются асинхронно в изолированном корутин-скоупе.
 * - Реактивное управление: подписка на discoveryFlow сервиса и трансляция состояний.
 * - Не меняет глобальный ConnectionState напрямую, только делегирует результат менеджеру.
 *
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Монитор инкапсулирует логику "умных попыток" поиска, используя AppBluetoothService 
 * только как транспорт для получения событий из Bluetooth-эфира.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: AppBluetoothService.kt (Доступ к Discovery API)
 * - Вызывается из: BluetoothConnectionManager.kt (Оркестратор процесса)
 * - Взаимодействует: AppLogger.kt (Диагностика)
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
        log("Запуск системного поиска (попытка ${currentAttempt}/$MAX_SEARCH_ATTEMPTS)")

        // 1. Отменяем предыдущие подписки, чтобы не плодить слушателей при ретраях
        discoveryJob?.cancel()
        stateMonitoringJob?.cancel()

        // 2. Подписываемся на поток обнаруженных устройств
        discoveryJob = searchScope?.launch {
            bluetoothService.discoveryFlow.collect { device ->
                // Если монитор активен и адрес совпал — цель достигнута
                if (isActive && device.address == targetAddress) {
                    log("✓ Найдено целевое устройство: ${device.name} (${device.address})")
                    // Останавливаем системный поиск немедленно
                    bluetoothService.stopDiscovery()
                    // Уведомляем менеджер о доступности
                    stateChangeCallback(ConnectionState.DEVICE_AVAILABLE, null)
                    // Завершаем работу монитора
                    stop()
                }
            }
        }

        // 3. Подписываемся на состояние поиска для управления ретраями (реактивный подход)
        stateMonitoringJob = searchScope?.launch {
            bluetoothService.isDiscovering
                .drop(1) // Пропускаем начальное состояние StateFlow ( fusion optimization )
                .collect { isNowDiscovering ->
                    // Если поиск перешел в состояние false — значит итерация завершена на уровне системы
                    if (!isNowDiscovering) {
                        log("Система сообщила о завершении сканирования")
                        handleIterationFinished()
                    }
                }
        }

        // 4. Запускаем сам поиск в Bluetooth-сервисе
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
     * Если устройство не найдено, планирует следующую попытку или завершает мониторинг.
     */
    private fun handleIterationFinished() {
        // Если поиск уже был остановлен извне (или найден), ничего не делаем
        if (!isActive) return
        
        currentAttempt++
        log("Итерация поиска завершена ($currentAttempt/$MAX_SEARCH_ATTEMPTS)")

        // Если лимит попыток не исчерпан — планируем следующую через 1 секунду
        if (currentAttempt <= MAX_SEARCH_ATTEMPTS) {
            log("Ждем $RETRY_INTERVAL_MS мс перед следующей попыткой...")
            searchScope?.launch {
                delay(RETRY_INTERVAL_MS)
                // Проверяем активность еще раз после задержки
                if (isActive) startDiscovery()
            }
        } else {
            // Если все попытки исчерпаны — уведомляем о недоступности
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
