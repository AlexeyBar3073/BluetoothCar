package com.alexbar3073.bluetoothcar.ui.screens.settings.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * Компонент для отображения настройки с переключателем (Switch)
 *
 * Используется для включения/выключения виджетов:
 * - Показывать спидометр
 * - Показывать уровень топлива
 * - Показывать напряжение
 *
 * @param title Название настройки
 * @param subtitle Описание настройки (подзаголовок)
 * @param icon Иконка настройки
 * @param iconColor Цвет иконки
 * @param isChecked Текущее состояние переключателя
 * @param onCheckedChange Обработчик изменения состояния
 */
@Composable
fun SwitchSettingItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Иконка настройки
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
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Текст настройки
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextTertiary
            )
        }

        // Переключатель
        Switch(
            checked = isChecked,
            onCheckedChange = { newValue ->
                Log.d("SwitchSettingItem", "Переключение: $title -> $newValue")
                onCheckedChange(newValue)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppColors.PrimaryBlue,
                checkedTrackColor = AppColors.PrimaryBlue.copy(alpha = 0.5f),
                uncheckedThumbColor = AppColors.TextSecondary,
                uncheckedTrackColor = AppColors.SurfaceMedium
            )
        )
    }
}