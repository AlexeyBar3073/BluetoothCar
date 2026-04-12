// Файл: data/bluetooth/OtaManager.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import android.util.Base64
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
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
        object Idle : OtaState()
        object Validating : OtaState()
        data class Sending(val progress: Float, val currentPacket: Int, val totalPackets: Int) : OtaState()
        object Success : OtaState()
        data class Error(val message: String) : OtaState()
    }

    /** Текущее состояние процесса для UI */
    private val _state = MutableStateFlow<OtaState>(OtaState.Idle)
    val state: StateFlow<OtaState> = _state.asStateFlow()

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
     * Подготовка списка JSON-команд для передачи прошивки.
     * 
     * @param fileBytes Содержимое файла прошивки.
     * @return Список строк JSON готовых к отправке через DataStreamHandler.
     */
    fun prepareOtaCommands(fileBytes: ByteArray): List<String> {
        val commands = mutableListOf<String>()
        val totalSize = fileBytes.size
        
        // 1. Команда начала OTA
        commands.add("{\"command\":\"ota_start\", \"data\":{\"size\":$totalSize}}")
        
        // 2. Нарезка на чанки и формирование ota_data
        var packetNumber = 1
        var offset = 0
        
        while (offset < totalSize) {
            // Рассчитываем размер текущего чанка (последний может быть меньше 512)
            val currentChunkSize = minOf(CHUNK_SIZE, totalSize - offset)
            val chunk = ByteArray(currentChunkSize)
            System.arraycopy(fileBytes, offset, chunk, 0, currentChunkSize)
            
            // Кодируем в Base64 (NO_WRAP убирает переносы строк)
            val base64Data = Base64.encodeToString(chunk, Base64.NO_WRAP)
            
            // Формируем команду чанка
            commands.add("{\"command\":\"ota_data\", \"data\":{\"pack\":$packetNumber, \"bin\":\"$base64Data\"}}")
            
            offset += currentChunkSize
            packetNumber++
        }
        
        // 3. Команда завершения OTA
        commands.add("{\"command\":\"ota_end\"}")
        
        AppLogger.logInfo("OTA: Подготовлено ${commands.size} пакетов для прошивки размером $totalSize байт", TAG)
        return commands
    }

    /**
     * Обновить текущее состояние прогресса на основе подтверждения от БК.
     * Вызывается при получении пакета с полем ota_read.
     * 
     * @param confirmedPacket Номер пакета, который БК успешно прочитал и подтвердил.
     * @param total Общее количество пакетов в текущей сессии обновления.
     */
    fun updateProgress(confirmedPacket: Int, total: Int) {
        // Логика определения завершения: если подтвержден последний пакет
        if (confirmedPacket >= total && total > 0) {
            // Устанавливаем состояние успеха
            _state.value = OtaState.Success
            AppLogger.logInfo("OTA: Получено подтверждение финального пакета ($confirmedPacket/$total). Успех.", TAG)
        } else if (total > 0) {
            // Рассчитываем процент прогресса (от 0.0 до 1.0)
            val progress = confirmedPacket.toFloat() / total.toFloat()
            // Обновляем состояние для UI
            _state.value = OtaState.Sending(progress, confirmedPacket, total)
        }
    }

    /**
     * Сброс состояния менеджера.
     */
    fun reset() {
        _state.value = OtaState.Idle
    }

    /**
     * Установка состояния ошибки.
     */
    fun setError(message: String) {
        _state.value = OtaState.Error(message)
    }
}
