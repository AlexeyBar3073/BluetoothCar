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
import androidx.compose.ui.window.Dialog
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.viewmodels.EcuDiagnosticItem

/**
 * ТЕГ: UI/Widgets/EcuErrorDialog
 *
 * ФАЙЛ: ui/screens/home/widgets/EcuErrorDialog.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/widgets/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Диалоговое окно для отображения информации об ошибках ЭБУ.
 * Поддерживает отображение списка диагностических данных.
 *
 * ОТВЕТСТВЕННОСТЬ: Визуализация данных справочника ошибок для пользователя.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Jetpack Compose Widget (Master-Detail inside Dialog)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Простота и наглядность вывода расшифровки кодов.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: (Использует: SharedViewModel.kt / Вызывается из: Dashboards)
 */

@Composable
fun EcuErrorDialog(
    diagnosticItems: List<EcuDiagnosticItem>,
    onDismiss: () -> Unit
) {
    // Состояние для хранения выбранного элемента для детального просмотра
    var selectedItem by remember { mutableStateOf<EcuDiagnosticItem?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = AppColors.SurfaceMedium,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // 1. ДИНАМИЧЕСКИЙ ЗАГОЛОВОК И КНОПКА НАЗАД
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    if (selectedItem != null) {
                        IconButton(onClick = { selectedItem = null }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = Color.White
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = AppColors.Error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedItem == null) "ДИАГНОСТИКА" else "ПОДРОБНОСТИ",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // 2. КОНТЕНТ С АНИМАЦИЕЙ ПЕРЕХОДА
                Box(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = selectedItem,
                        label = "diagnostic_navigation"
                    ) { item ->
                        if (item == null) {
                            // ЭКРАН 1: СПИСОК
                            if (diagnosticItems.isEmpty()) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(
                                        "Активных проблем не обнаружено",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = AppColors.TextSecondary
                                    )
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(diagnosticItems) { diagnosticItem ->
                                        when (diagnosticItem) {
                                            is EcuDiagnosticItem.SingleError -> {
                                                EcuErrorListItem(
                                                    error = diagnosticItem.error,
                                                    onClick = { selectedItem = diagnosticItem }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            // ЭКРАН 2: ДЕТАЛЬНЫЙ ПРОСМОТР
                            when (item) {
                                is EcuDiagnosticItem.SingleError -> EcuErrorFullDetail(item.error)
                            }
                        }
                    }
                }

                // 3. КНОПКА ЗАКРЫТИЯ
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.SurfaceLight)
                ) {
                    Text("ЗАКРЫТЬ", color = Color.White)
                }
            }
        }
    }
}

/**
 * Элемент списка для одиночной ошибки.
 */
@Composable
private fun EcuErrorListItem(
    error: EcuErrorEntity,
    onClick: () -> Unit
) {
    val iconColor = when {
        error.priority <= 1 -> AppColors.Error
        error.priority <= 2 -> AppColors.Warning
        else -> AppColors.TextPrimary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = error.code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = error.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ContentDetail,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Детальная информация об одиночной ошибке.
 */
@Composable
private fun EcuErrorFullDetail(error: EcuErrorEntity) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = error.code,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = AppColors.PrimaryBlue
        )
        Text(
            text = error.shortDescription,
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            InfoChip(
                label = "Приоритет: ${error.priority}",
                color = if (error.priority <= 1) AppColors.Error else AppColors.TextSecondary,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            InfoChip(
                label = "Движение: ${error.canDrive}",
                color = if (error.canDrive.contains("НЕТ", true)) AppColors.Error else AppColors.BluetoothDeviceConnected,
                modifier = Modifier.weight(1.5f)
            )
        }

        DetailSection("Техническое описание", error.detailedDescription)

        if (error.symptoms.isNotEmpty()) {
            DetailSection("Наблюдаемые симптомы", error.symptoms, Icons.Default.Info)
        }

        if (error.causes.isNotEmpty()) {
            SectionHeader("Возможные причины и решения")
            error.causes.forEach { cause ->
                Card(
                    modifier = Modifier.padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${cause.probability}%",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.PrimaryBlue
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = cause.cause, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                        }
                        Text(
                            text = "Решение: ${cause.action}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp),
                            color = AppColors.ContentDetail
                        )
                    }
                }
            }
        }

        if (error.toolsNeeded.isNotEmpty()) {
            DetailSection("Необходимый инструмент", error.toolsNeeded, Icons.Default.Build)
        }

        if (error.clubExpertNote.isNotEmpty()) {
            ExpertNote(error.clubExpertNote)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Блок заметки эксперта.
 */
@Composable
private fun ExpertNote(note: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(AppColors.PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Column {
            Text(
                "СОВЕТ КЛУБНОГО ЭКСПЕРТА", 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Bold, 
                color = AppColors.PrimaryBlue
            )
            Text(
                text = note,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            )
        }
    }
}

/** Вспомогательный компонент для заголовка секции */
@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.PrimaryBlue,
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
}

/** Секция с текстом */
@Composable
private fun DetailSection(title: String, content: String) {
    SectionHeader(title)
    Text(text = content, style = MaterialTheme.typography.bodyMedium, color = Color.White)
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
            Icon(
                imageVector = icon, 
                contentDescription = null, 
                modifier = Modifier.size(14.dp), 
                tint = AppColors.ContentDetail
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = item, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
    }
}

/** Маленький индикатор (чип) для статусов */
@Composable
private fun InfoChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            color = Color.White
        )
    }
}
