// Файл: ui/screens/settings/components/ColorSettingItem.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: Элемент выбора цвета / ColorSettingItem
 *
 * ФАЙЛ: ui/screens/settings/components/ColorSettingItem.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/components/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Компонент списка настроек для выбора цвета оформления.
 * Отображает иконку палитры, окрашенную в текущий выбранный цвет,
 * заголовок и описание.
 *
 * ОТВЕТСТВЕННОСТЬ: Визуализация текущего цвета и инициация диалога выбора.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Compose UI Component
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Наглядность выбора через окрашивание основной иконки.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Используется в: WidgetsSection.kt.
 * - Взаимодействует: ColorPickerDialog.kt (через callback).
 */
@Composable
fun ColorSettingItem(
    title: String,
    /** Краткое описание настройки */
    subtitle: String,
    /** Текущий выбранный цвет для окрашивания иконки */
    currentColor: Color,
    /** Callback при нажатии на элемент */
    onColorClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onColorClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        /** 
         * Контейнер иконки.
         * Иконка палитры теперь является индикатором текущего цвета.
         */
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
                imageVector = Icons.Default.Palette,
                contentDescription = title,
                /** Окрашивание иконки в выбранный пользователем цвет */
                tint = currentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Текстовая информация
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
    }
}
