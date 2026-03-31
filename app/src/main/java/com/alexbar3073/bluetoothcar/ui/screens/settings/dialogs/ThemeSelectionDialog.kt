// Файл: ui/screens/settings/dialogs/ThemeSelectionDialog.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: Диалог выбора темы / ThemeSelectionDialog
 *
 * ФАЙЛ: ui/screens/settings/dialogs/ThemeSelectionDialog.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/dialogs/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Диалог для выбора визуальной темы приложения (темная, светлая, синяя темная).
 * Реализован в виде списка с радиокнопками и поддержкой прокрутки.
 * "Системная" тема удалена для обеспечения предсказуемости интерфейса.
 *
 * ОТВЕТСТВЕННОСТЬ: Предоставление пользователю возможности смены темы оформления.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Compose Component
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Непрозрачный интерфейс с централизованным управлением оформлением через тему.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: AppColors (для DialogBackground и DialogBorder).
 * - Вызывается из: SettingsScreen.kt.
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    /** 
     * Список доступных тем и их отображаемых названий.
     * Вариант "system" удален по требованию.
     */
    val themes = listOf(
        "dark" to "Темная",
        "light" to "Светлая",
        "blue_dark" to "Синяя темная"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, AppColors.DialogBorder),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.DialogBackground,
                contentColor = AppColors.TextPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Заголовок диалога
                Text(
                    text = "Выбор темы оформления",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Список тем с ограниченной высотой и прокруткой
                Column(
                    modifier = Modifier
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 4.dp)
                ) {
                    themes.forEach { (themeId, themeName) ->
                        val isSelected = currentTheme == themeId

                        ThemeOptionItem(
                            themeId = themeId,
                            themeName = themeName,
                            isSelected = isSelected,
                            onThemeSelected = {
                                onThemeSelected(themeId)
                                onDismiss()
                            }
                        )
                    }
                }

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppColors.SurfaceMedium,
                        contentColor = AppColors.TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Закрыть")
                }
            }
        }
    }
}

@Composable
private fun ThemeOptionItem(
    themeId: String,
    themeName: String,
    isSelected: Boolean,
    onThemeSelected: () -> Unit
) {
    Surface(
        onClick = onThemeSelected,
        color = if (isSelected)
            AppColors.PrimaryBlue.copy(alpha = 0.15f)
        else Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        border = if (isSelected)
            BorderStroke(1.dp, AppColors.PrimaryBlue.copy(alpha = 0.3f))
        else null
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onThemeSelected,
                colors = RadioButtonDefaults.colors(
                    selectedColor = AppColors.PrimaryBlue,
                    unselectedColor = AppColors.TextSecondary
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    themeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppColors.TextPrimary,
                    fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
                )
            }
        }
    }
}
