// Файл: ui/screens/settings/components/WidgetsSection.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
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
 * ТЕГ: Секция настроек виджетов / WidgetsSection
 *
 * ФАЙЛ: ui/screens/settings/components/WidgetsSection.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/components/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Секция настроек визуального оформления приборов и виджетов. 
 * Позволяет пользователю выбирать цветовую схему и тему приложения.
 *
 * ОТВЕТСТВЕННОСТЬ: Отображение карточек настроек визуализации.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Compose Component
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Группировка связанных настроек интерфейса в единый блок.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: ColorSettingItem.kt, SimpleSettingItem.kt.
 * - Вызывает: ColorPickerDialog.kt (диалог выбора).
 * - Вызывается из: SettingsContent.kt.
 */
@Composable
fun WidgetsSection(
    appSettings: AppSettings,
    onThemeDialogShow: () -> Unit,
    onUpdateSetting: (AppSettings) -> Unit
) {
    /** Управление видимостью диалога выбора цвета */
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
            // Настройка темы оформления
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

            /** 
             * Настройка цвета оформления.
             * Иконка в элементе окрашена в текущий выбранный цвет.
             */
            ColorSettingItem(
                title = "Цвет оформления",
                subtitle = "Основной цвет оформления приборов",
                currentColor = Color(appSettings.currentDashboardColor),
                onColorClick = { showColorPicker = true }
            )
        }
    }

    /** Отображение диалога выбора цвета (HSV) */
    if (showColorPicker) {
        ColorPickerDialog(
            appSettings = appSettings,
            onDismiss = { showColorPicker = false },
            onColorSelected = { color ->
                /** Сохранение нового цвета в настройки приложения */
                onUpdateSetting(appSettings.copy(currentDashboardColor = color.toArgb().toLong()))
            }
        )
    }
}
