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
 * ТЕГ: DataStreamHandler
 * ФАЙЛ: data/bluetooth/listeners/DataStreamHandler.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ТРАНСПОРТНЫЙ ШЛЮЗ ПОТОКА ДАННЫХ. Реализует низкоуровневый механизм обмена данными
 * по принципу: очередь команд -> упаковка в JSON с msg_id -> ожидание подтверждения ack_id.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Гарантированная доставка исходящих JSON-пакетов (циклическая переотправка до получения ack_id).
 * 2. Присвоение уникальных msg_id всем исходящим сообщениям.
 * 3. Трансляция входящего потока данных в виде JsonObject для вышестоящих компонентов.
 * 4. Управление низкоуровневыми потоками чтения/записи Bluetooth-сервиса.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Чистый транспорт: не содержит бизнес-логики, не знает о структуре CarData или настройках.
 * - Работает исключительно с JSON-пакетами.
 * - Ответственность за контент сообщений лежит на вызывающем компоненте (оркестраторе).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: AppBluetoothService.kt для низкоуровневая отправки/приема данных.
 * 2. Уведомляет: BluetoothConnectionManager.kt через входящий поток сообщений.
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
        val id: String,
        val payload: String
    ) {
        /**
         * Формирует итоговый JSON пакет с внедрением msg_id.
         * @param json Настроенный экземпляр Json.
         */
        fun getEnvelope(json: Json): String {
            val element = json.parseToJsonElement(payload).jsonObject.toMutableMap()
            element["msg_id"] = JsonPrimitive(id)
            return json.encodeToString(JsonObject(element))
        }
    }

    // ========== ПОТОКИ И ОЧЕРЕДИ ==========

    /** Поток входящих сообщений (распарсенный JsonObject) для трансляции наверх */
    private val _incomingMessagesFlow = MutableSharedFlow<JsonObject>()
    val incomingMessagesFlow: SharedFlow<JsonObject> = _incomingMessagesFlow.asSharedFlow()

    /** Очередь исходящих команд (Unlimited для предотвращения блокировки отправителей) */
    private val commandQueue = Channel<QueuedCommand>(Channel.UNLIMITED)

    /** Поток входящих AckID для пробуждения воркера. Используем replay=1 для исключения гонок. */
    private val acknowledgmentsFlow = MutableSharedFlow<String>(replay = 1)

    // ========== СОСТОЯНИЕ И СИНХРОНИЗАЦИЯ ==========

    /** JSON сериализатор */
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    
    /** Генератор уникальных ID сообщений */
    private val msgIdGenerator = AtomicLong(1)

    /** Последний полученный ID подтверждения для проверки в цикле */
    private val lastAckId = AtomicReference<String?>(null)

    /** Флаг активности обработчика */
    private val isHandlerActive = AtomicBoolean(false)
    /** Флаг готовности приемника к приему подтверждений */
    private val isReceiverReady = AtomicBoolean(false)

    /** Job для цикла обработки очереди */
    private var workerJob: Job? = null
    /** Job для приема входящих данных */
    private var receiverJob: Job? = null

    // ========== ПАРАМЕТРЫ ПРОТОКОЛА ==========

    /** Интервал между попытками переотправки при отсутствии Ack (мс) */
    private val SEND_INTERVAL_MS = 1500L

    /**
     * Запустить процессы приема и передачи.
     * Инициализирует воркеры и открывает транспортный канал.
     * Не отправляет никаких стартовых команд (сценарий управляется извне).
     */
    fun start() {
        log("=== ЗАПУСК ТРАНСПОРТНОГО ШЛЮЗА DataStreamHandler ===")

        if (isHandlerActive.get()) {
            log("DataStreamHandler уже активен")
            return
        }

        isHandlerActive.set(true)
        lastAckId.set(null)

        // 1. Запускаем приемник (должен слушать Ack до начала отправки)
        startReceiver()

        // 2. Запускаем воркер очереди
        startWorker()

        log("Транспортные воркеры запущены. Ожидание внешних команд.")
    }

    /**
     * Поместить команду в очередь на отправку.
     * Генерирует уникальный ID для команды.
     * 
     * @param jsonPayload JSON строка команды.
     */
    private suspend fun queueCommand(jsonPayload: String) {
        val id = msgIdGenerator.getAndIncrement().toString()
        log("Добавление в очередь: ID=$id, Payload=${jsonPayload.take(50)}...")
        commandQueue.send(QueuedCommand(id, jsonPayload))
    }

    /**
     * Публичный метод для отправки JSON команды в очередь.
     * 
     * @param jsonCommand Строка JSON.
     */
    fun sendJsonCommand(jsonCommand: String) {
        if (!isHandlerActive.get()) return
        coroutineScope.launch {
            queueCommand(jsonCommand)
        }
    }

    /**
     * Запуск основного воркера обработки очереди команд.
     * Реализует цикл гарантированной доставки с ожиданием подтверждения.
     */
    private fun startWorker() {
        workerJob = coroutineScope.launch(ioDispatcher) {
            log("Worker: Ожидание готовности приемника...")
            while (!isReceiverReady.get()) delay(50)

            // Обрабатываем команды из канала по одной
            for (command in commandQueue) {
                if (!isHandlerActive.get()) break
                
                val envelope = command.getEnvelope(json)
                log("Worker: Берем команду ID=${command.id}")

                // Цикл гарантированной доставки: отправляем пока lastAckId не станет равен нашему id
                while (isHandlerActive.get()) {
                    // 1. Проверяем, может подтверждение уже получено до начала цикла
                    if (lastAckId.get() == command.id) {
                        log("Worker: Команда ID=${command.id} уже подтверждена")
                        break
                    }

                    log("Worker: Отправка пакета ID=${command.id}")
                    bluetoothService.sendData(envelope)

                    // 2. "Умное ожидание": ждем подтверждения в потоке, но не дольше интервала
                    withTimeoutOrNull(SEND_INTERVAL_MS) {
                        acknowledgmentsFlow.first { it == command.id }
                    }

                    // 3. После таймаута или получения ack проверяем еще раз
                    if (lastAckId.get() == command.id) break
                }
                log("Worker: Команда ID=${command.id} успешно завершена")
            }
        }
    }

    /**
     * Запуск приемника входящих данных.
     * Транслирует входящий поток из BluetoothService в локальный incomingMessagesFlow.
     */
    private fun startReceiver() {
        receiverJob = coroutineScope.launch(ioDispatcher) {
            log("Receiver: Подписка на поток данных")
            isReceiverReady.set(true)

            bluetoothService.startDataListening()
                .catch { e -> 
                    log("Receiver: Критическая ошибка потока: ${e.message}")
                    stateChangeCallback(ConnectionState.ERROR, "Ошибка приема: ${e.message}")
                }
                .collect { data ->
                    // ГЛОБАЛЬНОЕ ЛОГИРОВАНИЕ ПРИЕМА ДАННЫХ
                    AppLogger.logReceive("Входящий пакет", data)
                    handleIncomingData(data)
                }
        }
    }

    /**
     * Обработать входящий JSON пакет.
     * Распознает подтверждения (ack_id) для внутренней логики транспорта,
     * все остальные сообщения пробрасывает наверх.
     * 
     * @param data Входящая строка JSON.
     */
    private fun handleIncomingData(data: String) {
        if (data.isBlank()) return
        
        try {
            val jsonObject = json.parseToJsonElement(data).jsonObject

            // 1. Техническая часть транспорта: Проверка на подтверждение (Ack)
            jsonObject["ack_id"]?.jsonPrimitive?.content?.let { ackId ->
                log("Receiver: Получен AckID=$ackId")
                lastAckId.set(ackId)
                coroutineScope.launch { acknowledgmentsFlow.emit(ackId) }
                // Убран return, так как пакет может быть комбинированным (Ack + Данные)
            }

            // 2. Пробрасываем распарсенный объект наверх
            coroutineScope.launch {
                _incomingMessagesFlow.emit(jsonObject)
            }

        } catch (e: Exception) {
            log("Receiver: Ошибка разбора пакета: ${e.message}")
        }
    }

    /**
     * Остановить все активные процессы обмена.
     */
    fun stop() {
        if (!isHandlerActive.get()) return
        log("Остановка DataStreamHandler")

        isHandlerActive.set(false)
        isReceiverReady.set(false)
        
        workerJob?.cancel()
        receiverJob?.cancel()
        
        bluetoothService.stopDataListening()
    }

    /**
     * Очистить ресурсы и сбросить состояние.
     */
    fun cleanup() {
        stop()
        log("Ресурсы DataStreamHandler очищены")
    }

    /**
     * Логирование с тегом компонента.
     * @param message Текст сообщения.
     */
    private fun log(message: String) {
        AppLogger.logInfo("[$TAG] $message", TAG)
    }
}
