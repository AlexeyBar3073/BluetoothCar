// Файл: data/serializers/BooleanAsIntSerializer.kt
package com.alexbar3073.bluetoothcar.data.serializers

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * ТЕГ: Serializer/Boolean/AsInt
 * 
 * ФАЙЛ: data/serializers/BooleanAsIntSerializer.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: data/serializers/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Кастомный сериализатор для корректной обработки числовых булевых значений (0/1).
 * Позволяет десериализовать Int в Boolean и наоборот. 
 * Это критически важно для протоколов, оптимизирующих размер пакета.
 * 
 * ОТВЕТСТВЕННОСТЬ: Маппинг 0 -> false, любое другое число -> true.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Kotlinx Serialization Custom Serializer
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Устойчивость к входящим данным (0/1) при сохранении Boolean типа в моделях.
 */
object BooleanAsIntSerializer : KSerializer<Boolean> {
    /** Описание типа данных - целое число (Int) */
    override val descriptor: SerialDescriptor = 
        PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)

    /**
     * Превращает Boolean в Int при сериализации (отправке).
     * @param encoder Энкодер
     * @param value Значение
     */
    override fun serialize(encoder: Encoder, value: Boolean) {
        // Кодируем как 1 для true и 0 для false
        encoder.encodeInt(if (value) 1 else 0)
    }

    /**
     * Превращает Int в Boolean при десериализации (получении).
     * @param decoder Декодер
     * @return Boolean значение (true если не 0)
     */
    override fun deserialize(decoder: Decoder): Boolean {
        // Любое значение отличное от нуля трактуется как true
        return decoder.decodeInt() != 0
    }
}
