// Файл: ui/screens/home/widgets/ConnectionStateHelpers.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.ui.graphics.Color
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState

/**
 * Вспомогательные функции для работы с ConnectionState в UI.
 *
 * @deprecated Вся логика перенесена в ConnectionState. Используйте методы напрямую:
 * - ConnectionStateHelpers.getBackgroundColor(state) → state.getBackgroundColor()
 * - ConnectionStateHelpers.getIconColor(state) → state.getIconColor()
 * - ConnectionStateHelpers.getTextColor(state) → state.getTextColor()
 * - ConnectionStateHelpers.getStatusText(state) → state.getStatusText()
 * - ConnectionStateHelpers.getShortStatusText(state) → state.getShortStatusText()
 * - ConnectionStateHelpers.getStatusDescription(state) → state.getStatusDescription()
 *
 * Удалите этот файл после обновления всех использований.
 */
@Deprecated(
    "Используйте методы напрямую из ConnectionState",
    ReplaceWith(
        "state.getBackgroundColor()",
        "com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState"
    )
)
object ConnectionStateHelpers {

    @Deprecated("Используйте state.getBackgroundColor()")
    fun getBackgroundColor(state: ConnectionState): Color {
        return state.getBackgroundColor()
    }

    @Deprecated("Используйте state.getIconColor()")
    fun getIconColor(state: ConnectionState): Color {
        return state.getIconColor()
    }

    @Deprecated("Используйте state.getTextColor()")
    fun getTextColor(state: ConnectionState): Color {
        return state.getTextColor()
    }

    @Deprecated("Используйте state.getStatusText()")
    fun getStatusText(state: ConnectionState): String {
        return state.getStatusText()
    }

    @Deprecated("Используйте state.getShortStatusText()")
    fun getShortStatusText(state: ConnectionState): String {
        return state.getShortStatusText()
    }

    @Deprecated("Используйте state.getStatusDescription()")
    fun getStatusDescription(state: ConnectionState): String {
        return state.getStatusDescription()
    }
}