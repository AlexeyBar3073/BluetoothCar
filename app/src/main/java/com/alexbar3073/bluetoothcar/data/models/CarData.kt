// Файл: data/models/CarData.kt
package com.alexbar3073.bluetoothcar.data.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Модель данных для информации от бортового компьютера автомобиля.
 * Аннотация @Immutable позволяет Compose оптимизировать рекомпозиции.
 */
@Immutable
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

    @SerialName("coolant_temp")
    val coolantTemp: Float = 0f,      // °C

    @SerialName("transmission_temp")
    val transmissionTemp: Float = 0f,      // °C

    @SerialName("ecu_errors")
    val ecuErrors: String = "",          // Коды ошибок ЭБУ, разделенные ";"

    @SerialName("tire_pressure_low")
    val tirePressureLow: Boolean = false, // Низкое давление в шинах

    @SerialName("washer_fluid_low")
    val washerFluidLow: Boolean = false,  // Низкий уровень омывайки

    @Transient
    val isFuelLow: Boolean = false,      // Рассчитываемый признак "мало топлива"

    val timestamp: Long = System.currentTimeMillis()
) {
    companion object {
        fun hasMeaningfulData(data: CarData): Boolean {
            return data.speed != 0f || data.voltage != 0f || data.fuel != 0f ||
                    data.tripA != 0f || data.tripB != 0f || data.odometer != 0f ||
                    data.coolantTemp != 0f || data.transmissionTemp != 0f ||
                    data.ecuErrors.isNotEmpty() || data.tirePressureLow || data.washerFluidLow
        }
    }
}
