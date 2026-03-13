package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
 * Диалог для выбора темы оформления с поддержкой скроллинга
 */
@Composable
fun ThemeSelectionDialog(
    currentTheme: String,
    onDismiss: () -> Unit,
    onThemeSelected: (String) -> Unit
) {
    val themes = listOf(
        "system" to "Системная",
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
                .fillMaxWidth(0.9f) // 90% ширины экрана
                .wrapContentHeight() // Автоматическая высота
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.SurfaceLight,
                contentColor = AppColors.TextPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Заголовок
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
                        .heightIn(max = 300.dp) // Максимальная высота списка
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

                Spacer(modifier = Modifier.height(20.dp))

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
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

/**
 * Компонент для отображения одного варианта темы
 */
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
                if (isSelected) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "Текущая тема",
                        style = MaterialTheme.typography.labelSmall,
                        color = AppColors.PrimaryBlue
                    )
                }
            }
        }
    }
}