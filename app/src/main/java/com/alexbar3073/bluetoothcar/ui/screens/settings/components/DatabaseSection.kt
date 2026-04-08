// Файл: ui/screens/settings/components/DatabaseSection.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: Настройки/Компоненты/БазыДанных
 *
 * ФАЙЛ: ui/screens/settings/components/DatabaseSection.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/components/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Секция настроек для управления локальными базами данных.
 * Позволяет обновлять справочники ошибок из внешних файлов,
 * а также выгружать текущий состав баз для резервного копирования или редактирования.
 *
 * ОТВЕТСТВЕННОСТЬ: Отображение карточек управления БД с функциями импорта и экспорта.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Jetpack Compose Component
 */

@Composable
fun DatabaseSection(
    onImportErrors: () -> Unit,
    onExportErrors: () -> Unit
) {
    Column {
        // Заголовок секции
        Text(
            text = "УПРАВЛЕНИЕ БАЗАМИ ДАННЫХ",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = AppColors.PrimaryBlue,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )

        // Контейнер для карточек
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight)
        ) {
            Column {
                // Пункт 1: База ошибок (Импорт и Экспорт)
                DatabaseItem(
                    title = "База ошибок ЭБУ",
                    subtitle = "Импорт/Экспорт справочника кодов",
                    mainIcon = Icons.Default.Storage,
                    onMainClick = onImportErrors,
                    onActionClick = onExportErrors,
                    actionIcon = Icons.Default.Save
                )
            }
        }
    }
}

/**
 * Вспомогательный элемент списка для секции БД.
 * Содержит основную область клика (импорт) и дополнительную кнопку справа (экспорт).
 */
@Composable
private fun DatabaseItem(
    title: String,
    subtitle: String,
    mainIcon: ImageVector,
    onMainClick: () -> Unit,
    onActionClick: () -> Unit,
    actionIcon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Левая часть: Иконка и Текст (клик вызывает импорт)
        Row(
            modifier = Modifier
                .weight(1f)
                .clickable { onMainClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = mainIcon,
                contentDescription = null,
                tint = AppColors.PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ContentDetail
                )
            }
        }

        // Правая часть: Отдельная кнопка для экспорта (иконка дискеты)
        IconButton(
            onClick = onActionClick,
            modifier = Modifier.padding(end = 8.dp)
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = "Выгрузить базу",
                tint = AppColors.PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
