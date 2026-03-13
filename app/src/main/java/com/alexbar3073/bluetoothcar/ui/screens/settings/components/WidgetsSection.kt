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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ФАЙЛ: ui/screens/settings/components/WidgetsSection.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/components/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Секция настроек отображения виджетов. Содержит переключатели для управления видимостью виджетов.
 *
 * ИЗМЕНЕНИЯ:
 * - 2026.02.05 17:00: Убран параметр scope, убраны корутины
 *   Обновление настроек теперь выполняется синхронно
 */

@Composable
fun WidgetsSection(
    appSettings: AppSettings,
    onUpdateSetting: (AppSettings) -> Unit
) {
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
                    Log.d("WidgetsSection", "Переключатель: Спидометр = $isChecked")
                    onUpdateSetting(appSettings.copy(showSpeedometer = isChecked))
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Показывать уровень топлива
            SwitchSettingItem(
                title = "Показывать уровень топлива",
                subtitle = "Отображение виджета топлива",
                icon = Icons.Default.LocalGasStation,
                iconColor = AppColors.TextSecondary,
                isChecked = appSettings.showFuelGauge,
                onCheckedChange = { isChecked ->
                    Log.d("WidgetsSection", "Переключатель: Уровень топлива = $isChecked")
                    onUpdateSetting(appSettings.copy(showFuelGauge = isChecked))
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Показывать напряжение
            SwitchSettingItem(
                title = "Показывать напряжение",
                subtitle = "Отображение виджета напряжения",
                icon = Icons.Default.BatteryChargingFull,
                iconColor = AppColors.TextSecondary,
                isChecked = appSettings.showVoltage,
                onCheckedChange = { isChecked ->
                    Log.d("WidgetsSection", "Переключатель: Напряжение = $isChecked")
                    onUpdateSetting(appSettings.copy(showVoltage = isChecked))
                }
            )
        }
    }
}