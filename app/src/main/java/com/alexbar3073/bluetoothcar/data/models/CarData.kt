// Файл: data/models/CarData.kt
package com.alexbar3073.bluetoothcar.data.models

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * ТЕГ: CarData
 * 
 * ФАЙЛ: data/models/CarData.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: data/models/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Модель данных для информации от бортового компьютера автомобиля.
 * Аннотация @Immutable позволяет Compose оптимизировать рекомпозиции.
 * 
 * ОБНОВЛЕНИЕ ПРОТОКОЛА: Ключи оптимизированы для уменьшения размера пакета.
 * ПРИМЕЧАНИЕ: Внутри приложения все числовые данные хранятся как Float для 
 * плавности анимаций и удобства расчетов, независимо от типа в пакете (byte/ushort).
 */
@Immutable
@Serializable
data class CarData(
    // --- ОСНОВНАЯ ТЕЛЕМЕТРИЯ ---
    @SerialName("spd") val speed: Float = 0f,
    @SerialName("rpm") val rpm: Float = 0f,
    @SerialName("gear") val gear: Float = 0f,
    @SerialName("sel") val selector: String = "",
    @SerialName("vlt") val voltage: Float = 12.6f,
    @SerialName("eng") val engineStatus: Boolean = false,
    @SerialName("tcc") val tccLocked: Boolean = false,
    @SerialName("hl") val headlightsOn: Boolean = false,
    @SerialName("odo") val odometer: Float = 0f,
    @SerialName("trip_a") val tripA: Float = 0f,
    @SerialName("trip_b") val tripB: Float = 0f,
    @SerialName("trip_cur") val tripCur: Float = 0f,
    @SerialName("fuel") val fuel: Float = 0f,
    @SerialName("fuel_a") val fuelA: Float = 0f,
    @SerialName("fuel_b") val fuelB: Float = 0f,
    @SerialName("fuel_cur") val fuelCur: Float = 0f,
    @SerialName("inst") val instantConsumption: Float = 0f,
    @SerialName("avg") val averageConsumption: Float = 0f,
    @SerialName("avg_cur") val averageConsumptionCurrent: Float = 0f,
    @SerialName("t_cool") val coolantTemp: Float = 0f,
    @SerialName("t_atf") val transmissionTemp: Float = 0f,
    @SerialName("err") val ecuErrors: String = "",
    @SerialName("tire") val tirePressureLow: Boolean = false,
    @SerialName("wash") val washerFluidLow: Boolean = false,

    // --- K-LINE ПОЛЯ (РЕАЛЬНЫЕ ДАННЫЕ С ЭБУ) ---
    @SerialName("k_rpm") val kRpm: Float = 0f,                // Обороты с ЭБУ (об/мин)
    @SerialName("k_spd") val kSpeed: Float = 0f,              // Скорость с ЭБУ (км/ч)
    @SerialName("k_gear") val kGear: Int = 0,                 // Текущая передача с ЭБУ (1/2/3/4/5)
    @SerialName("k_eng") val kEngineStatus: Boolean = false,   // Двигатель работает (по данным ЭБУ)
    @SerialName("k_od_off") val kOdOff: Boolean = false,      // Блокировка ГДТ / O/D OFF (ЭБУ)
    @SerialName("k_err") val kEcuErrors: String = "",          // Коды ошибок из памяти ЭБУ
    @SerialName("k_fuel") val kFuel: Float = 0f,               // Уровень топлива с ЭБУ (%)
    @SerialName("k_tcool") val kCoolantTemp: Float = 0f,       // Температура ОЖ с ЭБУ (°C)
    @SerialName("k_tatf") val kTransmissionTemp: Float = 0f,    // Температура ATF с ЭБУ (°C)
    @SerialName("k_vlt") val kVoltage: Float = 0f,             // Напряжение с ЭБУ (В)
    @SerialName("k_thr") val kThrottle: Float = 0f,            // Положение дросселя с ЭБУ (%)
    @SerialName("k_dtc") val kDtcCount: Int = 0,               // Количество активных ошибок ЭБУ
    @SerialName("k_shaft_rpm") val kShaftRpm: Float = 0f,      // Обороты выходного вала АКПП
    @SerialName("k_sel") val kSelector: String = "",           // Позиция селектора с ЭБУ (P/R/N/D)

    @Transient val isFuelLow: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Выполняет частичное обновление данных на основе входящего транспортного пакета.
     * 
     * ПРИНЦИП РАБОТЫ:
     * Метод проверяет каждое поле в пакете. Если значение присутствует (не null), 
     * оно перезаписывает текущее значение. Если поле в пакете отсутствует (null), 
     * сохраняется старое значение.
     *
     * @param packet Входящий транспортный пакет.
     * @return Обновленный экземпляр CarData.
     */
    fun updateWith(packet: CarPacket): CarData {
        return this.copy(
            speed = packet.speed ?: this.speed,
            rpm = packet.rpm ?: this.rpm,
            gear = packet.gear ?: this.gear,
            selector = packet.selector ?: this.selector,
            voltage = packet.voltage ?: this.voltage,
            engineStatus = packet.engineStatus ?: this.engineStatus,
            tccLocked = packet.tccLocked ?: this.tccLocked,
            headlightsOn = packet.headlightsOn ?: this.headlightsOn,
            odometer = packet.odometer ?: this.odometer,
            tripA = packet.tripA ?: this.tripA,
            tripB = packet.tripB ?: this.tripB,
            tripCur = packet.tripCur ?: this.tripCur,
            fuel = packet.fuel ?: this.fuel,
            fuelA = packet.fuelA ?: this.fuelA,
            fuelB = packet.fuelB ?: this.fuelB,
            fuelCur = packet.fuelCur ?: this.fuelCur,
            instantConsumption = packet.instantConsumption ?: this.instantConsumption,
            averageConsumption = packet.averageConsumption ?: this.averageConsumption,
            averageConsumptionCurrent = packet.averageConsumptionCurrent ?: this.averageConsumptionCurrent,
            coolantTemp = packet.coolantTemp ?: this.coolantTemp,
            transmissionTemp = packet.transmissionTemp ?: this.transmissionTemp,
            ecuErrors = packet.ecuErrors ?: this.ecuErrors,
            tirePressureLow = packet.tirePressureLow ?: this.tirePressureLow,
            washerFluidLow = packet.washerFluidLow ?: this.washerFluidLow,
            kRpm = packet.kRpm ?: this.kRpm,
            kSpeed = packet.kSpeed ?: this.kSpeed,
            kGear = packet.kGear ?: this.kGear,
            kEngineStatus = packet.kEngineStatus ?: this.kEngineStatus,
            kOdOff = packet.kOdOff ?: this.kOdOff,
            kEcuErrors = packet.kEcuErrors ?: this.kEcuErrors,
            kFuel = packet.kFuel ?: this.kFuel,
            kCoolantTemp = packet.kCoolantTemp ?: this.kCoolantTemp,
            kTransmissionTemp = packet.kTransmissionTemp ?: this.kTransmissionTemp,
            kVoltage = packet.kVoltage ?: this.kVoltage,
            kThrottle = packet.kThrottle ?: this.kThrottle,
            kDtcCount = packet.kDtcCount ?: this.kDtcCount,
            kShaftRpm = packet.kShaftRpm ?: this.kShaftRpm,
            kSelector = packet.kSelector ?: this.kSelector,
            timestamp = System.currentTimeMillis()
        )
    }

    /** Поле для совместимости со старыми виджетами. */
    @Transient val fuelConsumption: Float get() = averageConsumption

    /** Поле для совместимости со старыми виджетами. */
    @Transient val remainingRange: Float = 0f

    companion object {
        /** Проверяет, содержит ли объект значимые данные. */
        fun hasMeaningfulData(data: CarData): Boolean {
            return data.speed != 0f || data.voltage != 0f || data.fuel != 0f ||
                    data.tripA != 0f || data.tripB != 0f || data.odometer != 0f ||
                    data.coolantTemp != 0f || data.transmissionTemp != 0f ||
                    data.ecuErrors.isNotEmpty() || data.tirePressureLow || data.washerFluidLow ||
                    data.rpm != 0f || data.gear != 0f
        }
    }
}
