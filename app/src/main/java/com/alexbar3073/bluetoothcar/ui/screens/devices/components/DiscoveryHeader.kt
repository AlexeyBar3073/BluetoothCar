// Файл: ui/screens/devices/components/DiscoveryHeader.kt
package com.alexbar3073.bluetoothcar.ui.screens.devices.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * Заголовок секции поиска устройств.
 * Отображает статус сканирования, количество найденных устройств и кнопку управления поиском.
 *
 * @param availableDevicesCount Количество обнаруженных устройств.
 * @param isDiscovering Флаг активного процесса поиска.
 * @param onStartDiscovery Действие при нажатии кнопки начала поиска.
 * @param onStopDiscovery Действие при нажатии кнопки остановки поиска.
 */
@Composable
fun DiscoveryHeader(
    availableDevicesCount: Int,
    isDiscovering: Boolean,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Основной контейнер заголовка с уменьшенными вертикальными отступами для компактности
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp), // Уменьшено с 24.dp до 12.dp
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левая часть: Индикатор секции и текстовая информация
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Вертикальный цветовой акцент
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(20.dp)
                        .background(
                            AppColors.Error,
                            shape = RoundedCornerShape(2.dp)
                        )
                )
                Spacer(modifier = Modifier.width(12.dp))
                
                // Текстовые метки
                Column {
                    Text(
                        text = "НАЙДЕННЫЕ УСТРОЙСТВА",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextSecondary
                    )
                    Text(
                        text = "$availableDevicesCount обнаружено",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        // Правая часть: Кнопка управления состоянием поиска
        if (isDiscovering) {
            // Кнопка остановки с индикатором прогресса
            IconButton(
                onClick = onStopDiscovery,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            AppColors.TransparentPrimary,
                            shape = CircleShape
                        )
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = AppColors.PrimaryBlue
                    )
                }
            }
        } else {
            // Кнопка запуска поиска
            IconButton(
                onClick = onStartDiscovery,
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            AppColors.TransparentPrimary,
                            shape = CircleShape
                        )
                ) {
                    androidx.compose.material3.Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Начать поиск",
                        tint = AppColors.PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
