// Файл: data/models/AppSettings.kt
package com.alexbar3073.bluetoothcar.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.*

/**
 * ТЕГ: AppSettings
 * 
 * ФАЙЛ: data/models/AppSettings.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: data/models/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Единая модель настроек приложения. Является "источником истины" для конфигурации 
 * как UI-части, так и параметров работы с бортовым компьютером.
 * 
 * ОБНОВЛЕНИЕ ПРОТОКОЛА: Ключи настроек переименованы (cfg), добавлена версия прошивки.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Используется SettingsRepository для сохранения/загрузки через DataStore (в формате JSON).
 * 2. Считывается AppController для инициализации всей системы.
 * 3. Передается в BluetoothConnectionManager для настройки протокола обмена.
 */
@Serializable
data class AppSettings(
    // --- Bluetooth настройки ---
    @SerialName("selected_device")
    val selectedDevice: BluetoothDeviceData = BluetoothDeviceData.empty(),

    // --- Параметры бортового компьютера (Синхронизация с ключом cfg) ---
    @SerialName("tank")
    val fuelTankCapacity: Float = 60f,

    @SerialName("inj_perf")
    val injectorPerformance: Float = 250f,

    @SerialName("inj_cnt")
    val injectorCount: Int = 4,

    @SerialName("spd_sig")
    val speedSensorSignalsPerMeter: Int = 3,

    @SerialName("fw")
    val firmwareVersion: String = "v1.0", // Версия прошивки БК

    // --- Локальные настройки приложения (Не отправляются на БК) ---
    @SerialName("min_fuel_level")
    val minFuelLevel: Float = 5f,

    /** 
     * Выбранная тема оформления. 
     * Возможные значения: "dark", "light", "blue_dark". 
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
     * Преобразование технических настроек в JSON строку для протокола БК (ключ cfg).
     * Версия прошивки (fw) НЕ отправляется обратно на БК.
     */
    fun toDeviceSettingsJson(): String {
        val jsonObject = buildJsonObject {
            put("tank", fuelTankCapacity.toDouble())
            put("inj_perf", injectorPerformance.toDouble())
            put("inj_cnt", injectorCount)
            put("spd_sig", speedSensorSignalsPerMeter)
        }
        return jsonObject.toString()
    }

    /**
     * Сливает входящие настройки от БК с текущими.
     * Обрабатывает переименованные ключи согласно новому протоколу.
     * 
     * @param remoteJson JSON объект с настройками от БК (ключ cfg).
     * @return Новый экземпляр AppSettings если данные изменились, иначе текущий.
     */
    fun mergeWithRemote(remoteJson: JsonObject): AppSettings {
        // Извлекаем значения из JSON с проверкой типов
        val newCapacity = remoteJson["tank"]?.jsonPrimitive?.floatOrNull ?: fuelTankCapacity
        val newInjPerf = remoteJson["inj_perf"]?.jsonPrimitive?.floatOrNull ?: injectorPerformance
        val newInjCount = remoteJson["inj_cnt"]?.jsonPrimitive?.intOrNull ?: injectorCount
        val newSpeedSignals = remoteJson["spd_sig"]?.jsonPrimitive?.intOrNull ?: speedSensorSignalsPerMeter
        val newFirmware = remoteJson["fw"]?.jsonPrimitive?.content ?: firmwareVersion

        // Проверяем, есть ли реальные изменения
        val hasChanges = newCapacity != fuelTankCapacity ||
                newInjPerf != injectorPerformance ||
                newInjCount != injectorCount ||
                newSpeedSignals != speedSensorSignalsPerMeter ||
                newFirmware != firmwareVersion

        return if (hasChanges) {
            this.copy(
                fuelTankCapacity = newCapacity,
                injectorPerformance = newInjPerf,
                injectorCount = newInjCount,
                speedSensorSignalsPerMeter = newSpeedSignals,
                firmwareVersion = newFirmware
            )
        } else {
            this
        }
    }
}
