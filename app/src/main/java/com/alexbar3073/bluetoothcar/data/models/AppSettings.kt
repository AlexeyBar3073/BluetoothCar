// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/data/models/AppSettings.kt
package com.alexbar3073.bluetoothcar.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

/**
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Единая модель настроек приложения. Является "источником истины" для конфигурации 
 * как UI-части, так и параметров работы с бортовым компьютером.
 *
 * СВЯЗЬ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Используется SettingsRepository для сохранения/загрузки через DataStore (в формате JSON).
 * 2. Считывается AppController для инициализации всей системы.
 * 3. Передается в BluetoothConnectionManager для настройки протокола обмена.
 */
@Serializable
data class AppSettings(
    // --- Bluetooth настройки ---
    @SerialName("selected_device")
    val selectedDevice: BluetoothDeviceData = BluetoothDeviceData.empty(),

    // --- Параметры топливной системы (для расчетов БК) ---
    @SerialName("fuel_tank_capacity")
    val fuelTankCapacity: Float = 60f,

    // Это поле сохраняется локально в приложении, но не отправляется на БК
    @SerialName("min_fuel_level")
    val minFuelLevel: Float = 5f,

    @SerialName("injector_performance")
    val injectorPerformance: Float = 250f,

    @SerialName("injector_count")
    val injectorCount: Int = 4,

    // --- Параметры датчиков ---
    @SerialName("speed_sensor_signals")
    val speedSensorSignalsPerMeter: Int = 3,

    // --- Настройки внешнего вида и поведения UI ---
    /** 
     * Выбранная тема оформления. 
     * Возможные значения: "dark", "light", "blue_dark". 
     * Значение "system" удалено для исключения неопределенности.
     */
    @SerialName("selected_theme")
    val selectedTheme: String = "dark",

    @SerialName("show_speedometer")
    val showSpeedometer: Boolean = true,

    @SerialName("show_fuel_gauge")
    val showFuelGauge: Boolean = true,

    @SerialName("show_voltage")
    val showVoltage: Boolean = true,

    @SerialName("update_interval")
    val updateInterval: Int = 1000,

    @SerialName("default_dashboard_color")
    val defaultDashboardColor: Long = 0xFFFC4903,

    @SerialName("current_dashboard_color")
    val currentDashboardColor: Long = 0xFFFC4903
) {
    /**
     * Преобразование технических настроек в Map для отправки на устройство.
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
     * Преобразование технических настроек в JSON строку для протокола БК.
     * min_fuel_level намеренно не включен, так как БК о нем знать не нужно.
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

    /**
     * Сливает входящие настройки от БК с текущими.
     * Сравнивает только те поля, которые относятся к БК.
     * @param remoteJson JSON объект с настройками от БК.
     * @return Новый экземпляр AppSettings если данные изменились, иначе текущий.
     */
    fun mergeWithRemote(remoteJson: JsonObject): AppSettings {
        // Извлекаем значения из JSON с проверкой типов, если ключа нет - оставляем текущее
        val newCapacity = remoteJson["fuel_tank_capacity"]?.jsonPrimitive?.floatOrNull ?: fuelTankCapacity
        val newInjCount = remoteJson["injector_count"]?.jsonPrimitive?.intOrNull ?: injectorCount
        val newInjPerf = remoteJson["injector_performance"]?.jsonPrimitive?.floatOrNull ?: injectorPerformance
        val newSpeedSignals = remoteJson["speed_sensor_signals"]?.jsonPrimitive?.intOrNull ?: speedSensorSignalsPerMeter

        // Проверяем, есть ли реальные изменения в технических полях
        val hasChanges = newCapacity != fuelTankCapacity ||
                newInjCount != injectorCount ||
                newInjPerf != injectorPerformance ||
                newSpeedSignals != speedSensorSignalsPerMeter

        return if (hasChanges) {
            this.copy(
                fuelTankCapacity = newCapacity,
                injectorCount = newInjCount,
                injectorPerformance = newInjPerf,
                speedSensorSignalsPerMeter = newSpeedSignals
            )
        } else {
            this
        }
    }
}
