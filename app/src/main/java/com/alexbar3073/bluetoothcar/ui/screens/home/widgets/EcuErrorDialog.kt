// Файл: ui/screens/home/widgets/EcuErrorDialog.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity

/**
 * ТЕГ: UI/Widgets/EcuErrorDialog
 *
 * ФАЙЛ: ui/screens/home/widgets/EcuErrorDialog.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/widgets/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Диалоговое окно для отображения детальной информации об ошибках ЭБУ.
 * Показывает список ошибок с их кодами, описаниями и рекомендациями по устранению.
 *
 * ОТВЕТСТВЕННОСТЬ: Визуализация данных справочника ошибок для пользователя.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Jetpack Compose Widget
 */

@Composable
fun EcuErrorDialog(
    errors: List<EcuErrorEntity>,
    onDismiss: () -> Unit
) {
    // Используем стандартный Dialog для перекрытия всего контента
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Заголовок диалога
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Yellow,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ошибки ЭБУ (${errors.size})",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Список ошибок
                if (errors.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Нет данных по текущим кодам ошибок", style = MaterialTheme.typography.bodyMedium)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(errors) { error ->
                            EcuErrorItem(error)
                        }
                    }
                }

                // Кнопка закрытия
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Text("ЗАКРЫТЬ")
                }
            }
        }
    }
}

@Composable
private fun EcuErrorItem(error: EcuErrorEntity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Код и заголовок
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = error.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = error.shortDescription,
                    style = MaterialTheme.typography.titleSmall,
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Детальное описание
            Text(
                text = error.detailedDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Блок "Возможность движения"
            Row(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "Ехать можно: ",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = error.canDrive,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (error.canDrive.contains("НЕТ", true)) Color.Red else Color.Green
                )
            }

            // Блок причин (если есть)
            if (error.causes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(alpha = 0.2f)
                Text(
                    text = "Возможные причины:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                error.causes.forEach { cause ->
                    Column(modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(
                            text = "• ${cause.cause} (${cause.probability}%)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            text = "  Действие: ${cause.action}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
            
            // Заметка эксперта
            if (error.clubExpertNote.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Color.Yellow.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Совет эксперта: ${error.clubExpertNote}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}
