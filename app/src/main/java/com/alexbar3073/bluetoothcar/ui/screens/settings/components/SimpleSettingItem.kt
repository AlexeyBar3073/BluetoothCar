// Файл: ui/screens/settings/components/SimpleSettingItem.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: Settings/UI/Item
 * 
 * Компонент для отображения простой настройки с иконкой и значением.
 * Обеспечивает единый визуальный стиль для всех элементов списков настроек и ошибок.
 *
 * @param title Название настройки
 * @param value Текущее значение (отображаемое)
 * @param icon Иконка настройки
 * @param iconColor Цвет иконки
 * @param hasClearButton Показывать ли кнопку очистки
 * @param onClick Обработчик клика по настройке
 * @param onClear Обработчик очистки настройки (если доступно)
 */
@Composable
fun SimpleSettingItem(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    hasClearButton: Boolean = false,
    onClick: () -> Unit = {},
    onClear: () -> Unit = {}
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Иконка настройки в круглой подложке
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

            // Текстовый блок
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                // ОБНОВЛЕНИЕ: Используется семантический цвет ContentDetail для лучшей видимости
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ContentDetail
                )
            }

            // Кнопка очистки (если предусмотрена логикой)
            if (hasClearButton) {
                IconButton(
                    onClick = onClear,
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(30.dp)
                            .background(
                                AppColors.SurfaceMedium,
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Очистить",
                            tint = AppColors.TextTertiary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
