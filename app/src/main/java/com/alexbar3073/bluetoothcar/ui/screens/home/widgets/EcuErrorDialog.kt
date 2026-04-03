// Файл: ui/screens/home/widgets/EcuErrorDialog.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
 * Реализует двухуровневую навигацию: 
 * 1. Список всех обнаруженных ошибок (кратко).
 * 2. Детальная карточка выбранной ошибки со всеми техническими подробностями.
 *
 * ОТВЕТСТВЕННОСТЬ: Визуализация данных справочника ошибок для пользователя.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Jetpack Compose Widget (Master-Detail inside Dialog)
 */
@Composable
fun EcuErrorDialog(
    errors: List<EcuErrorEntity>,
    onDismiss: () -> Unit
) {
    // Состояние для хранения выбранной ошибки. Если null — показываем список.
    var selectedError by remember { mutableStateOf<EcuErrorEntity?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // ДИНАМИЧЕСКИЙ ЗАГОЛОВОК И КНОПКА НАЗАД
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (selectedError != null) {
                        IconButton(onClick = { selectedError = null }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color.Yellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedError == null) "Ошибки ЭБУ (${errors.size})" else "Детали ошибки",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // КОНТЕНТ С АНИМАЦИЕЙ ПЕРЕХОДА
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedError,
                        label = "error_navigation"
                    ) { error ->
                        if (error == null) {
                            // ЭКРАН 1: СПИСОК ОШИБОК
                            if (errors.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("Нет данных по кодам", style = MaterialTheme.typography.bodyMedium)
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(errors) { item ->
                                        EcuErrorListItem(
                                            error = item,
                                            onClick = { selectedError = item }
                                        )
                                    }
                                }
                            }
                        } else {
                            // ЭКРАН 2: ПОЛНАЯ ИНФОРМАЦИЯ
                            EcuErrorFullDetail(error)
                        }
                    }
                }

                // КНОПКА ЗАКРЫТИЯ
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

/**
 * Компактный элемент списка для главного экрана диалога.
 * Отображает только код и краткое название.
 */
@Composable
private fun EcuErrorListItem(
    error: EcuErrorEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = error.code,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(80.dp)
            )
            Text(
                text = error.shortDescription,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

/**
 * Полноэкранный (в рамках диалога) контент с максимальной детализацией ошибки.
 * Включает скроллинг и все доступные поля из базы данных.
 */
@Composable
private fun EcuErrorFullDetail(error: EcuErrorEntity) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(end = 4.dp)
    ) {
        // Блок основного заголовка
        Text(
            text = error.code,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = error.shortDescription,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Критичность и возможность движения
        Row(modifier = Modifier.fillMaxWidth()) {
            InfoChip(
                label = "Приоритет: ${error.priority}",
                color = if (error.priority <= 1) Color.Red else Color.Gray,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            val canDriveColor = when {
                error.canDrive.contains("НЕТ", true) -> Color.Red
                error.canDrive.contains("ОГРАНИЧ", true) -> Color.Yellow
                else -> Color.Green
            }
            InfoChip(
                label = "Движение: ${error.canDrive}",
                color = canDriveColor,
                modifier = Modifier.weight(1.5f)
            )
        }

        DetailSection("Техническое описание", error.detailedDescription)

        // СИМПТОМЫ
        if (error.symptoms.isNotEmpty()) {
            DetailSection(
                title = "Наблюдаемые симптомы",
                items = error.symptoms,
                icon = Icons.Default.Info
            )
        }

        // ВОЗМОЖНЫЕ ПРИЧИНЫ (с вероятностью)
        if (error.causes.isNotEmpty()) {
            SectionHeader("Возможные причины и решения")
            error.causes.forEach { cause ->
                Card(
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${cause.probability}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cause.cause, style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            text = "Решение: ${cause.action}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // НЕОБХОДИМЫЕ ИНСТРУМЕНТЫ
        if (error.toolsNeeded.isNotEmpty()) {
            DetailSection(
                title = "Что понадобится для ремонта",
                items = error.toolsNeeded,
                icon = Icons.Default.Build
            )
        }

        // СВЯЗАННЫЕ КОДЫ
        if (error.relatedCodes.isNotEmpty()) {
            DetailSection(
                title = "Связанные ошибки",
                content = error.relatedCodes.joinToString(", ")
            )
        }

        // ЗАМЕТКА ЭКСПЕРТА (выделенная)
        if (error.clubExpertNote.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Yellow.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Text("СОВЕТ КЛУБНОГО ЭКСПЕРТА", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.Yellow)
                    Text(
                        text = error.clubExpertNote,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/** Вспомогательный компонент для заголовка секции */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/** Секция с текстом */
@Composable
private fun DetailSection(title: String, content: String) {
    SectionHeader(title)
    Text(text = content, style = MaterialTheme.typography.bodyMedium)
}

/** Секция со списком элементов и иконкой */
@Composable
private fun DetailSection(title: String, items: List<String>, icon: ImageVector) {
    SectionHeader(title)
    items.forEach { item ->
        Row(
            modifier = Modifier.padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** Маленький индикатор (чип) для статусов */
@Composable
private fun InfoChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White
        )
    }
}
