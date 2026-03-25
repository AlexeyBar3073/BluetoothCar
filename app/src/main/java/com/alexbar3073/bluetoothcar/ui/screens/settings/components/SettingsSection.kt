// Файл: ui/screens/settings/components/SettingsSection.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.EvStation
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs.EditDialogData
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

@Composable
fun SettingsSection(
    appSettings: AppSettings,
    selectedDevice: BluetoothDeviceData?,
    navController: NavController,
    onEditDialogShow: (EditDialogData) -> Unit,
    onThemeDialogShow: () -> Unit,
    onDeviceClear: () -> Unit,
    onDeviceSelect: () -> Unit
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
            SimpleSettingItem(
                title = "Устройство данных",
                value = selectedDevice?.name ?: "Не выбрано",
                icon = Icons.Default.Bluetooth,
                iconColor = AppColors.PrimaryBlue,
                hasClearButton = selectedDevice != null,
                onClick = onDeviceSelect,
                onClear = onDeviceClear
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Объем топливного бака",
                value = "${appSettings.fuelTankCapacity.toInt()} л",
                icon = Icons.Default.LocalGasStation,
                iconColor = AppColors.TextSecondary,
                hasClearButton = false,
                onClick = {
                    onEditDialogShow(
                        EditDialogData(
                            title = "Объем топливного бака",
                            currentValue = appSettings.fuelTankCapacity,
                            minValue = 10f,
                            maxValue = 200f,
                            unit = "л",
                            step = 1f
                        )
                    )
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Мин. остаток топлива",
                value = "${appSettings.minFuelLevel.toInt()} л",
                icon = Icons.Default.PriorityHigh,
                iconColor = AppColors.Error,
                hasClearButton = false,
                onClick = {
                    onEditDialogShow(
                        EditDialogData(
                            title = "Мин. остаток топлива",
                            currentValue = appSettings.minFuelLevel,
                            minValue = 1f,
                            maxValue = appSettings.fuelTankCapacity,
                            unit = "л",
                            step = 1f
                        )
                    )
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Производительность форсунки",
                value = "${appSettings.injectorPerformance.toInt()} мл/мин",
                icon = Icons.Default.PrecisionManufacturing,
                iconColor = AppColors.TextSecondary,
                hasClearButton = false,
                onClick = {
                    onEditDialogShow(
                        EditDialogData(
                            title = "Производительность форсунки",
                            currentValue = appSettings.injectorPerformance,
                            minValue = 100f,
                            maxValue = 500f,
                            unit = "мл/мин",
                            step = 10f
                        )
                    )
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Количество форсунок",
                value = appSettings.injectorCount.toString(),
                icon = Icons.Default.EvStation,
                iconColor = AppColors.TextSecondary,
                hasClearButton = false,
                onClick = {
                    onEditDialogShow(
                        EditDialogData(
                            title = "Количество форсунок",
                            currentValue = appSettings.injectorCount.toFloat(),
                            minValue = 1f,
                            maxValue = 12f,
                            unit = "шт",
                            step = 1f
                        )
                    )
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Сигналы датчика скорости",
                value = "${appSettings.speedSensorSignalsPerMeter} на 1 метр",
                icon = Icons.Default.Speed,
                iconColor = AppColors.TextSecondary,
                hasClearButton = false,
                onClick = {
                    onEditDialogShow(
                        EditDialogData(
                            title = "Сигналы датчика скорости",
                            currentValue = appSettings.speedSensorSignalsPerMeter.toFloat(),
                            minValue = 100f,
                            maxValue = 10000f,
                            unit = "сигналов/метр",
                            step = 100f
                        )
                    )
                }
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Тема оформления",
                value = when (appSettings.selectedTheme) {
                    "system" -> "Системная"
                    "dark" -> "Темная"
                    "light" -> "Светлая"
                    "blue_dark" -> "Синяя темная"
                    else -> "Системная"
                },
                icon = Icons.Default.Palette,
                iconColor = AppColors.TextSecondary,
                hasClearButton = false,
                onClick = onThemeDialogShow
            )

            HorizontalDivider(
                color = AppColors.SurfaceMedium,
                thickness = 1.dp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            SimpleSettingItem(
                title = "Интервал обновления",
                value = "${appSettings.updateInterval} мс",
                icon = Icons.Default.Update,
                iconColor = AppColors.TextSecondary,
                hasClearButton = false,
                onClick = {
                    onEditDialogShow(
                        EditDialogData(
                            title = "Интервал обновления",
                            currentValue = appSettings.updateInterval.toFloat(),
                            minValue = 100f,
                            maxValue = 5000f,
                            unit = "мс",
                            step = 100f
                        )
                    )
                }
            )
        }
    }
}
