// Файл: data/bluetooth/ConnectionState.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothDisabled
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.alexbar3073.bluetoothcar.ui.theme.ListeningGreenBright
import com.alexbar3073.bluetoothcar.ui.theme.ListeningGreenDim

/**
 * ФАЙЛ: data/bluetooth/ConnectionState.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Определяет единую систему состояний Bluetooth подключения. Содержит ВСЮ информацию,
 * необходимую для отображения состояний в UI: описания, цвета, иконки, короткие имена.
 * Используется во всех компонентах приложения для синхронизации логики.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * Этот файл содержит ПОЛНУЮ информацию о каждом статусе. Другие компоненты (виджеты, логгеры)
 * только ЗАПРАШИВАЮТ информацию, не зная деталей реализации. При добавлении новых статусов
 * достаточно обновить только этот файл.
 */

/**
 * Тип отображения для компактного виджета
 */
enum class CompactDisplayType {
    /** Статическая иконка */
    ICON,

    /** Анимированный круговой индикатор прогресса */
    PROGRESS_INDICATOR,

    /** Кастомная анимация (зарезервировано на будущее) */
    CUSTOM_ANIMATION
}

/**
 * Структура, содержащая ВСЕ данные о статусе подключения для UI.
 * Соответствует требованиям ТЗ: "структура статуса, в которой тот вернет все необходимое".
 */
data class ConnectionStatusInfo(
    /** Текущее состояние подключения как enum */
    val state: ConnectionState,

    /** Отображаемое имя статуса для пользователя */
    val displayName: String,

    /** Подробное описание статуса */
    val description: String,

    /** Иконка для полного виджета (BluetoothStatusWidget) */
    val icon: ImageVector,

    /** Тип отображения в компактном виджете */
    val compactDisplayType: CompactDisplayType,

    /** Иконка для компактного виджета (используется если compactDisplayType == ICON) */
    val compactIcon: ImageVector,

    /** Цвет иконки для отображения состояния */
    val iconColor: Color,

    /** Цвет фона для карточки состояния */
    val backgroundColor: Color,

    /** Цвет текста для отображения состояния */
    val textColor: Color,

    /** Краткий текст статуса для компактных виджетов */
    val shortStatusText: String,

    /** Короткое имя состояния для использования в логах и отладке */
    val logShortName: String,

    /** Разрешает ли состояние ручное переподключение */
    val allowsManualRetry: Boolean,

    /** Находится ли соединение в активном состоянии */
    val isActive: Boolean,

    /** Находится ли соединение в процессе подключения */
    val isConnecting: Boolean,

    /** Является ли состояние ошибкой */
    val isError: Boolean
)

/**
 * Единое перечисление состояний Bluetooth подключения.
 * Содержит ПОЛНУЮ информацию для отображения: описания, цвета, иконки.
 *
 * Согласно ТЗ (последний абзац страницы 5):
 * - ERROR: требует ручного переподключения
 * - DEVICE_UNAVAILABLE: требует ручного переподключения
 * - Остальные статусы: автоматическая обработка
 */
enum class ConnectionState(
    /** Разрешает ли состояние ручное переподключение */
    val allowsManualRetry: Boolean = false
) {
    /**
     * Начальное состояние, инициализация системы
     * Используется при старте BluetoothConnectionManager
     */
    UNDEFINED,

    /**
     * Устройство не выбрано в настройках или не сопряжено
     * Результат проверки ConnectionFeasibilityChecker
     */
    NO_DEVICE_SELECTED,

    /**
     * Устройство выбрано и готово к проверке доступности
     */
    DEVICE_SELECTED,

    /**
     * Bluetooth отключен или недоступен
     * Результат проверки ConnectionFeasibilityChecker
     * Автоматическое переподключение при включении Bluetooth через событийную модель
     */
    BLUETOOTH_DISABLED,

    /**
     * Активный поиск устройства в эфире
     * Используется DeviceAvailabilityMonitor
     */
    SEARCHING_DEVICE,

    /**
     * Устройство обнаружено и доступно для подключения
     * Результат работы DeviceAvailabilityMonitor
     */
    DEVICE_AVAILABLE,

    /**
     * Устройство не найдено после заданного количества попыток
     * Результат работы DeviceAvailabilityMonitor
     * Согласно ТЗ: требует ручного переподключения
     */
    DEVICE_UNAVAILABLE(allowsManualRetry = true),

    /**
     * Установка RFCOMM соединения с устройством
     * Используется ConnectionStateManager
     */
    CONNECTING,

    /**
     * Физическое соединение установлено
     * Результат работы ConnectionStateManager
     */
    CONNECTED,

    /**
     * Отправка настроек приложения на устройство
     * Используется DataStreamHandler
     */
    SENDING_SETTINGS,

    /**
     * Отправка команды запроса данных
     * Используется DataStreamHandler
     */
    REQUESTING_DATA,

    /**
     * Активный прием данных от устройства
     * Финальное рабочее состояние
     */
    LISTENING_DATA,

    /**
     * Соединение разорвано
     * Результат мониторинга ConnectionStateManager
     * Автоматический перезапуск согласно Потоку 5 ТЗ
     */
    DISCONNECTED,

    /**
     * Произошла ошибка в процессе подключения
     * Согласно ТЗ: требует ручного переподключения
     */
    ERROR(allowsManualRetry = true);

    // ===== ОСНОВНЫЕ UI МЕТОДЫ =====

    /**
     * Получить цвет фона для состояния подключения (Compose Color).
     * Используется в виджетах для фона карточек.
     */
    fun getBackgroundColor(): Color {
        return when (this) {
            LISTENING_DATA -> Color(0xFFE8F5E9) // Светло-зеленый
            CONNECTED, SENDING_SETTINGS, REQUESTING_DATA ->
                Color(0xFFE3F2FD) // Светло-синий
            CONNECTING -> Color(0xFFFFF8E1) // Светло-желтый
            DEVICE_AVAILABLE -> Color(0xFFE8EAF6) // Светло-индиго
            DEVICE_SELECTED, DEVICE_UNAVAILABLE ->
                Color(0xFFF3E5F5) // Светло-фиолетовый
            BLUETOOTH_DISABLED -> Color(0xFFFFEBEE) // Светло-красный
            NO_DEVICE_SELECTED -> Color(0xFFFAFAFA) // Светло-серый
            ERROR -> Color(0xFFFFEBEE) // Светло-красный
            DISCONNECTED -> Color(0xFFF5F5F5) // Светло-серый
            UNDEFINED -> Color(0xFFF5F5F5) // Светло-серый
            SEARCHING_DEVICE -> Color(0xFFFFF8E1) // Светло-желтый (как CONNECTING)
        }
    }

    /**
     * Получить цвет иконки для состояния подключения (Compose Color).
     * Используется для окрашивания иконок в виджетах.
     * @param isDarkTheme Флаг темной темы для выбора адаптивного цвета.
     */
    fun getIconColor(isDarkTheme: Boolean = true): Color {
        return when (this) {
            LISTENING_DATA -> if (isDarkTheme) ListeningGreenBright else ListeningGreenDim
            CONNECTED, SENDING_SETTINGS, REQUESTING_DATA ->
                Color(0xFF1565C0) // Темно-синий
            CONNECTING -> Color(0xFFF57C00) // Оранжевый
            DEVICE_AVAILABLE -> Color(0xFF3949AB) // Индиго
            DEVICE_SELECTED -> Color(0xFF7B1FA2) // Фиолетовый
            DEVICE_UNAVAILABLE -> Color(0xFF5D4037) // Коричневый
            BLUETOOTH_DISABLED -> Color(0xFFD32F2F) // Красный
            NO_DEVICE_SELECTED -> Color(0xFF757575) // Серый
            ERROR -> Color(0xFFD32F2F) // Красный
            DISCONNECTED -> Color(0xFF757575) // Серый
            UNDEFINED -> Color(0xFF757575) // Серый
            SEARCHING_DEVICE -> Color(0xFFF57C00) // Оранжевый (как SEARCHING)
        }
    }

    /**
     * Получить цвет текста для состояния подключения (Compose Color).
     * Используется для текста в виджетах.
     */
    fun getTextColor(): Color {
        return when (this) {
            LISTENING_DATA -> Color(0xFF1B5E20) // Темно-зеленый
            CONNECTED, SENDING_SETTINGS, REQUESTING_DATA ->
                Color(0xFF0D47A1) // Темно-синий
            CONNECTING -> Color(0xFFE65100) // Темно-оранжевый
            DEVICE_AVAILABLE -> Color(0xFF283593) // Темно-индиго
            DEVICE_SELECTED -> Color(0xFF4A148C) // Темно-фиолетовый
            DEVICE_UNAVAILABLE -> Color(0xFF3E2723) // Темно-коричневый
            BLUETOOTH_DISABLED -> Color(0xFFB71C1C) // Темно-красный
            NO_DEVICE_SELECTED -> Color(0xFF424242) // Темно-серый
            ERROR -> Color(0xFFB71C1C) // Темно-красный
            DISCONNECTED -> Color(0xFF424242) // Темно-серый
            UNDEFINED -> Color(0xFF424242) // Темно-серый
            SEARCHING_DEVICE -> Color(0xFFE65100) // Темно-оранжевый (как SEARCHING)
        }
    }

    /**
     * Получить текст статуса для отображения в UI.
     * Более короткий и понятный вариант, чем getDescription().
     */
    fun getStatusText(): String {
        return when (this) {
            LISTENING_DATA -> "Соединение установлено"
            CONNECTED -> "Устройство подключено"
            SENDING_SETTINGS -> "Отправка настроек"
            REQUESTING_DATA -> "Запрос данных"
            CONNECTING -> "Подключение..."
            DEVICE_AVAILABLE -> "Устройство доступно"
            DEVICE_SELECTED -> "Устройство выбрано"
            DEVICE_UNAVAILABLE -> "Устройство недоступно"
            BLUETOOTH_DISABLED -> "Bluetooth отключен"
            NO_DEVICE_SELECTED -> "Устройство не выбрано"
            ERROR -> "Ошибка подключения"
            DISCONNECTED -> "Отключено"
            UNDEFINED -> "Не определено"
            SEARCHING_DEVICE -> "Поиск устройства..."
        }
    }

    /**
     * Получить краткий текст статуса для компактных виджетов.
     * Используется в CompactBluetoothStatusWidget.
     */
    fun getShortStatusText(): String {
        return when (this) {
            LISTENING_DATA -> "ON"
            CONNECTED -> "CON"
            SENDING_SETTINGS -> "SET"
            REQUESTING_DATA -> "REQ"
            CONNECTING -> "CONN"
            DEVICE_AVAILABLE -> "AVAIL"
            DEVICE_SELECTED -> "SEL"
            DEVICE_UNAVAILABLE -> "UNAVL"
            BLUETOOTH_DISABLED -> "OFF"
            NO_DEVICE_SELECTED -> "NO DEV"
            ERROR -> "ERR"
            DISCONNECTED -> "OFF"
            UNDEFINED -> "?"
            SEARCHING_DEVICE -> "SRCH"
        }
    }

    /**
     * Получить описание статуса для отображения в UI.
     * Более подробное описание, чем getStatusText().
     */
    fun getStatusDescription(): String {
        return when (this) {
            LISTENING_DATA -> "Устройство подключено и передает данные"
            CONNECTED -> "Соединение установлено, настройка протокола"
            SENDING_SETTINGS -> "Отправка настроек приложения на устройство"
            REQUESTING_DATA -> "Запрос передачи данных от устройства"
            CONNECTING -> "Устанавливается соединение с устройством"
            DEVICE_AVAILABLE -> "Устройство обнаружено и готово к подключению"
            DEVICE_SELECTED -> "Устройство выбрано, проверка доступности"
            DEVICE_UNAVAILABLE -> "Устройство не найдено"
            BLUETOOTH_DISABLED -> "Включите Bluetooth для подключения"
            NO_DEVICE_SELECTED -> "Выберите устройство в настройках"
            ERROR -> "Произошла ошибка при подключении"
            DISCONNECTED -> "Соединение разорвано"
            UNDEFINED -> "Состояние не определено"
            SEARCHING_DEVICE -> "Активный поиск устройства в зоне действия"
        }
    }

    // ===== ИКОНКИ =====

    /**
     * Получает иконку для отображения состояния в UI (полная версия).
     * Каждому состоянию соответствует своя иконка Material Icons.
     * Используется в полных виджетах (BluetoothStatusWidget).
     * @return ImageVector иконки состояния
     */
    fun getIcon(): ImageVector {
        return when (this) {
            UNDEFINED -> Icons.Outlined.HelpOutline
            NO_DEVICE_SELECTED -> Icons.Outlined.BluetoothDisabled
            DEVICE_SELECTED -> Icons.Outlined.Bluetooth
            BLUETOOTH_DISABLED -> Icons.Outlined.BluetoothDisabled
            SEARCHING_DEVICE -> Icons.Outlined.Search
            DEVICE_AVAILABLE -> Icons.Outlined.BluetoothSearching
            DEVICE_UNAVAILABLE -> Icons.Outlined.BluetoothDisabled
            CONNECTING -> Icons.Outlined.BluetoothSearching
            CONNECTED -> Icons.Outlined.BluetoothConnected
            SENDING_SETTINGS -> Icons.Outlined.Settings
            REQUESTING_DATA -> Icons.Outlined.Sync
            LISTENING_DATA -> Icons.Outlined.DataUsage
            DISCONNECTED -> Icons.Outlined.BluetoothDisabled
            ERROR -> Icons.Outlined.ErrorOutline
        }
    }

    /**
     * Получает компактную иконку для отображения состояния в UI.
     * Используется в компактных виджетах когда compactDisplayType == ICON.
     * @return ImageVector компактной иконки состояния
     */
    fun getCompactIcon(): ImageVector {
        return when (this) {
            LISTENING_DATA -> Icons.Outlined.Power
            CONNECTED, SENDING_SETTINGS, REQUESTING_DATA -> Icons.Outlined.Power
            DEVICE_AVAILABLE -> Icons.Outlined.Bluetooth
            DEVICE_SELECTED -> Icons.Outlined.Bluetooth
            DEVICE_UNAVAILABLE, BLUETOOTH_DISABLED, DISCONNECTED -> Icons.Outlined.BluetoothDisabled
            NO_DEVICE_SELECTED -> Icons.Outlined.BluetoothDisabled
            ERROR -> Icons.Outlined.ErrorOutline
            UNDEFINED -> Icons.Outlined.HelpOutline
            CONNECTING, SEARCHING_DEVICE -> Icons.Outlined.BluetoothSearching // Fallback
        }
    }

    /**
     * Определяет тип отображения для компактного виджета.
     * Для состояний процесса подключения используем анимированный индикатор.
     * @return CompactDisplayType тип отображения
     */
    private fun getCompactDisplayType(): CompactDisplayType {
        return when (this) {
            CONNECTING, SEARCHING_DEVICE -> CompactDisplayType.PROGRESS_INDICATOR
            else -> CompactDisplayType.ICON
        }
    }

    // ===== НОВЫЙ МЕТОД ДЛЯ ПОЛУЧЕНИЯ ПОЛНОЙ СТРУКТУРЫ =====

    /**
     * Получить полную структуру данных о статусе для передачи в UI.
     * Соответствует требованиям ТЗ: "структура статуса, в которой тот вернет все необходимое".
     * @param isDarkTheme Флаг темной темы для выбора адаптивного цвета иконки.
     * @return ConnectionStatusInfo с ВСЕМИ данными для отображения
     */
    fun toStatusInfo(isDarkTheme: Boolean = true): ConnectionStatusInfo {
        return ConnectionStatusInfo(
            state = this,
            displayName = getStatusText(),
            description = getStatusDescription(),
            icon = getIcon(),
            compactDisplayType = getCompactDisplayType(),
            compactIcon = getCompactIcon(),
            iconColor = getIconColor(isDarkTheme),
            backgroundColor = getBackgroundColor(),
            textColor = getTextColor(),
            shortStatusText = getShortStatusText(),
            logShortName = getShortName(),
            allowsManualRetry = allowsManualRetry,
            isActive = isActiveState(),
            isConnecting = isConnectingState(),
            isError = isErrorState()
        )
    }

    // ===== МЕТОДЫ ИЗ ИСХОДНОГО ConnectionState (с сохранением обратной совместимости) =====

    /**
     * Получает пользовательское описание состояния для отображения в UI.
     * @return Локализованное описание состояния
     * @deprecated Используйте getStatusText() для более точных текстов
     */
    @Deprecated(
        "Используйте getStatusText() для отображения или toStatusInfo() для полной структуры",
        ReplaceWith("getStatusText()")
    )
    fun getDescription(): String {
        return getStatusText() // Делегируем новому методу
    }

    /**
     * Получает короткое имя состояния для использования в логах и отладке.
     * @return Короткое строковое представление состояния
     */
    fun getShortName(): String {
        return when (this) {
            UNDEFINED -> "UNDEFINED"
            NO_DEVICE_SELECTED -> "NO_DEVICE"
            DEVICE_SELECTED -> "DEVICE_SELECTED"
            BLUETOOTH_DISABLED -> "BT_DISABLED"
            SEARCHING_DEVICE -> "SEARCHING"
            DEVICE_AVAILABLE -> "AVAILABLE"
            DEVICE_UNAVAILABLE -> "UNAVAILABLE"
            CONNECTING -> "CONNECTING"
            CONNECTED -> "CONNECTED"
            SENDING_SETTINGS -> "SENDING_SETTINGS"
            REQUESTING_DATA -> "REQUESTING_DATA"
            LISTENING_DATA -> "LISTENING_DATA"
            DISCONNECTED -> "DISCONNECTED"
            ERROR -> "ERROR"
        }
    }

    /**
     * Получает цвет для отображения состояния в UI.
     * @return Цвет состояния в формате ARGB Int
     */
    fun getColor(): Int {
        // Используем value property вместо toArgb()
        return getIconColor().value.toInt()
    }

    /**
     * Получает цвет для отображения состояния в UI (Compose Color версия).
     * @return Color для использования в Compose UI
     */
    fun getComposeColor(): Color {
        return getIconColor() // Используем цвет иконки как основной цвет
    }

    /**
     * Получает цвет иконки для отображения состояния в UI.
     * @return Цвет иконки в формате ARGB Int
     */
    fun getIconTint(): Int {
        return getIconColor().value.toInt()
    }

    /**
     * Получает цвет иконки для отображения состояния в UI (Compose Color версия).
     * @return Color для использования в Compose UI
     */
    fun getIconComposeTint(): Color {
        return getIconColor()
    }

    /**
     * Получает цвет фона для виджета состояния.
     * Обычно это более светлая/прозрачная версия основного цвета.
     * @return Цвет фона в формате ARGB Int
     */
    fun getBackgroundColorAsInt(): Int {
        return getBackgroundColor().value.toInt()
    }

    /**
     * Получает цвет фона для виджета состояния (Compose Color версия).
     * @return Color для использования в Compose UI
     */
    fun getBackgroundComposeColor(): Color {
        return getBackgroundColor()
    }

    // ===== ЛОГИЧЕСКИЕ МЕТОДЫ ДЛЯ БИЗНЕС-ЛОГИКИ (без изменений) =====

    /**
     * Проверяет, находится ли соединение в активном состоянии работы с данными.
     * Активными считаются состояния от CONNECTED до LISTENING_DATA включительно.
     * @return true если соединение активно (установлено или обмен данными)
     */
    fun isActiveState(): Boolean {
        return when (this) {
            CONNECTED, SENDING_SETTINGS, REQUESTING_DATA, LISTENING_DATA -> true
            else -> false
        }
    }

    /**
     * Проверяет, находится ли соединение в процессе подключения.
     * Включает состояния поиска устройства и установки соединения.
     * @return true если выполняется процесс подключения
     */
    fun isConnectingState(): Boolean {
        return when (this) {
            SEARCHING_DEVICE, DEVICE_AVAILABLE, CONNECTING -> true
            else -> false
        }
    }

    /**
     * Проверяет, можно ли выполнить попытку подключения из текущего состояния.
     * Подключение можно начать только из состояний DEVICE_AVAILABLE и DEVICE_SELECTED.
     * @return true если можно инициировать подключение
     */
    fun canAttemptConnection(): Boolean {
        return this == DEVICE_AVAILABLE || this == DEVICE_SELECTED
    }

    /**
     * Проверяет, является ли состояние конечным (ошибка или явное отключение).
     * @return true если состояние конечное
     */
    fun isTerminalState(): Boolean {
        return this == DISCONNECTED || this == ERROR || this == DEVICE_UNAVAILABLE
    }

    /**
     * Проверяет, установлено ли физическое соединение.
     * Включает состояния от CONNECTED до LISTENING_DATA.
     * @return true если физическое соединение установлено
     */
    fun isPhysicallyConnected(): Boolean {
        return when (this) {
            CONNECTED, SENDING_SETTINGS, REQUESTING_DATA, LISTENING_DATA -> true
            else -> false
        }
    }

    /**
     * Проверяет, находится ли соединение в состоянии ошибки.
     * @return true если состояние ERROR
     */
    fun isErrorState(): Boolean {
        return this == ERROR
    }

    /**
     * Проверяет, находится ли соединение в состоянии поиска или подключения.
     * @return true если состояние SEARCHING_DEVICE или CONNECTING
     */
    fun isSearchingOrConnecting(): Boolean {
        return this == SEARCHING_DEVICE || this == CONNECTING
    }

    /**
     * Проверяет, показывает ли состояние, что устройство доступно.
     * @return true если состояние DEVICE_AVAILABLE
     */
    fun isDeviceAvailable(): Boolean {
        return this == DEVICE_AVAILABLE
    }

    /**
     * Проверяет, показывает ли состояние, что устройство недоступно.
     * @return true если состояние DEVICE_UNAVAILABLE
     */
    fun isDeviceUnavailable(): Boolean {
        return this == DEVICE_UNAVAILABLE
    }
}
