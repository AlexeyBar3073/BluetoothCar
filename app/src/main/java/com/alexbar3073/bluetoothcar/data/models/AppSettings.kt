// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/data/models/AppSettings.kt
package com.alexbar3073.bluetoothcar.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Единая модель настроек приложения. Является "источником истины" для конфигурации 
 * как UI-части, так и параметров работы с бортовым компьютером.
 *
 * СВЯЗЬ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Используется SettingsRepository для сохранения/загрузки через DataStore.
 * 2. Считывается AppController для инициализации всей системы.
 * 3. Передается в BluetoothConnectionManager для настройки протокола обмена.
 * 4. Наблюдается в SharedViewModel для отображения состояния в UI.
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.04 13:30 UTC: Удалены методы логики, класс приведен к состоянию "чистой модели".
 * - 2025.02.04 14:35 UTC: Файл приведен к стандарту оформления AI_RULES.md (путь в 1 строке, описание после импортов).
 */
@Serializable
data class AppSettings(
    // --- Bluetooth настройки ---
    @SerialName("selected_device")
    val selectedDevice: BluetoothDeviceData? = null,

    // --- Параметры топливной системы (для расчетов БК) ---
    @SerialName("fuel_tank_capacity")
    val fuelTankCapacity: Float = 60f,

    @SerialName("injector_performance")
    val injectorPerformance: Float = 250f,

    @SerialName("injector_count")
    val injectorCount: Int = 4,

    // --- Параметры датчиков ---
    @SerialName("speed_sensor_signals")
    val speedSensorSignalsPerMeter: Int = 3,

    // --- Настройки внешнего вида и поведения UI ---
    @SerialName("selected_theme")
    val selectedTheme: String = "system",

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
     * Преобразование технических настроек в Map для отправки на устройство.
     * Используется DataStreamHandler при реализации протокола обмена.
     */
    fun toDeviceSettingsMap(): Map<String, Any> {
        return mapOf(
            "fuel_tank_capacity" to fuelTankCapacity.toDouble(),
            "injector_count" to injectorCount,
            "injector_performance" to injectorPerformance.toDouble(),
            "speed_sensor_signals" to speedSensorSignalsPerMeter
        )
    }

    /**
     * Преобразование технических настроек в JSON строку.
     * Используется для логирования и отладки протокола.
     */
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
