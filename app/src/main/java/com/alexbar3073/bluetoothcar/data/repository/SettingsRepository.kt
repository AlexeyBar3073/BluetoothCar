// Файл: data/repository/SettingsRepository.kt
package com.alexbar3073.bluetoothcar.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * ФАЙЛ: data/repository/SettingsRepository.kt
 * МЕСТОНАХОЖДЕНИЕ: data/repository/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Репозиторий для работы с настройками приложения через DataStore.
 * Хранит все настройки в виде единого JSON объекта для простоты управления.
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.04 13:45 UTC: ОБНОВЛЕНИЕ ДЛЯ BluetoothDeviceData
 *   1. Исправлено логирование (используются методы BluetoothDeviceData)
 *   2. Добавлен импорт AppSettings (BluetoothDeviceData уже внутри)
 *   3. Улучшена читаемость кода логирования
 * - 2026.02.04 15:00 UTC: ПРИВЕДЕНИЕ selectedDevice К НЕ-NULL ТИПУ
 *   1. Удалены проверки на null для selectedDevice
 *   2. Использование isValidDevice() для определения наличия выбора
 */
class SettingsRepository(private val context: Context) {

    private val TAG = "SettingsRepository"

    // JSON сериализатор с настройками по умолчанию
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    companion object {
        private val APPSETTINGS_JSON = stringPreferencesKey("app_settings_json")
        private val DEFAULT_SETTINGS = AppSettings()
    }

    /**
     * Поток настроек приложения.
     */
    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            AppLogger.logError("Ошибка чтения настроек из DataStore: $exception", TAG)
            emit(emptyPreferences())
        }
        .map { preferences ->
            val jsonString = preferences[APPSETTINGS_JSON]
            parseAppSettingsFromJson(jsonString)
        }
        .flowOn(Dispatchers.IO)

    /**
     * Получить текущие настройки приложения.
     */
    suspend fun getCurrentSettings(): AppSettings {
        return withContext(Dispatchers.IO) {
            try {
                val preferences = context.dataStore.data.first()
                val jsonString = preferences[APPSETTINGS_JSON]
                parseAppSettingsFromJson(jsonString)
            } catch (e: Exception) {
                AppLogger.logError("Ошибка получения настроек: ${e.message}", TAG)
                DEFAULT_SETTINGS
            }
        }
    }

    /**
     * Сохранить все настройки приложения.
     */
    suspend fun saveSettings(settings: AppSettings) {
        withContext(Dispatchers.IO) {
            try {
                AppLogger.logInfo("Сохранение настроек в формате JSON", TAG)

                val jsonString = json.encodeToString(settings)

                context.dataStore.edit { preferences ->
                    preferences[APPSETTINGS_JSON] = jsonString
                }

                AppLogger.logInfo("Настройки сохранены успешно (${jsonString.length} символов)", TAG)
                logAppSettings(settings)

            } catch (e: SerializationException) {
                AppLogger.logError("Ошибка сериализации настроек: ${e.message}", TAG)
                throw e
            } catch (e: Exception) {
                AppLogger.logError("Ошибка сохранения настроек: ${e.message}", TAG)
                throw e
            }
        }
    }

    /**
     * Удалить все настройки (для отладки).
     */
    suspend fun clearAllSettings() {
        withContext(Dispatchers.IO) {
            AppLogger.logInfo("Очистка всех настроек", TAG)
            context.dataStore.edit { preferences ->
                preferences.clear()
            }
            AppLogger.logInfo("Все настройки очищены", TAG)
        }
    }

    /**
     * Распечатать все настройки в лог (для отладки).
     */
    suspend fun debugPrintAllSettings() {
        withContext(Dispatchers.IO) {
            try {
                val settings = getCurrentSettings()
                AppLogger.logInfo("=== ТЕКУЩИЕ НАСТРОЙКИ ПРИЛОЖЕНИЯ ===", TAG)
                logAppSettings(settings)

                val jsonString = json.encodeToString(settings)
                AppLogger.logInfo("JSON представление (${jsonString.length} символов): $jsonString", TAG)

            } catch (e: Exception) {
                AppLogger.logError("Ошибка при выводе настроек: ${e.message}", TAG)
            }
        }
    }

    /**
     * Распечатать настройки в удобочитаемом формате.
     */
    private fun logAppSettings(settings: AppSettings) {
        // Свойство selectedDevice теперь гарантированно не null
        val device = settings.selectedDevice
        val deviceDisplayName = if (device.isValidDevice()) device.getDisplayName() else "не выбрано"
        val deviceAddress = device.getDeviceAddress()
        val deviceShortAddress = device.getShortAddress()

        AppLogger.logInfo("Bluetooth устройство: $deviceDisplayName", TAG)

        if (device.isValidDevice()) {
            AppLogger.logInfo("Адрес устройства: $deviceAddress", TAG)
            if (deviceShortAddress.isNotBlank() && deviceShortAddress != deviceAddress) {
                AppLogger.logInfo("Короткий адрес: $deviceShortAddress", TAG)
            }
        }

        AppLogger.logInfo("Топливная система:", TAG)
        AppLogger.logInfo("  Объем бака: ${settings.fuelTankCapacity} л", TAG)
        AppLogger.logInfo("  Форсунки: ${settings.injectorCount} шт по ${settings.injectorPerformance} мл/мин", TAG)

        AppLogger.logInfo("Датчики:", TAG)
        AppLogger.logInfo("  Сигналов/метр: ${settings.speedSensorSignalsPerMeter}", TAG)

        AppLogger.logInfo("Внешний вид:", TAG)
        AppLogger.logInfo("  Тема: ${settings.selectedTheme}", TAG)
        AppLogger.logInfo("  Виджеты: скорость=${settings.showSpeedometer}, топливо=${settings.showFuelGauge}, напряжение=${settings.showVoltage}", TAG)

        AppLogger.logInfo("Интервал обновления: ${settings.updateInterval} мс", TAG)
    }

    /**
     * Парсить AppSettings из JSON строки.
     */
    private fun parseAppSettingsFromJson(jsonString: String?): AppSettings {
        return try {
            when {
                jsonString == null || jsonString.isBlank() -> {
                    AppLogger.logInfo("JSON строки нет, возвращаем дефолтные настройки", TAG)
                    DEFAULT_SETTINGS
                }
                else -> {
                    json.decodeFromString<AppSettings>(jsonString)
                }
            }
        } catch (e: SerializationException) {
            AppLogger.logError("Ошибка десериализации JSON: ${e.message}", TAG)
            AppLogger.logError("Проблемный JSON: $jsonString", TAG)
            DEFAULT_SETTINGS
        } catch (e: Exception) {
            AppLogger.logError("Неожиданная ошибка десериализации: ${e.message}", TAG)
            DEFAULT_SETTINGS
        }
    }
}
