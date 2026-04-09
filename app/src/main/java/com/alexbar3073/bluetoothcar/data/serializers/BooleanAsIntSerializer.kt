// Файл: data/serializers/BooleanAsIntSerializer.kt
package com.alexbar3073.bluetoothcar.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull

/**
 * ТЕГ: Serializer/Boolean/AsInt
 * 
 * ФАЙЛ: data/serializers/BooleanAsIntSerializer.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: data/serializers/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Кастомный универсальный сериализатор для булевых значений.
 * Поддерживает десериализацию из:
 * 1. Чисел (0 -> false, !0 -> true)
 * 2. Булевых значений (true/false)
 * 3. Строк ("0", "1", "true", "false")
 * 
 * Это критически важно для протоколов, оптимизирующих размер пакета, где булево
 * передается как бит или байт (0/1).
 * 
 * ОТВЕТСТВЕННОСТЬ: Маппинг различных входящих типов в Boolean.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Kotlinx Serialization Custom Serializer
 */
object BooleanAsIntSerializer : KSerializer<Boolean> {
    /** Описание типа данных - целое число (Int) для совместимости */
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)

    /**
     * Превращает Boolean в Int при сериализации (отправке).
     * @param encoder Энкодер
     * @param value Значение
     */
    override fun serialize(encoder: Encoder, value: Boolean) {
        // Всегда отправляем как число для экономии трафика
        encoder.encodeInt(if (value) 1 else 0)
    }

    /**
     * Гибкая десериализация: пробует прочитать как Boolean, если не выходит — как Int.
     * @param decoder Декодер
     * @return Boolean значение
     */
    override fun deserialize(decoder: Decoder): Boolean {
        // Пытаемся использовать JsonDecoder для анализа типа элемента
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val element = jsonDecoder.decodeJsonElement()
            if (element is JsonPrimitive) {
                // 1. Пробуем как булево (true/false)
                element.booleanOrNull?.let { return it }
                // 2. Пробуем как число (0/1)
                element.intOrNull?.let { return it != 0 }
                // 3. Пробуем как строку
                return element.content.lowercase() == "true" || element.content == "1"
            }
        }
        
        // Fallback для не-JSON декодеров или если что-то пошло не так
        return try {
            decoder.decodeInt() != 0
        } catch (e: Exception) {
            try {
                decoder.decodeBoolean()
            } catch (e2: Exception) {
                false
            }
        }
    }
}
