// Файл: data/models/AppSettings.kt
package com.alexbar3073.bluetoothcar.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * ФАЙЛ: AppSettings.kt
 * МЕСТОНАХОЖДЕНИЕ: data/models/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Единая модель настроек приложения согласно ТЗ.
 * ТОЛЬКО ДАННЫЕ, логика в соответствующих классах.
 *
 * КЛЮЧЕВЫЕ ПРИНЦИПЫ:
 * 1. Чистая модель данных (без бизнес-логики)
 * 2. ConnectionFeasibilityChecker проверяет возможность подключения
 * 3. DataStreamHandler использует toDeviceSettingsMap()
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.04 13:30 UTC: ИСПРАВЛЕНИЕ ОШИБКИ
 *   1. Удалены ВСЕ методы проверки (логика в ConnectionFeasibilityChecker)
 *   2. Оставлен только toDeviceSettingsMap() для DataStreamHandler
 *   3. Класс теперь - чистая модель данных
 */
@Serializable
data class AppSettings(
    // Bluetooth настройки
    @SerialName("selected_device")
    val selectedDevice: BluetoothDeviceData? = null,

    // Топливная система (для расчета расхода)
    @SerialName("fuel_tank_capacity")
    val fuelTankCapacity: Float = 60f,

    @SerialName("injector_performance")
    val injectorPerformance: Float = 250f,

    @SerialName("injector_count")
    val injectorCount: Int = 4,

    // Датчики (для калибровки)
    @SerialName("speed_sensor_signals")
    val speedSensorSignalsPerMeter: Int = 3,

    // Внешний вид (только для приложение)
    @SerialName("selected_theme")
    val selectedTheme: String = "system",

    // Дополнительные настройки (только для приложения)
    @SerialName("show_speedometer")
    val showSpeedometer: Boolean = true,

    @SerialName("show_fuel_gauge")
    val showFuelGauge: Boolean = true,

    @SerialName("show_voltage")
    val showVoltage: Boolean = true,

    @SerialName("update_interval")
    val updateInterval: Int = 1000
) {
    /**
     * Конвертировать в настройки для устройства.
     * DataStreamHandler отправляет на БК (технические настройки).
     * Устройство НЕ отправляется на БК.
     *
     * ИЗ ТЗ: "отправка настроек приложения на БК"
     * ЕДИНСТВЕННЫЙ необходимый метод.
     */
    fun toDeviceSettingsMap(): Map<String, Any> {
        return mapOf(
            "fuel_tank_capacity" to fuelTankCapacity.toDouble(),
            "injector_count" to injectorCount,
            "injector_performance" to injectorPerformance.toDouble(),
            "speed_sensor_signals" to speedSensorSignalsPerMeter
        )
    }

    fun toDeviceSettingsJson(): String {
        val jsonObject = buildJsonObject {
            put("fuel_tank_capacity", fuelTankCapacity.toDouble())
            put("injector_count", injectorCount)
            put("injector_performance", injectorPerformance.toDouble())
            put("speed_sensor_signals", speedSensorSignalsPerMeter)
        }
        return jsonObject.toString()
    }
}