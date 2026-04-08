// Файл: ui/screens/ecu_errors/EcuErrorDetailScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.ecu_errors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Экран/ОшибкиЭБУ/Детали
 *
 * ФАЙЛ: ui/screens/ecu_errors/EcuErrorDetailScreen.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/ecu_errors/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Экран детального отображения конкретной ошибки ЭБУ.
 * Предоставляет максимально подробную информацию об ошибке, включая причины, симптомы и советы.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuErrorDetailScreen(
    errorCode: String,
    navController: NavController,
    viewModel: SharedViewModel
) {
    // Получаем реактивный поток данных из БД. 
    // Unit используется как маркер "загрузка", чтобы избежать мгновенного показа "не найдено"
    val errorResult by remember(errorCode) { 
        viewModel.getEcuErrorByCode(errorCode) 
    }.collectAsState(initial = Unit)

    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Цвет иконки в заголовке зависит от приоритета (если данные загружены)
                val statusColor = if (errorResult is EcuErrorEntity) {
                    val entity = errorResult as EcuErrorEntity
                    if (entity.priority <= 1) AppColors.Error else AppColors.Warning
                } else AppColors.TextPrimary

                CompactTopBar(
                    title = "ОШИБКА $errorCode",
                    titleIcon = Icons.Default.Build,
                    titleIconTint = statusColor,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = { navController.popBackStack() }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                when (errorResult) {
                    is Unit -> {
                        // СОСТОЯНИЕ 1: Идет запрос к БД (предотвращает ложное "не найдено")
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = AppColors.PrimaryBlue)
                        }
                    }
                    null -> {
                        // СОСТОЯНИЕ 2: Запрос завершен, но кода действительно нет в БД
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.Info, null, tint = AppColors.Error, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Код $errorCode не найден", style = MaterialTheme.typography.titleMedium, color = Color.White)
                                Text("Обновите базу в настройках", style = MaterialTheme.typography.bodySmall, color = AppColors.ContentDetail)
                            }
                        }
                    }
                    is EcuErrorEntity -> {
                        // СОСТОЯНИЕ 3: Ошибка найдена
                        EcuErrorDetailContent(
                            error = errorResult as EcuErrorEntity, 
                            navController = navController,
                            errorCode = errorCode
                        )
                    }
                }
            }
        }
    }
}

/**
 * Отрисовывает подробное содержимое ошибки.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EcuErrorDetailContent(
    error: EcuErrorEntity,
    navController: NavController,
    errorCode: String
) {
    // Скролл-стейт, который принудительно сбрасывается при смене кода ошибки
    val scrollState = rememberScrollState()
    LaunchedEffect(errorCode) {
        scrollState.scrollTo(0)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // 1. КАРТОЧКА ЗАГОЛОВКА
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = error.shortDescription,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    StatusChip(
                        label = "ПРИОРИТЕТ: ${error.priority}",
                        color = if (error.priority <= 1) AppColors.Error else Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val canDriveColor = when {
                        error.canDrive.contains("НЕТ", true) -> AppColors.Error
                        error.canDrive.contains("ОГРАНИЧ", true) -> AppColors.Warning
                        else -> AppColors.Success
                    }
                    StatusChip(
                        label = "ДВИЖЕНИЕ: ${error.canDrive}",
                        color = canDriveColor,
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }

        // 2. ТЕХНИЧЕСКОЕ ОПИСАНИЕ
        DetailSectionHeader("ТЕХНИЧЕСКОЕ ОПИСАНИЕ")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = error.detailedDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.ContentDetail,
                modifier = Modifier.padding(16.dp)
            )
        }

        // 3. СВЯЗАННЫЕ ОШИБКИ (Кликабельные)
        if (error.relatedCodes.isNotEmpty()) {
            DetailSectionHeader("СВЯЗАННЫЕ ОШИБКИ")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                shape = MaterialTheme.shapes.medium
            ) {
                FlowRow(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    error.relatedCodes.forEach { relatedCode ->
                        AssistChip(
                            onClick = { 
                                // Выполняем навигацию. 
                                // Благодаря LaunchedEffect(errorCode) скролл сбросится вверх.
                                navController.navigate("ecu_error_detail/$relatedCode") 
                            },
                            label = { Text(relatedCode, color = Color.White) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = AppColors.SurfaceMedium,
                                labelColor = Color.White
                            ),
                            border = BorderStroke(1.dp, AppColors.PrimaryBlue.copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        // 4. СИМПТОМЫ
        if (error.symptoms.isNotEmpty()) {
            DetailSectionHeader("СИМПТОМЫ")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    error.symptoms.forEachIndexed { index, symptom ->
                        DetailRow(text = symptom, icon = Icons.Default.Info)
                        if (index < error.symptoms.size - 1) {
                            HorizontalDivider(
                                color = AppColors.SurfaceMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. ПРИЧИНЫ И РЕШЕНИЯ
        if (error.causes.isNotEmpty()) {
            DetailSectionHeader("ВОЗМОЖНЫЕ ПРИЧИНЫ И РЕШЕНИЯ")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    error.causes.forEachIndexed { index, cause ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${cause.probability}%",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = AppColors.PrimaryBlue
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = cause.cause,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "ДЕЙСТВИЕ: ${cause.action}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.ContentDetail
                            )
                        }
                        if (index < error.causes.size - 1) {
                            HorizontalDivider(
                                color = AppColors.SurfaceMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 6. ИНСТРУМЕНТЫ
        if (error.toolsNeeded.isNotEmpty()) {
            DetailSectionHeader("НЕОБХОДИМЫЕ ИНСТРУМЕНТЫ")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    error.toolsNeeded.forEachIndexed { index, tool ->
                        DetailRow(text = tool, icon = Icons.Default.Build)
                        if (index < error.toolsNeeded.size - 1) {
                            HorizontalDivider(
                                color = AppColors.SurfaceMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 7. СОВЕТ ЭКСПЕРТА
        if (error.clubExpertNote.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Warning.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "СОВЕТ КЛУБНОГО ЭКСПЕРТА", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold, 
                        color = AppColors.Warning
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = error.clubExpertNote,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun DetailSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = AppColors.PrimaryBlue,
        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun DetailRow(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = AppColors.PrimaryBlue
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyMedium, color = Color.White)
    }
}

@Composable
private fun StatusChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.2f),
        shape = RoundedCornerShape(4.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.White
        )
    }
}
