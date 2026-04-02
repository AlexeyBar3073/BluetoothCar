// Файл: data/database/converters/EcuErrorTypeConverters.kt
package com.alexbar3073.bluetoothcar.data.database.converters

import androidx.room.TypeConverter
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorCause
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * ТЕГ: Database/Converters/EcuError
 *
 * ФАЙЛ: data/database/converters/EcuErrorTypeConverters.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: data/database/converters/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Конвертер типов для Room. Позволяет сохранять сложные объекты (списки, вложенные классы)
 * в базу данных в виде JSON-строк и восстанавливать их обратно.
 *
 * ОТВЕТСТВЕННОСТЬ: Сериализация и десериализация полей EcuErrorEntity.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Room TypeConverters
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Использование существующей библиотеки kotlinx.serialization.
 */
class EcuErrorTypeConverters {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Преобразовать список строк в JSON строку для хранения в БД.
     */
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        // Сериализуем список в строку
        return json.encodeToString(value)
    }

    /**
     * Восстановить список строк из JSON строки из БД.
     */
    @TypeConverter
    fun toStringList(value: String): List<String> {
        // Десериализуем строку обратно в список
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Преобразовать список причин (EcuErrorCause) в JSON строку.
     */
    @TypeConverter
    fun fromCauseList(value: List<EcuErrorCause>): String {
        // Сериализуем список объектов в строку
        return json.encodeToString(value)
    }

    /**
     * Восстановить список причин из JSON строки.
     */
    @TypeConverter
    fun toCauseList(value: String): List<EcuErrorCause> {
        // Десериализуем строку обратно в список объектов
        return try {
            json.decodeFromString(value)
        } catch (e: Exception) {
            emptyList()
        }
    }
}
