// Файл: data/bluetooth/OtaManager.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import android.util.Base64
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

/**
 * ТЕГ: OtaManager
 * 
 * ФАЙЛ: data/bluetooth/OtaManager.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Компонент управления процессом OTA (Over-The-Air) обновления прошивки БК.
 * Реализует сценарий: Валидация -> Чтение -> Разбиение на чанки -> Формирование JSON.
 * 
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Валидация файлов прошивки (имя, расширение).
 * 2. Чтение бинарных данных и конвертация в Base64.
 * 3. Сегментация данных на пакеты заданного размера (256 байт).
 * 4. Формирование протокольных JSON-команд: ota_start, ota_data, ota_end.
 * 5. Отслеживание прогресса передачи.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Инкапсуляция логики подготовки данных для OTA.
 * - Не зависит от UI напрямую, предоставляет состояние через StateFlow.
 * - Использует DataStreamHandler через AppController для физической отправки.
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП:
 * Надежная доставка прошивки частями без блокировки основной телеметрии.
 * 
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Используется: AppController.kt для запуска процесса и отправки команд.
 * Взаимодействует: DataStreamHandler.kt (через контроллер).
 */
class OtaManager {
    private val TAG = "OtaManager"

    /** Размер чанка данных в байтах (согласно спецификации) */
    private val CHUNK_SIZE = 256

    /** Состояния процесса обновления */
    sealed class OtaState {
        /** Простой, ожидание запуска */
        object Idle : OtaState()
        
        /** Валидация выбранного файла */
        object Validating : OtaState()
        
        /** Ожидание ответа ota_init от БК после отправки команды ota_update */
        object WaitingForInit : OtaState()
        
        /** Процесс передачи чанков данных */
        data class Sending(val progress: Float, val currentPacket: Int, val totalPackets: Int) : OtaState()
        
        /** Ожидание перезагрузки БК после отправки ota_end */
        object WaitingForReboot : OtaState()
        
        /** Успешное завершение обновления с указанием новой версии */
        data class Success(val newVersion: String) : OtaState()
        
        /** Ошибка в процессе обновления */
        data class Error(val message: String) : OtaState()
    }

    /** Текущее состояние процесса для UI */
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

    /** Хранилище байтов прошивки для нарезки на чанки */
    private var firmwareData: ByteArray? = null
    
    /** Размер чанка, определенный БК в ota_init */
    private var currentChunkSize: Int = 0
    
    /** Общее количество чанков (пакетов) */
    var totalChunks: Int = 0
        private set

    /** Номер текущего отправляемого пакета */
    private var currentPacketIndex: Int = 0

    /** Область видимости для корутин таймаутов */
    private val otaScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    /** Задача для отслеживания таймаута перезагрузки */
    private var rebootTimeoutJob: Job? = null

    /** Константа таймаута перезагрузки (15 секунд) */
    private val REBOOT_TIMEOUT_MS = 15_000L

    /**
     * Валидация файла прошивки.
     * Имя должно содержать "firm", "car" и иметь расширение ".bin".
     * 
     * @param fileName Имя файла для проверки.
     * @return true если файл валиден, иначе false.
     */
    fun validateFile(fileName: String): Boolean {
        // Приведение к нижнему регистру для нечувствительности к регистру
        val lowerName = fileName.lowercase()
        
        // Проверка расширения
        if (!lowerName.endsWith(".bin")) {
            AppLogger.logError("OTA: Неверное расширение файла. Ожидается .bin", TAG)
            return false
        }
        
        // Проверка вхождения ключевых слов
        val hasFirm = lowerName.contains("firm")
        val hasCar = lowerName.contains("car")
        
        if (!hasFirm || !hasCar) {
            AppLogger.logError("OTA: Имя файла должно содержать 'firm' и 'car'", TAG)
            return false
        }
        
        return true
    }

    /**
     * Инициализация данных для OTA. 
     * Сохраняет байты прошивки и переводит состояние в WaitingForInit.
     */
    fun initUpdate(fileBytes: ByteArray) {
        firmwareData = fileBytes
        currentPacketIndex = 0
        _state.value = OtaState.WaitingForInit
        AppLogger.logInfo("OTA: Инициализация обновления, ожидание ota_init от БК", TAG)
    }

    /**
     * Обработка команды ota_init от БК.
     * Инициализирует параметры сессии и возвращает ПЕРВЫЙ пакет данных.
     * 
     * @param chunkSize Размер чанка, который готов принимать БК.
     * @param expectedCount Количество чанков, которое ожидает БК.
     * @return JSON-строка первого пакета ota_data или null.
     */
    fun onOtaInitReceived(chunkSize: Int, expectedCount: Int): String? {
        val data = firmwareData ?: return null
        currentChunkSize = chunkSize
        
        val totalSize = data.size
        // Рассчитываем общее кол-во пакетов
        totalChunks = (totalSize + chunkSize - 1) / chunkSize
        
        AppLogger.logInfo("OTA: Сессия начата. Чанк: $chunkSize, Всего пакетов: $totalChunks", TAG)
        
        // Возвращаем первый пакет для старта цепочки подтверждений
        return getOtaDataCommand(1)
    }

    /**
     * Формирует JSON команду для конкретного пакета данных.
     * 
     * @param packetNum Номер пакета (начиная с 1).
     * @return JSON строка с командой ota_data.
     */
    fun getOtaDataCommand(packetNum: Int): String? {
        val data = firmwareData ?: return null
        if (packetNum < 1 || packetNum > totalChunks) return null
        
        // Вычисляем смещение в массиве байтов
        val offset = (packetNum - 1) * currentChunkSize
        val actualChunkSize = minOf(currentChunkSize, data.size - offset)
        
        // Копируем нужный участок данных
        val chunk = ByteArray(actualChunkSize)
        System.arraycopy(data, offset, chunk, 0, actualChunkSize)
        
        // Кодируем в Base64 без переносов строк
        val base64Data = Base64.encodeToString(chunk, Base64.NO_WRAP)
        
        return "{\"command\":\"ota_data\", \"data\":{\"pack\":$packetNum, \"bin\":\"$base64Data\"}}"
    }

    /**
     * Формирует финальную команду завершения передачи.
     */
    fun getOtaEndCommand(): String {
        return "{\"command\":\"ota_end\"}"
    }

    /**
     * Обновить прогресс на основе подтверждения от БК.
     * Вызывается из AppController при получении ключа "ota_read".
     * 
     * @param confirmedPackets Номер последнего пакета, который БК успешно прочитал.
     */
    fun updateProgress(confirmedPackets: Int) {
        if (totalChunks > 0) {
            // Рассчитываем прогресс на основе реально полученных БК данных
            val progress = confirmedPackets.toFloat() / totalChunks.toFloat()
            _state.value = OtaState.Sending(progress, confirmedPackets, totalChunks)
            
            if (confirmedPackets >= totalChunks) {
                AppLogger.logInfo("OTA: Все пакеты подтверждены БК. Ожидание команды ota_restart для смены индикации.", TAG)
            }
        }
    }

    /**
     * Обработка подтверждения перезагрузки от БК.
     * Переводит диалог в режим ожидания физической перезагрузки (круговой индикатор).
     * Вызывается при получении {"ota_restart": 1} в ответ на ota_end.
     */
    fun onOtaRestartReceived() {
        _state.value = OtaState.WaitingForReboot
        AppLogger.logInfo("OTA: Получено подтверждение ota_restart. БК уходит в перезагрузку.", TAG)
        
        // ЗАПУСК ТАЙМАУТА: Если БК не пришлет новую версию в течение 15 секунд — считаем это ошибкой
        rebootTimeoutJob?.cancel()
        rebootTimeoutJob = otaScope.launch {
            delay(REBOOT_TIMEOUT_MS)
            if (_state.value is OtaState.WaitingForReboot) {
                AppLogger.logError("OTA: Превышено время ожидания перезагрузки БК (15 сек)", TAG)
                setError("Таймаут перезагрузки БК. Проверьте устройство.")
            }
        }
    }

    /**
     * Обработка сообщения об успешном обновлении после перезагрузки БК.
     */
    fun onUpdateSuccess(newVersion: String) {
        // Отменяем таймаут, так как БК успешно вышел на связь
        rebootTimeoutJob?.cancel()
        rebootTimeoutJob = null
        
        _state.value = OtaState.Success(newVersion)
        AppLogger.logInfo("OTA: Обновление успешно завершено. Новая версия: $newVersion", TAG)
        firmwareData = null
    }

    /**
     * Сброс состояния менеджера.
     */
    fun reset() {
        rebootTimeoutJob?.cancel()
        rebootTimeoutJob = null
        
        _state.value = OtaState.Idle
        firmwareData = null
        currentPacketIndex = 0
        totalChunks = 0
    }

    /**
     * Установка состояния ошибки.
     */
    fun setError(message: String) {
        rebootTimeoutJob?.cancel()
        rebootTimeoutJob = null

        _state.value = OtaState.Error(message)
        firmwareData = null
    }
}
