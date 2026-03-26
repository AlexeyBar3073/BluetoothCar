// Файл: ui/screens/settings/components/WidgetsSection.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.ColorPickerDialog
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: Секция настроек виджетов
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Секция настроек отображения виджетов и кастомизации приборов.
 */

@Composable
fun WidgetsSection(
    appSettings: AppSettings,
    onUpdateSetting: (AppSettings) -> Unit
) {
    var showColorPicker by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.SurfaceLight
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            // Показывать спидометр
            SwitchSettingItem(
                title = "Показывать спидометр",
                subtitle = "Отображение виджета скорости",
                icon = Icons.Default.Speed,
                iconColor = AppColors.TextSecondary,
                isChecked = appSettings.showSpeedometer,
                onCheckedChange = { isChecked ->
                    onUpdateSetting(appSettings.copy(showSpeedometer = isChecked))
                }
            )

            HorizontalDivider(color = AppColors.SurfaceMedium, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            // Показывать уровень топлива
            SwitchSettingItem(
                title = "Показывать уровень топлива",
                subtitle = "Отображение виджета топлива",
                icon = Icons.Default.LocalGasStation,
                iconColor = AppColors.TextSecondary,
                isChecked = appSettings.showFuelGauge,
                onCheckedChange = { isChecked ->
                    onUpdateSetting(appSettings.copy(showFuelGauge = isChecked))
                }
            )

            HorizontalDivider(color = AppColors.SurfaceMedium, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            // Показывать напряжение
            SwitchSettingItem(
                title = "Показывать напряжение",
                subtitle = "Отображение виджета напряжения",
                icon = Icons.Default.BatteryChargingFull,
                iconColor = AppColors.TextSecondary,
                isChecked = appSettings.showVoltage,
                onCheckedChange = { isChecked ->
                    onUpdateSetting(appSettings.copy(showVoltage = isChecked))
                }
            )

            HorizontalDivider(color = AppColors.SurfaceMedium, thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            // Цвет дашборда
            ColorSettingItem(
                title = "Цвет оформления",
                subtitle = "Основной цвет оформления приборов",
                currentColor = Color(appSettings.currentDashboardColor),
                onColorClick = { showColorPicker = true },
                onResetClick = {
                    onUpdateSetting(appSettings.copy(currentDashboardColor = appSettings.defaultDashboardColor))
                }
            )
        }
    }

    if (showColorPicker) {
        ColorPickerDialog(
            appSettings = appSettings,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                onUpdateSetting(appSettings.copy(currentDashboardColor = color.toArgb().toLong()))
            }
        )
    }
}
