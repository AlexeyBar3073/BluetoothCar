// Файл: data/bluetooth/listeners/DeviceAvailabilityMonitor.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import kotlinx.coroutines.*

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
 * - Монитор создает callback-функции и передает их в BluetoothService
 * - BluetoothService только инструмент, не содержит бизнес-логики
 * - Сравнение адресов и управление попытками выполняется в мониторе
 * - Реактивная архитектура без блокировок
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: BluetoothService.kt для поиска устройств в эфире
 * 2. Уведомляет: BluetoothConnectionManager.kt через единую функцию обратного вызова
 * 3. Взаимодействует: AppLogger.kt для логирования
 */

class DeviceAvailabilityMonitor(
    private val bluetoothService: BluetoothService,
    private val stateChangeCallback: (ConnectionState, String?) -> Unit
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

    // ========== CALLBACK-ФУНКЦИИ ДЛЯ BLUETOOTH SERVICE ==========

    /**
     * Callback-функция для обработки найденных устройств.
     * Передается в BluetoothService.startDiscovery()
     * Вызывается BluetoothService при обнаружении любого устройства в эфире.
     *
     * @param address MAC-адрес найденного устройства
     */
    private val onDeviceFound: (String) -> Unit = { address ->
        if (address == targetAddress) {
            log("✓ Найдено целевое устройство с адресом: $address")

            // Останавливаем поиск
            bluetoothService.stopDiscovery()

            // Уведомляем о доступности устройства
            stateChangeCallback(ConnectionState.DEVICE_AVAILABLE, null)

            // Останавливаем монитор
            stop()
        }
    }

    /**
     * Callback-функция для обработки завершения поиска.
     * Передается в BluetoothService.startDiscovery()
     * Вызывается BluetoothService при завершении системного поиска.
     */
    private val onDiscoveryFinished: () -> Unit = {
        log("Системный поиск завершен (попытка $currentAttempt/$MAX_SEARCH_ATTEMPTS)")

        if (isActive) {
            currentAttempt++

            if (currentAttempt < MAX_SEARCH_ATTEMPTS) {
                log("Ждем $RETRY_INTERVAL_MS мс перед следующей попыткой...")

                // Запускаем следующую попытку через интервал
                searchScope?.launch {
                    delay(RETRY_INTERVAL_MS)

                    if (isActive) {
                        log("Запуск попытки поиска ${currentAttempt + 1}/$MAX_SEARCH_ATTEMPTS")
                        startDiscovery()
                    }
                }
            } else {
                log("✗ Все $MAX_SEARCH_ATTEMPTS попыток поиска исчерпаны")

                // Уведомляем о недоступности устройства
                stateChangeCallback(ConnectionState.DEVICE_UNAVAILABLE, null)

                // Останавливаем монитор
                stop()
            }
        }
    }

    /**
     * Callback-функция для обработки ошибок поиска.
     * Передается в BluetoothService.startDiscovery()
     * Вызывается BluetoothService при ошибке запуска или выполнения поиска.
     *
     * @param error Сообщение об ошибке
     */
    private val onDiscoveryError: (String) -> Unit = { error ->
        log("Ошибка поиска: $error")

        // Уведомляем об ошибке
        stateChangeCallback(ConnectionState.ERROR, error)

        // Останавливаем монитор
        stop()
    }

    /**
     * Начать поиск устройства в Bluetooth эфире.
     * Выполняет до 3 попыток поиска с интервалом 1 секунда согласно новым требованиям.
     * Вызывается из: BluetoothConnectionManager.kt
     *
     * @param targetDeviceData Устройство для поиска (должно быть сопряжено)
     */
    fun start(targetDeviceData: BluetoothDeviceData) {
        if (isActive) {
            stateChangeCallback(ConnectionState.ERROR, "Поиск уже выполняется")
            return
        }

        targetAddress = targetDeviceData.address
        currentAttempt = 0
        isActive = true

        log("Начало поиска устройства: ${targetDeviceData.name} (${targetDeviceData.address})")

        // СОГЛАСНО ТЗ: уведомление о начале поиска
        stateChangeCallback(ConnectionState.SEARCHING_DEVICE, null)

        searchScope = CoroutineScope(Dispatchers.IO + Job())

        // Запускаем первую попытку поиска
        startDiscovery()
    }

    /**
     * Запустить системный поиск устройств через BluetoothService.
     * Внутренний метод, передает callback-функции в BluetoothService.
     * Вызывается при старте и при перезапуске попыток.
     */
    private fun startDiscovery() {
        log("Запуск системного поиска (попытка ${currentAttempt + 1}/$MAX_SEARCH_ATTEMPTS)")

        // Передаем наши callback-функции в BluetoothService
        bluetoothService.startDiscovery(
            onDeviceFound = onDeviceFound,
            onDiscoveryFinished = onDiscoveryFinished,
            onDiscoveryError = onDiscoveryError
        )
    }

    /**
     * Остановить поиск устройства.
     * Вызывается BluetoothConnectionManager при необходимости прервать поиск.
     */
    fun stop() {
        if (isActive) {
            log("Остановка поиска")

            isActive = false
            bluetoothService.stopDiscovery()
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