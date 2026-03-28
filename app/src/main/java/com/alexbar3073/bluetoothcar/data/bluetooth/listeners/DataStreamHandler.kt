// Файл: data/bluetooth/listeners/DataStreamHandler.kt
package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ФАЙЛ: data/bluetooth/listeners/DataStreamHandler.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/listeners/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * ОБРАБОТЧИК ПОТОКА ДАННЫХ С BLUETOOTH УСТРОЙСТВОМ. Реализует протокол обмена данными
 * после установления соединения согласно ТЗ: отправка настроек → запрос данных → прием данных.
 *
 * ОТВЕТСТВЕННОСТЬ (СОГЛАСНО ТЗ):
 * 1. Циклическая отправка настроек приложения на устройство (интервал SEND_INTERVAL_MS)
 * 2. Циклическая отправка команды GET_DATA (интервал SEND_INTERVAL_MS)
 * 3. Прием и десериализация потока данных от устройства
 * 4. Обработка подтверждений получения настроек и команд (внутренняя)
 * 5. Уведомление о состоянии протокола через единую функцию обратного вызова
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Работает ТОЛЬКО с доменными моделями (BluetoothDeviceData, AppSettings, CarData)
 * - Все операции выполняются асинхронно в корутинах
 * - Результаты возвращаются через единую функцию обратного вызова
 * - Данные от устройства передаются через Flow
 * - Подтверждения настроек и команд обрабатываются ВНУТРЕННЕ (не передаются в BCM)
 * - Соответствует другим помощникам: один статус = один колбек
 * - СТРОГАЯ ПОСЛЕДОВАТЕЛЬНОСТЬ согласно ТЗ: настройки → подтверждение → команда → подтверждение → данные
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Использует: AppBluetoothService.kt для отправки/приема данных
 * 2. Уведомляет: BluetoothConnectionManager.kt через единую функцию обратного вызова
 * 3. Использует: AppSettings, CarData для сериализации данных
 * 4. Взаимодействует: AppLogger.kt для логирования
 */
class DataStreamHandler(
    private val bluetoothService: AppBluetoothService,
    private val coroutineScope: CoroutineScope,
    private val stateChangeCallback: (ConnectionState, String?) -> Unit
) {
    /** Тег для логирования компонента */
    private val TAG = "DataStreamHandler"

    // ========== ПОТОК ДАННЫХ ДЛЯ BLUETOOTHCONNECTIONMANAGER ==========

    /** Поток данных от устройства для BluetoothConnectionManager */
    private val _carDataFlow = MutableSharedFlow<CarData>()
    val carDataFlow: SharedFlow<CarData> = _carDataFlow.asSharedFlow()

    // ========== JSON СЕРИАЛИЗАЦИЯ ==========

    /** JSON сериализатор с настройками для работы с данными устройства */
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    // ========== ВНУТРЕННИЕ ФЛАГИ ПОДТВЕРЖДЕНИЙ ==========

    /** Флаг подтверждения получения настроек устройством (ВНУТРЕННИЙ) */
    private val settingsAcknowledged = AtomicBoolean(false)

    /** Флаг подтверждения получения команды устройством (ВНУТРЕННИЙ) */
    private val commandAcknowledged = AtomicBoolean(false)

    // ========== ТЕКУЩИЕ ДАННЫЯ ==========

    /** Текущие настройки приложения */
    private var currentSettings: AppSettings? = null

    /** Текущее устройство для обмена данными */
    private var currentDeviceData: BluetoothDeviceData? = null

    // ========== ФЛАГИ СОСТОЯНИЯ ==========

    /** Флаг активности обработчика */
    private val isHandlerActive = AtomicBoolean(false)

    /** Флаг отправки настроек */
    private val isSendingSettings = AtomicBoolean(false)

    /** Флаг отправки команды */
    private val isRequestingData = AtomicBoolean(false)

    /** Флаг приема данных */
    private val isListeningForData = AtomicBoolean(false)

    // ========== СИНХРОНИЗАЦИЯ ПОДПИСКИ И ОТПРАВКИ ==========

    /** Синхронизатор: отправка ждет установки подписки на поток данных */
    private var subscriptionReady = CompletableDeferred<Unit>()

    // ========== КОРУТИНЫ ==========

    /** Job для всего процесса обмена данными */
    private var dataStreamJob: Job? = null

    /** Job для приема данных */
    private var dataListeningJob: Job? = null

    // ========== ПАРАМЕТРЫ ПРОТОКОЛА ==========

    /** Интервал отправки сообщений согласно ТЗ (1500 мс) */
    private val SEND_INTERVAL_MS = 1500L

    /** Таймаут отправки настроек и команд (10 секунд) - единый таймаут */
    private val SEND_TIMEOUT_MS = 10000L

    /**
     * Запустить процесс обмена данными с устройством.
     * Выполняет ПОСЛЕДОВАТЕЛЬНОСТЬ согласно ТЗ: отправка настроек → отправка команды → прием данных.
     * Вызывается: BluetoothConnectionManager.kt после успешного подключения.
     *
     * @param deviceData Устройство для обмена данными
     * @param settings Настройки приложения для отправки на устройство
     */
    fun start(
        deviceData: BluetoothDeviceData,
        settings: AppSettings
    ) {
        log("=== НАЧАЛО МЕТОДА DataStreamHandler.start() ===")

        if (isHandlerActive.get()) {
            log("DataStreamHandler уже активен")
            return
        }

        log("Запуск DataStreamHandler для устройства: ${deviceData.name} (${deviceData.address})")

        this.currentDeviceData = deviceData
        this.currentSettings = settings

        // Сбрасываем внутренние флаги подтверждений
        settingsAcknowledged.set(false)
        commandAcknowledged.set(false)

        // Сбрасываем синхронизатор
        subscriptionReady.cancel()
        subscriptionReady = CompletableDeferred()

        isHandlerActive.set(true)

        log("ШАГ 1: Запуск подписки на поток данных (ТЗ 9.1)")

        // Запускаем подписку на поток данных
        coroutineScope.launch {
            startReceivingProcess()
        }

        log("ШАГ 2: Запуск процесса отправки (будет ждать подписки)")

        // Запускаем процесс отправки (будет ждать subscriptionReady)
        dataStreamJob = coroutineScope.launch {
            try {
                startSendingProcess(deviceData, settings)

                // Проверяем успешное завершение протокола
                if (isHandlerActive.get() &&
                    settingsAcknowledged.get() &&
                    commandAcknowledged.get()) {
                    stateChangeCallback(ConnectionState.LISTENING_DATA, null)
                } else {
                    stop()
                }

            } catch (e: CancellationException) {
                log("Протокол обмена отменен: ${e.message}")
            } catch (e: Exception) {
                log("Ошибка в протоколе обмена: ${e.message}")
                stateChangeCallback(ConnectionState.ERROR, "Ошибка протокола: ${e.message}")
                stop()
            }
        }

        log("Оба процесса запущены, отправка ждет подписки")
    }

    /**
     * Обновить настройки приложения.
     * Если обработчик активен и получает данные, новые настройки отправляются немедленно.
     * Вызывается: BluetoothConnectionManager.kt при изменении настроек во время работы.
     *
     * @param settings Новые настройки приложения
     */
    fun updateAppSettings(settings: AppSettings) {
        log("Обновление настроек")
        this.currentSettings = settings

        if (isHandlerActive.get() && isListeningForData.get()) {
            coroutineScope.launch {
                sendAppSettings(settings)
            }
        }
    }

    /**
     * Отправить команду GET_DATA на устройство.
     * Вызывается из: BluetoothConnectionManager.kt
     */
    fun sendCommand() {
        log("Публичный вызов отправки команды GET_DATA")

        if (isHandlerActive.get() && isListeningForData.get()) {
            coroutineScope.launch {
                sendDataRequestCommand()
            }
        } else {
            log("Обработчик не активен или не получает данные")
        }
    }

    /**
     * Отправить произвольную JSON команду на устройство.
     * @param jsonCommand Строка в формате JSON
     */
    fun sendJsonCommand(jsonCommand: String) {
        log("Публичный вызов отправки JSON команды: $jsonCommand")

        if (isHandlerActive.get() && isListeningForData.get()) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    bluetoothService.sendData(jsonCommand)
                }
            }
        } else {
            log("Обработчик не активен или не получает данные")
        }
    }

    /**
     * Остановить процесс обмена данными.
     * Вызывается BluetoothConnectionManager при разрыве соединения или остановке.
     * Вызывается: BluetoothConnectionManager.kt.
     */
    fun stop() {
        if (!isHandlerActive.get()) {
            return
        }

        log("Остановка DataStreamHandler")

        // Отменяем все корутины
        dataStreamJob?.cancel()
        dataListeningJob?.cancel()

        // Отменяем ожидание подписки
        subscriptionReady.cancel()

        // Останавливаем прослушивание данных в AppBluetoothService
        bluetoothService.stopDataListening()

        // Сбрасываем все флаги
        isHandlerActive.set(false)
        isSendingSettings.set(false)
        isRequestingData.set(false)
        isListeningForData.set(false)

        settingsAcknowledged.set(false)
        commandAcknowledged.set(false)

        log("DataStreamHandler остановлен")
    }

    /**
     * Полная очистка ресурсов обработчика.
     * Вызывается при уничтожении BluetoothConnectionManager.
     * Вызывается: BluetoothConnectionManager.kt.
     */
    fun cleanup() {
        log("Очистка ресурсов DataStreamHandler")
        stop()

        currentDeviceData = null
        currentSettings = null

        dataStreamJob = null
        dataListeningJob = null

        subscriptionReady.cancel()

        log("Ресурсы DataStreamHandler очищены")
    }

    // ========== ПРОЦЕСС ОТПРАВКИ ДАННЫХ ==========

    /**
     * Запустить процесс отправки данных (настроек и команды) ПОСЛЕДОВАТЕЛЬНО.
     * ИСПРАВЛЕНИЕ: Ждет установки подписки на поток данных перед началом отправки.
     * Вызывается: из метода start().
     *
     * @param deviceData Устройство для отправки
     * @param settings Настройки для отправки
     */
    private suspend fun startSendingProcess(
        deviceData: BluetoothDeviceData,
        settings: AppSettings
    ) {
        try {
            log("Начало startSendingProcess, ждем подписки на поток данных")

            // ЖДЕМ УСТАНОВКИ ПОДПИСКИ НА ПОТОК ДАННЫХ
            subscriptionReady.await()
            log("Подписка установлена, начинаем отправку")

            log("Запуск ПОСЛЕДОВАТЕЛЬНОГО процесса отправки данных согласно ТЗ")

            // Шаг 1: Отправка настроек (должна завершиться успешно или таймаутом)
            log("ШАГ 1.1: Отправка настроек на устройство")
            stateChangeCallback(ConnectionState.SENDING_SETTINGS, null)
            val settingsSent = sendAppSettings(settings)
            if (!settingsSent) {
                log("Не удалось отправить настройки, прерываем протокол")
                stateChangeCallback(ConnectionState.ERROR, "Таймаут отправки настроек")
                return
            }

            // Шаг 2: Проверяем подтверждение настроек
            if (!settingsAcknowledged.get()) {
                log("Настройки отправлены, но подтверждение не получено")
                stateChangeCallback(ConnectionState.ERROR, "Нет подтверждения настроек от устройства")
                return
            }

            log("Настройки отправлены и подтверждены, переходим к команде")

            // Шаг 3: Отправка команды запроса данных (ТОЛЬКО после подтверждения настроек)
            log("ШАГ 1.2: Отправка команды GET_DATA")
            stateChangeCallback(ConnectionState.REQUESTING_DATA, null)
            val commandSent = sendDataRequestCommand()
            if (!commandSent) {
                log("Не удалось отправить команду, прерываем протокол")
                stateChangeCallback(ConnectionState.ERROR, "Таймаут отправки команды")
                return
            }

            // Шаг 4: Проверяем подтверждение команды
            if (!commandAcknowledged.get()) {
                log("Команда отправлена, но подтверждение не получено")
                stateChangeCallback(ConnectionState.ERROR, "Нет подтверждения команды от устройства")
                return
            }

            log("Команда отправлена и подтверждена, протокол отправки завершен")

        } catch (e: CancellationException) {
            log("Процесс отправки отменен")
            throw e
        } catch (e: Exception) {
            log("Ошибка в процессе отправки: ${e.message}")
            stateChangeCallback(ConnectionState.ERROR, "Ошибка отправки: ${e.message}")
            throw e
        }
    }

    /**
     * Отправить настройки приложения на устройство.
     * Выполняет циклическую отправку до получения подтверждения или таймаута.
     * Вызывается: из startSendingProcess().
     *
     * @param settings Настройки для отправки
     * @return true if настройки отправлены и подтверждены
     */
    private suspend fun sendAppSettings(settings: AppSettings): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                log("=== НАЧАЛО sendAppSettings() ===")
                isSendingSettings.set(true)

                // Используем toDeviceSettingsJson() вместо всего объекта AppSettings
                val settingsJson = settings.toDeviceSettingsJson()
                val message = """{"settings":$settingsJson}"""

                log("Отправка настроек: ${message.take(100)}...")

                // Сбрасываем флаг подтверждения
                settingsAcknowledged.set(false)

                // Используем общую функцию с таймаутом (как и для команд)
                val success = sendWithTimeoutAndRetry(
                    message = message,
                    acknowledgmentFlag = settingsAcknowledged,
                    statusMessage = "Отправка настроек",
                    onSuccess = {
                        log("Настройки успешно отправлены и подтверждены")
                    }
                )

                isSendingSettings.set(false)
                return@withContext success

            } catch (e: Exception) {
                log("Ошибка отправки настроек: ${e.message}")
                isSendingSettings.set(false)
                false
            }
        }
    }

    /**
     * Отправить команду запроса данных на устройство.
     * Выполняет циклическую отправку до получения подтверждения или таймаута.
     * ВЫЗЫВАЕТ LISTENING_DATA при успешном подтверждении команды (согласно ТЗ 9.6).
     * Вызывается: из startSendingProcess().
     *
     * @return true if команда отправлена и подтверждена
     */
    private suspend fun sendDataRequestCommand(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                isRequestingData.set(true)

                val commandMessage = """{"command":"GET_DATA"}"""
                log("Отправка команды: $commandMessage")

                // Сбрасываем флаг подтверждения
                commandAcknowledged.set(false)

                val success = sendWithTimeoutAndRetry(
                    message = commandMessage,
                    acknowledgmentFlag = commandAcknowledged,
                    statusMessage = "Отправка команды",
                    onSuccess = {
                        // СОГЛАСНО ТЗ 9.6: При подтверждении команды вызываем LISTENING_DATA
                        log("Команда подтверждена, переходим в режим LISTENING_DATA")
                    }
                )

                isRequestingData.set(false)
                return@withContext success

            } catch (e: Exception) {
                log("Ошибка отправки команды: ${e.message}")
                isRequestingData.set(false)
                false
            }
        }
    }

    /**
     * Циклическая отправка сообщения с таймаутом и проверкой подтверждения.
     * Внутренний метод для реализации протокола обмена.
     * Вызывается: из sendAppSettings() и sendDataRequestCommand().
     *
     * @param message Сообщение для отправки
     * @param acknowledgmentFlag Флаг подтверждения получения
     * @param statusMessage Сообщение для логирования
     * @param onSuccess Действие при успешном подтверждении
     * @return true if подтверждение получено
     */
    private suspend fun sendWithTimeoutAndRetry(
        message: String,
        acknowledgmentFlag: AtomicBoolean,
        statusMessage: String,
        onSuccess: () -> Unit = {}
    ): Boolean {
        return try {
            log("=== НАЧАЛО sendWithTimeoutAndRetry ===")
            log("statusMessage: $statusMessage, таймаут: $SEND_TIMEOUT_MS мс, интервал: $SEND_INTERVAL_MS мс")

            withTimeout(SEND_TIMEOUT_MS) {
                var attempt = 0
                while (isHandlerActive.get() && !acknowledgmentFlag.get()) {
                    attempt++

                    val sent = bluetoothService.sendData(message)

                    if (sent) {
                        if (attempt % 10 == 0) { // Логируем каждую 10-ю попытку
                            log("$statusMessage: попытка $attempt...")
                        }

                        delay(SEND_INTERVAL_MS)

                        if (acknowledgmentFlag.get()) {
                            log("$statusMessage: подтверждение получено на попытке $attempt")
                            onSuccess()
                            return@withTimeout true
                        }
                    } else {
                        log("Ошибка отправки $statusMessage, повтор через ${SEND_INTERVAL_MS * 2} мс")
                        delay(SEND_INTERVAL_MS * 2)
                    }
                }
                log("$statusMessage: таймаут без подтверждения")
                false
            }
        } catch (e: TimeoutCancellationException) {
            log("Таймаут $statusMessage: ${e.message}")
            false
        } catch (e: CancellationException) {
            log("$statusMessage отменен: ${e.message}")
            false
        } catch (e: Exception) {
            log("Ошибка $statusMessage: ${e.message}")
            false
        }
    }

    // ========== ПРОЦЕСС ПРИЕМА ДАННЫХ ==========

    /**
     * Запустить процесс приема данных от устройства.
     * ИСПРАВЛЕНИЕ: Сигнализирует о готовности подписки через subscriptionReady.
     * Вызывается: из метода start().
     */
    private suspend fun startReceivingProcess() {
        log("Запуск приёма данных (ожидание установки подписки)")

        try {
            isListeningForData.set(true)

            dataListeningJob = coroutineScope.launch {
                bluetoothService.startDataListening()
                    .catch { e ->
                        log("Ошибка потока данных: ${e.message}")
                        stateChangeCallback(ConnectionState.ERROR, "Ошибка приёма: ${e.message}")
                        subscriptionReady.completeExceptionally(e)
                    }
                    .collect { data ->
                        handleIncomingData(data)
                    }
            }

            // СИГНАЛИЗИРУЕМ: подписка установлена!
            subscriptionReady.complete(Unit)
            log("Подписка на данные установлена, можно отправлять команды")

        } catch (e: CancellationException) {
            log("Приём данных отменён")
            subscriptionReady.completeExceptionally(e)
        } catch (e: Exception) {
            log("Ошибка подпики: ${e.message}")
            subscriptionReady.completeExceptionally(e)
        }
    }

    /**
     * Обработать входящие данные от устройства.
     * Определяет тип сообщения: подтверждение настроек/команды или данные.
     * ВАЖНО: Подтверждения обрабатываются ВНУТРЕННЕ, данные эмитируются в Flow.
     * Вызывается: AppBluetoothService при получении данных.
     *
     * @param data Сырые данные в формате JSON
     */
    private fun handleIncomingData(data: String) {
        try {
            if(data.trim().isEmpty()) return

            log("Получены данные: ${data.take(100)}...")

            val jsonElement = try {
                json.parseToJsonElement(data)
            } catch (e: Exception) {
                log("Ошибка парсинга JSON: ${e.message}")
                return
            }

            val jsonObject = jsonElement.jsonObject

            // Проверка подтверждения настроек (ВНУТРЕННЯЯ обработка)
            if (jsonObject.containsKey("settings")) {
                log("Подтверждение получения настроек (ВНУТРЕННЕЕ)")
                settingsAcknowledged.set(true)  // Устанавливаем ВНУТРЕННИЙ флаг
                return  // НЕ уведомляем BCM
            }

            // Проверка подтверждения команды (ВНУТРЕННЯЯ обработка)
            if (jsonObject.containsKey("GET_DATA")) {
                log("Подтверждение получения команды (ВНУТРЕННЕЕ)")
                commandAcknowledged.set(true)  // Устанавливаем ВНУТРЕННИЙ флаг
                return  // НЕ уведомляем BCM
            }

            // Проверка данных от устройства (эмитируем в Flow)
            if (jsonObject.containsKey("data")) {
                log("Данные от бортового компьютера")
                val dataElement: JsonElement? = jsonObject["data"]
                val dataString = dataElement.toString().trim()
                try {

                    log("Декодируем данные: $dataString")

                    val carData = json.decodeFromString<CarData>(dataString)

                    // Эмитируем данные в поток
                    coroutineScope.launch {
                        _carDataFlow.emit(carData)
                    }
                } catch (e: Exception) {
                    log("Ошибка парсинга CarData: ${e.message} ->${dataString}")
                    stateChangeCallback(ConnectionState.ERROR, "Ошибка парсинга данных: ${e.message}")
                }
                return
            }

            // Неизвестный формат данных
            log("Неизвестный формат данных: ${data.take(50)}...")

        } catch (e: Exception) {
            log("Ошибка обработки входящих данных: ${e.message}")
            stateChangeCallback(ConnectionState.ERROR, "Ошибка обработки данных: ${e.message}")
        }
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Проверить, активен ли обработчик данных.
     * Вызывается: BluetoothConnectionManager.kt.
     *
     * @return true if обработчик активен
     */
    fun isActive(): Boolean {
        return isHandlerActive.get()
    }

    /**
     * Проверить, находится ли обработчик в режиме приема данных.
     * Вызывается: BluetoothConnectionManager.kt.
     *
     * @return true if прием данных активен
     */
    fun isListeningForData(): Boolean {
        return isListeningForData.get()
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
