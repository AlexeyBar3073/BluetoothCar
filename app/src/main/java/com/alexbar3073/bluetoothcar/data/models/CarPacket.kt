// Файл: data/models/CarPacket.kt
package com.alexbar3073.bluetoothcar.data.models

import com.alexbar3073.bluetoothcar.data.serializers.BooleanAsIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * ТЕГ: CarPacket/Transport/Partial
 * 
 * ФАЙЛ: data/models/CarPacket.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: data/models/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Транспортная модель для десериализации входящих данных от БК.
 * Все поля являются необязательными (nullable). Если поле отсутствует в JSON, 
 * оно будет null, что позволяет реализовать логику частичного обновления (patching).
 * 
 * ОТВЕТСТВЕННОСТЬ: Прием и первичная типизация данных из Bluetooth-потока.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: DTO (Data Transfer Object).
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Поддержка инкрементальной телеметрии и 0/1 для Boolean.
 */
@Serializable
data class CarPacket(
    // --- ОСНОВНАЯ ТЕЛЕМЕТРИЯ ---
    @SerialName("spd") val speed: Float? = null,
    @SerialName("rpm") val rpm: Float? = null,
    @SerialName("gear") val gear: Float? = null,
    @SerialName("sel") val selector: String? = null,
    @SerialName("vlt") val voltage: Float? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("eng") val engineStatus: Boolean? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("tcc") val tccLocked: Boolean? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("hl") val headlightsOn: Boolean? = null,

    @SerialName("odo") val odometer: Float? = null,
    @SerialName("trip_a") val tripA: Float? = null,
    @SerialName("trip_b") val tripB: Float? = null,
    @SerialName("trip_cur") val tripCur: Float? = null,
    @SerialName("fuel") val fuel: Float? = null,
    @SerialName("fuel_a") val fuelA: Float? = null,
    @SerialName("fuel_b") val fuelB: Float? = null,
    @SerialName("fuel_cur") val fuelCur: Float? = null,
    @SerialName("inst_cons") val instantConsumption: Float? = null,
    @SerialName("avg_cons") val averageConsumption: Float? = null,
    @SerialName("t_cool") val coolantTemp: Float? = null,
    @SerialName("t_atf") val transmissionTemp: Float? = null,
    @SerialName("err") val ecuErrors: String? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("tire") val tirePressureLow: Boolean? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("wash") val washerFluidLow: Boolean? = null,

    // --- K-LINE ПОЛЯ (РЕАЛЬНЫЕ ДАННЫЕ С ЭБУ) ---
    @SerialName("k_rpm") val kRpm: Float? = null,
    @SerialName("k_spd") val kSpeed: Float? = null,
    
    /** Текущая передача с ЭБУ (1/2/3/4/5) */
    @SerialName("k_gear") val kGear: Int? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("k_eng") val kEngineStatus: Boolean? = null,

    @Serializable(with = BooleanAsIntSerializer::class)
    @SerialName("k_od_off") val kOdOff: Boolean? = null,

    @SerialName("k_err") val kEcuErrors: String? = null,
    @SerialName("k_fuel") val kFuel: Float? = null,
    @SerialName("k_tcool") val kCoolantTemp: Float? = null,
    @SerialName("k_tatf") val kTransmissionTemp: Float? = null,
    @SerialName("k_vlt") val kVoltage: Float? = null,
    @SerialName("k_thr") val kThrottle: Float? = null,
    @SerialName("k_dtc") val kDtcCount: Int? = null,
    @SerialName("k_shaft_rpm") val kShaftRpm: Float? = null,
    
    /** Позиция селектора с ЭБУ (P/R/N/D) */
    @SerialName("k_sel") val kSelector: String? = null
)
