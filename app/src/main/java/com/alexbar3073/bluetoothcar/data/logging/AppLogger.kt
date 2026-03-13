// Файл: data/logging/AppLogger.kt
package com.alexbar3073.bluetoothcar.data.logging

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * ФАЙЛ: data/logging/AppLogger.kt
 * МЕСТОНАХОЖДЕНИЕ: data/logging/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Единый менеджер логирования для всего приложения.
 * Предоставляет форматированный вывод логов с таймстемпами, номерами пакетов и категориями.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Используется: ВСЕМИ компонентами приложения для логирования
 * 2. Конфигурируется: AppController при инициализации
 * 3. Предоставляет: Единый формат логов согласно ТЗ
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.03 01:00 UTC: ПЕРЕИМЕНОВАНИЕ BluetoothLogger → AppLogger
 *   Файл перемещен из data/bluetooth/logging/ в data/logging/
 */

/**
 * Объект-синглтон для управления логированием во всем приложении.
 * СОГЛАСНО ТЗ: Формат "[ВРЕМЯ] [КОМПОНЕНТ] Сообщение"
 */
object AppLogger {

    // Конфигурация логирования
    private var verbose = true
    private var showTimestamps = true
    private var showPacketNumbers = true

    // Счетчик пакетов для отслеживания порядка сообщений
    private val packetCounter = AtomicInteger(0)

    // Формат времени для логов
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    /**
     * Настроить параметры логирования.
     * Вызывается AppController при инициализации.
     *
     * @param verbose Включить подробное логирование
     * @param timestamps Показывать временные метки
     * @param packetNumbers Показывать номера пакетов
     */
    fun configure(
        verbose: Boolean = true,
        timestamps: Boolean = true,
        packetNumbers: Boolean = true
    ) {
        this.verbose = verbose
        this.showTimestamps = timestamps
        this.showPacketNumbers = packetNumbers

        logInfo(
            "[AppLogger] Конфигурация логирования: verbose=$verbose, timestamps=$timestamps, packetNumbers=$packetNumbers",
            "AppLogger"
        )
    }

    /**
     * Логировать информационное сообщение.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logInfo(message: String, component: String) {
        if (verbose) {
            val formattedMessage = formatLogMessage(message, component, "Информация")
            println(formattedMessage)
        }
    }

    /**
     * Логировать сообщение об ошибке.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logError(message: String, component: String) {
        // Ошибки логируем всегда, даже если verbose = false
        val formattedMessage = formatLogMessage(message, component, "Ошибка")
        System.err.println(formattedMessage)
    }

    /**
     * Логировать предупреждение.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logWarning(message: String, component: String) {
        if (verbose) {
            val formattedMessage = formatLogMessage(message, component, "Предупреждение")
            println(formattedMessage)
        }
    }

    /**
     * Логировать событие подключения.
     * Используется для событий, связанных с установкой/разрывом соединений.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logConnection(message: String, component: String) {
        if (verbose) {
            val formattedMessage = formatLogMessage(message, component, "Событие соединения")
            println(formattedMessage)
        }
    }

    /**
     * Логировать изменение состояния протокола.
     * Используется для отслеживания переходов между состояниями.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logStateChange(message: String, component: String) {
        if (verbose) {
            val formattedMessage =
                formatLogMessage(message, component, "Изменение состояния протокола")
            println(formattedMessage)
        }
    }

    /**
     * Логировать отправку данных.
     *
     * @param message Сообщение для логирования
     * @param data Отправляемые данные
     */
    fun logSend(message: String, data: String) {
        if (verbose) {
            val formattedMessage =
                formatLogMessage("$message: $data", "DataStream", "Отправка данных")
            println(formattedMessage)
        }
    }

    /**
     * Логировать прием данных.
     *
     * @param message Сообщение для логирования
     * @param data Принимаемые данные
     */
    fun logReceive(message: String, data: String) {
        if (verbose) {
            val formattedMessage = formatLogMessage(
                "$message: ${data.take(100)}${if (data.length > 100) "..." else ""}",
                "DataStream",
                "Прием данных"
            )
            println(formattedMessage)
        }
    }

    /**
     * Отформатировать сообщение лога согласно ТЗ.
     * Формат: "[ВРЕМЯ] [КОМПОНЕНТ] Сообщение" или "[НОМЕР_ПАКЕТА] [ВРЕМЯ] [КОМПОНЕНТ] Сообщение"
     *
     * @param message Исходное сообщение
     * @param component Компонент-источник
     * @param logType Тип лога (для категоризации)
     * @return Отформатированная строка лога
     */
    private fun formatLogMessage(message: String, component: String, logType: String): String {
        val packetNumber = if (showPacketNumbers) {
            val number = packetCounter.incrementAndGet()
            String.format("%04d", number)
        } else {
            ""
        }

        val timestamp = if (showTimestamps) {
            timeFormat.format(Date())
        } else {
            ""
        }

        return buildString {
            if (packetNumber.isNotEmpty()) {
                append("$packetNumber ")
            }
            if (timestamp.isNotEmpty()) {
                append("$timestamp ")
            }
            if (logType.isNotEmpty()) {
                append("$logType в $component: ")
            }
            append(message)
        }
    }

    /**
     * Получить текущий номер пакета.
     * Полезно для отладки и отслеживания порядка сообщений.
     *
     * @return Текущий номер пакета
     */
    fun getCurrentPacketNumber(): Int {
        return packetCounter.get()
    }

    /**
     * Сбросить счетчик пакетов.
     * Вызывается при перезапуске соединения или для отладки.
     */
    fun resetPacketCounter() {
        packetCounter.set(0)
        logInfo("[AppLogger] Счетчик пакетов сброшен", "AppLogger")
    }

    /**
     * Проверить, включено ли подробное логирование.
     *
     * @return true если verbose = true, false в противном случае
     */
    fun isVerbose(): Boolean = verbose

    /**
     * Проверить, показываются ли временные метки.
     *
     * @return true если showTimestamps = true, false в противном случае
     */
    fun showsTimestamps(): Boolean = showTimestamps

    /**
     * Проверить, показываются ли номера пакетов.
     *
     * @return true если showPacketNumbers = true, false в противном случае
     */
    fun showsPacketNumbers(): Boolean = showPacketNumbers
}