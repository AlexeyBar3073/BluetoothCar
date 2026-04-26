// Файл: data/bluetooth/listeners/DataStreamHandler.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * ТЕГ: BLUETOOTH_TRANSPORT_LAYER / DATA_STREAM_HANDLER
 *
 * ФАЙЛ: data/bluetooth/listeners/DataStreamHandler.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Реализация надежного транспортного уровня поверх Bluetooth RFCOMM. 
 * Использует механизм подтверждений (msg_id/ack_id) для гарантированной доставки JSON-пакетов.
 * Включает в себя очередизацию команд, автоматическую переотправку при потере пакетов 
 * и десериализацию входящего потока данных.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Генерация уникальных ID для исходящих сообщений.
 * 2. Обеспечение циклической переотправки до получения подтверждения (Reliable Messaging).
 * 3. Фильтрация и обработка технических подтверждений (ack_id).
 * 4. Трансляция полезных данных во внешние слои приложения.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Transport Layer (OSI Layer 4 equivalent for custom protocol).
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Чистый транспорт — не содержит бизнес-логики управления автомобилем.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: 
 * - Использует: [AppBluetoothService] (низкоуровневый ввод-вывод).
 * - Вызывается из: [BluetoothConnectionManager] (управление сессией).
 * - Взаимодействует: [OtaManager] (через проброс входящих данных).
 */
class DataStreamHandler(
    private val bluetoothService: AppBluetoothService,
    private val coroutineScope: CoroutineScope,
    private val stateChangeCallback: (ConnectionState, String?) -> Unit,
    /** Диспетчер для выполнения операций ввода-вывода (IO) */
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    /** Тег для логирования компонента */
    private val TAG = "DataStreamHandler"

    // ========== МОДЕЛИ ДАННЫХ ==========

    /**
     * Внутренняя модель команды в очереди на отправку.
     * @property id Уникальный номер сообщения для отслеживания подтверждения.
     * @property payload Полезная нагрузка команды в формате JSON.
     */
    private data class QueuedCommand(
        val id: Long,
        val payload: String
    ) {
        /**
         * Формирует итоговый JSON пакет с внедрением msg_id.
         * @param json Настроенный экземпляр Json.
         */
        fun getEnvelope(json: Json): String {
            val element = try {
                // Пытаемся распарсить payload как объект
                json.parseToJsonElement(payload).jsonObject.toMutableMap()
            } catch (e: Exception) {
                // Если не JSON, оборачиваем в структуру {cmd: payload}
                mutableMapOf<String, JsonElement>("cmd" to JsonPrimitive(payload))
            }
            
            // Внедряем идентификатор сообщения для транспортного уровня
            element["msg_id"] = JsonPrimitive(id)

            return json.encodeToString(JsonObject(element))
        }
    }

    // ========== ПОТОКИ И ОЧЕРЕДИ ==========

    /** Поток входящих сообщений для вышестоящих слоев */
    private val _incomingMessagesFlow = MutableSharedFlow<JsonObject>()
    
    /** Публичный интерфейс потока входящих данных */
    val incomingMessagesFlow: SharedFlow<JsonObject> = _incomingMessagesFlow.asSharedFlow()

    /** Очередь исходящих команд. UNLIMITED гарантирует отсутствие блокировок UI-потока при отправке. */
    private val commandQueue = Channel<QueuedCommand>(Channel.UNLIMITED)

    /** 
     * Поток подтверждений. 
     * ВНИМАНИЕ: replay=0 обязателен! Использование replay=1 приводит к ложным подтверждениям
     * первой команды (ID=1) после переподключения из-за кэширования старого AckID.
     */
    private val acknowledgmentsFlow = MutableSharedFlow<Long>(replay = 0)

    // ========== СОСТОЯНИЕ И СИНХРОНИЗАЦИЯ ==========

    /** Настройки JSON-сериализатора */
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    /** Атомарный счетчик ID сообщений. Сбрасывается при каждом start(). */
    private val msgIdGenerator = AtomicLong(1)

    /** Хранилище последнего полученного ID подтверждения */
    private val lastAckId = AtomicLong(-1L)

    /** Флаг активности обработчика */
    private val isHandlerActive = AtomicBoolean(false)
    
    /** Флаг готовности приемника данных */
    private val isReceiverReady = AtomicBoolean(false)

    /** Задача воркера очереди */
    private var workerJob: Job? = null
    
    /** Задача приемника данных */
    private var receiverJob: Job? = null

    // ========== ПАРАМЕТРЫ ПРОТОКОЛА ==========

    /** Таймаут ожидания подтверждения перед переотправкой (мс) */
    private val SEND_INTERVAL_MS = 1500L

    /**
     * Инициализирует и запускает транспортные процессы.
     * При вызове происходит сброс всех счетчиков и очистка очередей.
     */
    fun start() {
        log("=== ЗАПУСК ТРАНСПОРТНОГО ШЛЮЗА ===")

        if (isHandlerActive.get()) {
            log("Транспорт уже запущен")
            return
        }

        isHandlerActive.set(true)

        // Инициализация состояния новой сессии
        lastAckId.set(-1L)
        msgIdGenerator.set(1L)
        
        // Очистка "хвостов" от предыдущих сессий
        clearQueue()

        // Сначала запускаем приемник, чтобы не пропустить Ack на первую же команду
        startReceiver()

        // Запуск передатчика
        startWorker()

        log("Воркеры транспорта инициализированы")
    }

    /**
     * Очищает очередь исходящих команд.
     * Публичный доступ необходим для экстренной остановки потока данных (например, BluetoothConnectionManager).
     */
    fun clearQueue() {
        var count = 0
        while (true) {
            val result = commandQueue.tryReceive()
            if (result.isSuccess) count++ else break
        }
        if (count > 0) log("Очередь очищена: удалено $count сообщений")
    }

    /**
     * Публичный метод для отправки команды.
     * @param jsonCommand JSON-строка команды.
     */
    fun sendJsonCommand(jsonCommand: String) {
        if (!isHandlerActive.get()) {
            log("ОШИБКА: Попытка отправки в неактивный транспорт: $jsonCommand")
            return
        }
        coroutineScope.launch {
            val id = msgIdGenerator.getAndIncrement()
            log("Команда поставлена в очередь: ID=$id")
            commandQueue.send(QueuedCommand(id, jsonCommand))
        }
    }

    /**
     * Основной воркер передачи данных.
     * Реализует логику ожидания подтверждения и переотправки.
     */
    private fun startWorker() {
        workerJob = coroutineScope.launch(ioDispatcher) {
            // Ждем готовности приемника для синхронизации
            while (!isReceiverReady.get()) delay(50)
            log("Воркер передачи готов")

            for (command in commandQueue) {
                if (!isHandlerActive.get()) break
                
                val envelope = command.getEnvelope(json)
                log("Обработка команды ID=${command.id}")

                // Цикл гарантированной доставки пакета
                while (isHandlerActive.get()) {
                    // Проверка на случай, если подтверждение пришло мгновенно
                    if (lastAckId.get() == command.id) break

                    log("Отправка пакета ID=${command.id} (попытка)")
                    bluetoothService.sendData(envelope)

                    // Ждем подтверждения из потока или срабатывания таймаута
                    withTimeoutOrNull(SEND_INTERVAL_MS) {
                        acknowledgmentsFlow.first { it == command.id }
                    }

                    // Повторная проверка после ожидания
                    if (lastAckId.get() == command.id) break
                    
                    log("Таймаут подтверждения ID=${command.id}, переотправка...")
                }
                log("Команда ID=${command.id} ПОДТВЕРЖДЕНА")
            }
        }
    }

    /**
     * Воркер приема данных из Bluetooth-сервиса.
     */
    private fun startReceiver() {
        receiverJob = coroutineScope.launch(ioDispatcher) {
            log("Приемник данных запущен")
            isReceiverReady.set(true)

            bluetoothService.startDataListening()
                .catch { e -> 
                    log("Критическая ошибка приема: ${e.message}")
                    stateChangeCallback(ConnectionState.ERROR, "Ошибка потока данных")
                }
                .collect { rawData ->
                    // Логируем каждый входящий пакет для отладки
                    AppLogger.logReceive("Bluetooth RAW", rawData)
                    handleIncomingPacket(rawData)
                }
        }
    }

    /**
     * Парсинг и маршрутизация входящего пакета.
     */
    private fun handleIncomingPacket(data: String) {
        if (data.isBlank()) return
        
        try {
            val jsonObject = json.parseToJsonElement(data).jsonObject

            // 1. Извлекаем подтверждение (ack_id), если оно есть
            jsonObject["ack_id"]?.jsonPrimitive?.let { primitive ->
                primitive.longOrNull ?: primitive.content.toLongOrNull()
            }?.let { ackId ->
                log("Получен AckID=$ackId")
                lastAckId.set(ackId)
                // Отправляем в поток для пробуждения воркера
                coroutineScope.launch { 
                    acknowledgmentsFlow.emit(ackId) 
                }
            }

            // 2. Транслируем весь объект наверх (включая Ack и полезную нагрузку)
            coroutineScope.launch {
                _incomingMessagesFlow.emit(jsonObject)
            }

        } catch (e: Exception) {
            log("Ошибка парсинга JSON: ${e.message}")
        }
    }

    /**
     * Останавливает все воркеры и освобождает ресурсы.
     */
    fun stop() {
        if (!isHandlerActive.get()) return
        log("Остановка DataStreamHandler...")

        isHandlerActive.set(false)
        isReceiverReady.set(false)
        
        workerJob?.cancel()
        receiverJob?.cancel()
        
        bluetoothService.stopDataListening()
    }

    /**
     * Полная очистка компонента.
     */
    fun cleanup() {
        stop()
        log("DataStreamHandler полностью очищен")
    }

    /**
     * Внутренний метод логирования.
     */
    private fun log(message: String) {
        AppLogger.logInfo("[$TAG] $message", TAG)
    }
}
