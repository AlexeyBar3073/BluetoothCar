// Файл: ui/screens/settings/components/InfoSection.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: Секция информации
 * 
 * ФАЙЛ: ui/screens/settings/components/InfoSection.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/components/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Секция с информацией о приложении и версии прошивки БК.
 * 
 * ОТВЕТСТВЕННОСТЬ: Отображение версии приложения и прошивки оборудования.
 */
@Composable
fun InfoSection(
    firmwareVersion: String = "v1.0"
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
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            AppColors.SurfaceMedium,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Информация",
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "BluetoothCar Monitor",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = AppColors.TextPrimary
                    )
                    Text(
                        "Версия 1.3.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = AppColors.TextTertiary
                    )
                    
                    // ОТОБРАЖЕНИЕ ВЕРСИИ ПРОШИВКИ БК
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Версия прошивки БК: $firmwareVersion",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.PrimaryBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "Программа предназначения для визуализации данных с бортового компьютера по Bluetooth каналу.",
                style = MaterialTheme.typography.labelSmall,
                color = AppColors.TextTertiary
            )
        }
    }
}
