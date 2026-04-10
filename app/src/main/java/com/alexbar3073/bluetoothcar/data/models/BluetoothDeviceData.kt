// Файл: data/models/BluetoothDeviceData.kt
package com.alexbar3073.bluetoothcar.data.models

import android.Manifest
import androidx.annotation.RequiresPermission
import kotlinx.serialization.Serializable

/**
 * ФАЙЛ: data/models/BluetoothDeviceData.kt (ПЕРЕИМЕНОВАН!)
 * МЕСТОНАХОЖДЕНИЕ: data/models/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Сериализуемая модель Bluetooth устройства для хранения в настройках приложения.
 * Используется ВЕЗДЕ в приложении вместо системного Android BluetoothDevice.
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.04 12:30 UTC: ПЕРЕИМЕНОВАНИЕ ДЛЯ ЯСНОСТИ
 *   1. Класс переименован: BluetoothDevice → BluetoothDeviceData
 *   2. Файл переименован: BluetoothDevice.kt → BluetoothDeviceData.kt
 *   3. Добавлены методы-геттеры для consistency с кодом
 *   4. Убрана путаница с системным BluetoothDevice
 */

@Serializable
data class BluetoothDeviceData(
    val name: String = "",
    val address: String = "",
    val isPaired: Boolean = false,
    val bondState: Int = 10, // Default to BOND_NONE (10)
    val deviceClass: Int = 0, // Bluetooth class code
    val majorDeviceClass: Int = 0 // Major device class
) {
    /**
     * Константы для Bluetooth классов устройств и состояний сопряжения.
     * Определяют основные и второстепенные классы Bluetooth устройств.
     */
    companion object {
        // Состояния сопряжения (Bond States)
        const val BOND_NONE = 10
        const val BOND_BONDING = 11
        const val BOND_BONDED = 12

        // Основные классы устройств (Major Device Classes)
        const val MAJOR_AUDIO_VIDEO = 0x0400
        const val MAJOR_COMPUTER = 0x0100
        const val MAJOR_PHONE = 0x0200
        const val MAJOR_PERIPHERAL = 0x0500

        // Второстепенные классы устройств для AUDIO_VIDEO (Minor Device Classes)
        const val AUDIO_VIDEO_HANDSFREE = 0x0002
        const val AUDIO_VIDEO_CAR_AUDIO = 0x0007
        const val AUDIO_VIDEO_WEARABLE_HEADSET = 0x0001
        const val AUDIO_VIDEO_HEADPHONES = 0x0005

        /**
         * Создает экземпляр BluetoothDeviceData из Android BluetoothDevice.
         * Обрабатывает исключения безопасности при получении имени устройства.
         *
         * @param device Нативное Android Bluetooth устройство
         * @return Доменный объект BluetoothDeviceData
         * @throws SecurityException если нет разрешения BLUETOOTH_CONNECT
         */
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        fun fromAndroidDevice(device: android.bluetooth.BluetoothDevice): BluetoothDeviceData {
            val deviceName = try {
                device.name ?: "Неизвестное устройство"
            } catch (e: SecurityException) {
                "Устройство (требуется разрешение)"
            }

            val bluetoothClass = device.bluetoothClass
            val bondState = device.bondState
            
            return BluetoothDeviceData(
                name = deviceName,
                address = device.address,
                isPaired = bondState == android.bluetooth.BluetoothDevice.BOND_BONDED,
                bondState = bondState,
                deviceClass = bluetoothClass?.deviceClass ?: 0,
                majorDeviceClass = bluetoothClass?.majorDeviceClass ?: 0
            )
        }

        /**
         * Создает пустой BluetoothDeviceData (для десериализации и тестов).
         */
        fun empty(): BluetoothDeviceData {
            return BluetoothDeviceData(
                name = "",
                address = "",
                isPaired = false,
                deviceClass = 0,
                majorDeviceClass = 0
            )
        }

        /**
         * Проверяет, является ли BluetoothDeviceData валидным (имеет адрес).
         */
        fun isValid(device: BluetoothDeviceData?): Boolean {
            return device != null && device.address.isNotBlank()
        }
    }

    // ========== ГЕТТЕРЫ ДЛЯ УДОБСТВА (ДОБАВЛЕНЫ) ==========

    /**
     * Получить имя устройства.
     * Алиас для поля name, но с более понятным названием.
     */
    fun getDeviceName(): String = name

    /**
     * Получить адрес устройства (MAC-адрес).
     * Алиас для поля address, но с более понятным названием.
     */
    fun getDeviceAddress(): String = address

    /**
     * Получить отображаемое имя устройства.
     * Если имя пустое, возвращает адрес.
     */
    fun getDisplayName(): String = if (name.isNotBlank()) name else address

    /**
     * Получить короткий адрес устройства (последние 8 символов).
     * Используется для компактного отображения.
     */
    fun getShortAddress(): String = if (address.length >= 8) address.takeLast(8) else address

    /**
     * Проверить, валидно ли устройство (имеет адрес).
     */
    fun isValidDevice(): Boolean = address.isNotBlank()

    /**
     * Определяет тип устройства на основе класса Bluetooth.
     * Используется для отображения соответствующих иконок и информации.
     *
     * @return DeviceType соответствующий классу устройства
     */
    val deviceType: DeviceType
        get() {
            val majorClass = deviceClass and 0x1F00
            val minorClass = deviceClass and 0x00FF

            return when (majorClass) {
                MAJOR_AUDIO_VIDEO -> {
                    when (minorClass) {
                        AUDIO_VIDEO_HANDSFREE -> DeviceType.HEADSET
                        AUDIO_VIDEO_CAR_AUDIO -> DeviceType.CAR_AUDIO
                        AUDIO_VIDEO_WEARABLE_HEADSET -> DeviceType.HEADPHONES
                        AUDIO_VIDEO_HEADPHONES -> DeviceType.HEADPHONES
                        else -> DeviceType.AUDIO_VIDEO
                    }
                }
                MAJOR_COMPUTER -> DeviceType.COMPUTER
                MAJOR_PHONE -> DeviceType.PHONE
                MAJOR_PERIPHERAL -> DeviceType.PERIPHERAL
                else -> DeviceType.UNKNOWN
            }
        }

    /**
     * Проверяет, является ли устройство автомобильной аудиосистемой.
     * Используется для автоматического определения устройств бортового компьютера.
     */
    val isCarAudio: Boolean
        get() {
            return (deviceClass and 0x1F00) == MAJOR_AUDIO_VIDEO &&
                    (deviceClass and 0x00FF) == AUDIO_VIDEO_CAR_AUDIO
        }

    /**
     * Проверяет, является ли устройство валидным (имеет адрес).
     * Старое свойство, оставлено для совместимости.
     */
    @Deprecated("Используйте isValidDevice()", ReplaceWith("isValidDevice()"))
    val isValid: Boolean
        get() = isValidDevice()
}

/**
 * Перечисление типов Bluetooth устройств.
 * Используется для классификации устройств в интерфейсе пользователя.
 */
@Serializable
enum class DeviceType {
    HEADSET,
    CAR_AUDIO,
    HEADPHONES,
    AUDIO_VIDEO,
    COMPUTER,
    PHONE,
    PERIPHERAL,
    UNKNOWN;

    /**
     * Получает локализованное название типа устройства.
     */
    fun getDisplayName(): String {
        return when (this) {
            HEADSET -> "Гарнитура"
            CAR_AUDIO -> "Автомобильная аудиосистема"
            HEADPHONES -> "Наушники"
            AUDIO_VIDEO -> "Аудио/Видео устройство"
            COMPUTER -> "Компьютер"
            PHONE -> "Телефон"
            PERIPHERAL -> "Периферийное устройство"
            UNKNOWN -> "Неизвестное устройство"
        }
    }
}

/**
 * Расширения для удобной работы с BluetoothDeviceData.
 */
object BluetoothDeviceDataExtensions {
    /**
     * Преобразует список Android BluetoothDevice в список доменных моделей.
     */
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun List<android.bluetooth.BluetoothDevice>.toDomainModels(): List<BluetoothDeviceData> {
        return this.map { device ->
            BluetoothDeviceData.fromAndroidDevice(device)
        }
    }

    /**
     * Находит устройство по адресу в списке.
     */
    fun List<BluetoothDeviceData>.findByAddress(address: String): BluetoothDeviceData? {
        return this.firstOrNull { it.address.equals(address, ignoreCase = true) }
    }
}