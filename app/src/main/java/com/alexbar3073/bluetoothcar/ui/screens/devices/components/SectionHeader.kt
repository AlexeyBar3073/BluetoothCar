// Файл: ui/screens/devices/components/SectionHeader.kt
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: UI/Components/SectionHeader
 * 
 * ФАЙЛ: ui/screens/devices/components/SectionHeader.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/devices/components/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Компонент заголовка логической секции. 
 * Используется для визуального разделения групп элементов в списках (например, "Сопряженные устройства").
 * 
 * ОТВЕТСТВЕННОСТЬ: Отображение названия секции с цветовым акцентом и дополнительным описанием.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Stateless компонент.
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Визуальная иерархия и компактность.
 * 
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: Используется в DevicesScreen и других экранах со списками.
 */

/**
 * Заголовок секции.
 * 
 * @param title Основной текст заголовка.
 * @param subtitle Дополнительный поясняющий текст.
 * @param modifier Модификатор оформления.
 */
@Composable
fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    // Горизонтальный контейнер с уменьшенными вертикальными отступами (8 dp) для компактности
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левый декоративный элемент: вертикальная полоса акцентного цвета
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .background(
                    AppColors.PrimaryBlue,
                    shape = RoundedCornerShape(2.dp)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Текстовый блок: Заголовок и подзаголовок
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextSecondary
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.TextTertiary,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Правый декоративный элемент: точка индикации (неактивная по умолчанию)
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = AppColors.PrimaryBlue.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
    }
}
