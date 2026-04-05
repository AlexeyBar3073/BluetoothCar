// Файл: data/logging/AppLogger.kt
package com.alexbar3073.bluetoothcar.data.logging

import android.util.Log
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

/**
 * ТЕГ: Logging/AppLogger
 *
 * ФАЙЛ: data/logging/AppLogger.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/logging/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Единый менеджер логирования для всего приложения.
 * Предоставляет форматированный вывод логов с номерами пакетов и категориями.
 * Использует стандартный Android Log (Logcat), который сам добавляет метки времени.
 *
 * ОТВЕТСТВЕННОСТЬ: 
 * 1. Централизованный вывод диагностических сообщений.
 * 2. Фильтрация логов по флагу отладки.
 * 3. Форматирование сообщений согласно ТЗ.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Singleton / Proxy to Android Log
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: 
 * Логирование выполняется только если активирован флаг isDebugMode.
 * В релизных сборках логгер полностью отключается для экономии ресурсов.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Используется: ВСЕМИ компонентами приложения для логирования
 * 2. Конфигурируется: AppController при инициализации
 * 3. Предоставляет: Единый формат логов согласно ТЗ
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.03 01:00 UTC: ПЕРЕИМЕНОВАНИЕ BluetoothLogger → AppLogger
 *   Файл перемещен из data/bluetooth/logging/ в data/logging/
 * - 2026.02.03: Добавлена поддержка вывода полных пакетов и фильтрация по флагу отладки.
 */
object AppLogger {

    /** Глобальный тег для фильтрации всех логов приложения в Logcat */
    private const val GLOBAL_TAG = "BluetoothCar"

    /** Флаг разрешения логирования (ключ управления) */
    private var isDebugMode = true

    /** Флаг подробного вывода информации */
    private var verbose = true

    /** Флаг отображения порядкового номера сообщения */
    private var showPacketNumbers = true

    /** Счетчик пакетов для отслеживания порядка сообщений */
    private val packetCounter = AtomicInteger(0)

    /**
     * Настроить параметры логирования.
     * Вызывается AppController при инициализации.
     *
     * @param isDebug Разрешить логирование (обычно передается BuildConfig.DEBUG)
     * @param verbose Включить подробное логирование
     * @param packetNumbers Показывать номера пакетов
     */
    fun configure(
        isDebug: Boolean = true,
        verbose: Boolean = true,
        packetNumbers: Boolean = true
    ) {
        // Устанавливаем глобальные настройки
        this.isDebugMode = isDebug
        this.verbose = verbose
        this.showPacketNumbers = packetNumbers

        // Логируем факт изменения конфигурации
        logInfo(
            "Конфигурация логирования: isDebug=$isDebugMode, verbose=$verbose, packetNumbers=$packetNumbers",
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
        // Проверяем, разрешено ли логирование и активен ли режим verbose
        if (isDebugMode && verbose) {
            // Форматируем сообщение согласно стандартам проекта
            val formattedMessage = formatLogMessage(message, component, "INFO")
            // Выводим в системный лог на уровне INFO
            Log.i(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Логировать сообщение об ошибке.
     * Ошибки логируются всегда при активном дебаге, независимо от флага verbose.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logError(message: String, component: String) {
        // Ошибки выводим только в режиме отладки
        if (isDebugMode) {
            // Форматируем сообщение
            val formattedMessage = formatLogMessage(message, component, "ERROR")
            // Выводим в системный лог на уровне ERROR
            Log.e(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Логировать предупреждение.
     *
     * @param message Сообщение для логирования
     * @param component Компонент, из которого производится логирование
     */
    fun logWarning(message: String, component: String) {
        // Проверяем условия вывода
        if (isDebugMode && verbose) {
            // Форматируем сообщение
            val formattedMessage = formatLogMessage(message, component, "WARN")
            // Выводим в системный лог на уровне WARN
            Log.w(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Логировать событие подключения.
     */
    @Suppress("unused")
    fun logConnection(message: String, component: String) {
        if (isDebugMode && verbose) {
            val formattedMessage = formatLogMessage(message, component, "CONN")
            Log.d(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Логировать изменение состояния протокола.
     */
    @Suppress("unused")
    fun logStateChange(message: String, component: String) {
        if (isDebugMode && verbose) {
            val formattedMessage = formatLogMessage(message, component, "STATE")
            Log.d(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Логировать отправку данных.
     */
    @Suppress("unused")
    fun logSend(message: String, data: String) {
        if (isDebugMode && verbose) {
            val formattedMessage = formatLogMessage("$message: $data", "DataStream", "SEND")
            Log.v(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Логировать прием данных.
     * Выводит полную строку данных без сокращений для детального анализа.
     *
     * @param message Описание действия
     * @param data Полученная сырая строка
     */
    fun logReceive(message: String, data: String) {
        // Проверяем глобальные флаги
        if (isDebugMode && verbose) {
            // Формируем полную строку без обрезания для анализа полей
            val formattedMessage = formatLogMessage(
                "$message: $data",
                "DataStream",
                "RECV"
            )
            // Используем уровень VERBOSE для сырых данных
            Log.v(GLOBAL_TAG, formattedMessage)
        }
    }

    /**
     * Форматирует сообщение для вывода.
     * Собирает строку: "[НОМЕР] [ТИП] [КОМПОНЕНТ] Сообщение"
     */
    private fun formatLogMessage(message: String, component: String, logType: String): String {
        // Формируем строку номера пакета, если это включено в настройках
        val packetNumberStr = if (showPacketNumbers) {
            // Используем Locale.US для стабильного формата чисел
            String.format(Locale.US, "[%04d] ", packetCounter.incrementAndGet())
        } else ""

        // Собираем итоговую строку. Временную метку не добавляем, так как её добавит Logcat.
        return "$packetNumberStr[$logType] [$component] $message"
    }

    /**
     * Получить текущий порядковый номер сообщения.
     */
    @Suppress("unused")
    fun getCurrentPacketNumber(): Int {
        return packetCounter.get()
    }

    /**
     * Сбросить счетчик сообщений.
     */
    @Suppress("unused")
    fun resetPacketCounter() {
        // Сбрасываем атомарный счетчик в ноль
        packetCounter.set(0)
        logInfo("Счетчик пакетов сброшен", "AppLogger")
    }

    /** Возвращает статус активности подробного логирования */
    fun isVerbose(): Boolean = verbose

    /** Возвращает статус отображения номеров пакетов */
    fun showsPacketNumbers(): Boolean = showPacketNumbers
}
