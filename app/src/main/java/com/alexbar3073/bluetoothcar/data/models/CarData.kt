// Файл: data/models/CarData.kt
package com.alexbar3073.bluetoothcar.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ФАЙЛ: CarData.kt
 * МЕСТОНАХОЖДЕНИЕ: data/models/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модель данных для информации от бортового компьютера автомобиля.
 * Использует Kotlin Serialization для автоматического парсинга JSON.
 *
 * ИЗМЕНЕНИЯ:
 * - 2026.02.02: Добавлена поддержка Kotlin Serialization
 * - Аннотации @Serializable и @SerialName для автоматического парсинга
 */
@Serializable
data class CarData(
    @SerialName("speed")
    val speed: Float = 0f,               // км/ч

    @SerialName("voltage")
    val voltage: Float = 12.6f,             // Вольт

    @SerialName("trip_a")
    val tripA: Float = 0f,               // км

    @SerialName("trip_b")
    val tripB: Float = 0f,               // км

    @SerialName("odometer")
    val odometer: Float = 0f,            // км

    @SerialName("fuel")
    val fuel: Float = 0f,                // Литры

    @SerialName("fuel_consumption")
    val fuelConsumption: Float = 0f,     // л/100км

    @SerialName("remaining_range")
    val remainingRange: Float = 0f,      // км

    @SerialName("rpm")
    val rpm: Float = 0f,      // об/мин

    // Это поле НЕ из JSON, поэтому не помечаем @SerialName
    // Оно не будет сериализоваться/десериализоваться автоматически
    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Проверить, содержит ли CarData значимые данные
         * (не все значения по умолчанию).
         */
        fun hasMeaningfulData(data: CarData): Boolean {
            return data.speed != 0f || data.voltage != 0f || data.fuel != 0f ||
                    data.tripA != 0f || data.tripB != 0f || data.odometer != 0f
        }
    }
}