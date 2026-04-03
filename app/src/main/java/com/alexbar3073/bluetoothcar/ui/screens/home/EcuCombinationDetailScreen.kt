// Файл: ui/screens/home/EcuCombinationDetailScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.data.database.entities.EcuCombinationEntity
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Экран/ОшибкиЭБУ/ДеталиКомбинации
 *
 * ФАЙЛ: ui/screens/home/EcuCombinationDetailScreen.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Экран отображения результатов интеллектуального анализа нескольких ошибок.
 * Предоставляет пользователю "общую картину" поломки, основанную на сочетании кодов.
 *
 * ОТВЕТСТВЕННОСТЬ: Визуализация комплексного экспертного заключения.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: MVVM / Jetpack Compose Screen
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Группировка данных по смысловым блокам (анализ, симптомы, решение).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Использует: SharedViewModel.kt, EcuCombinationEntity.kt / 
 * Вызывается из: SetupNavigation.kt, EcuErrorsScreen.kt
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuCombinationDetailScreen(
    combinationId: Int,
    navController: NavController,
    viewModel: SharedViewModel
) {
    // Подписка на поток активных комбинаций для поиска нужной по ID
    val combinations by viewModel.activeCombinations.collectAsState()
    val combination = combinations.find { it.id == combinationId }

    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Заголовок в стиле экспертной системы
                CompactTopBar(
                    title = "КОМПЛЕКСНЫЙ АНАЛИЗ",
                    titleIcon = Icons.Default.Star,
                    titleIconTint = AppColors.PrimaryBlue,
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
                // Обработка случая, когда комбинация исчезла из списка активных (напр. после сброса)
                if (combination == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Заключение не найдено", color = Color.White)
                    }
                } else {
                    // Отрисовка основного контента
                    EcuCombinationDetailContent(combination, navController)
                }
            }
        }
    }
}

/**
 * Рендерит вертикальный список секций с описанием комбинации.
 *
 * @param combination Данные о комбинации из БД
 * @param navController Контроллер навигации для перехода к одиночным ошибкам
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EcuCombinationDetailContent(
    combination: EcuCombinationEntity,
    navController: NavController
) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(20.dp)
    ) {
        // БЛОК 1: Главная карточка с заголовком и списком входящих кодов
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = combination.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выявлено по совокупности:",
                    style = MaterialTheme.typography.labelSmall,
                    color = AppColors.PrimaryBlue
                )
                Spacer(modifier = Modifier.height(12.dp))
                // Отображение кодов, из которых состоит комбинация, в виде чипов
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    combination.codes.forEach { code ->
                        SuggestionChip(
                            onClick = { navController.navigate("ecu_error_detail/$code") },
                            label = { Text(code) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // БЛОК 2: Подробный анализ ситуации
        DetailSectionHeader("АНАЛИЗ СИТУАЦИИ")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = combination.detailedDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = AppColors.ContentDetail,
                modifier = Modifier.padding(16.dp)
            )
        }

        // БЛОК 3: Характерные симптомы
        if (combination.symptoms.isNotEmpty()) {
            DetailSectionHeader("ХАРАКТЕРНЫЕ СИМПТОМЫ")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    combination.symptoms.forEachIndexed { index, symptom ->
                        DetailRow(text = symptom, icon = Icons.Default.Info)
                        // Разделитель между элементами списка внутри карточки
                        if (index < combination.symptoms.size - 1) {
                            HorizontalDivider(
                                color = AppColors.SurfaceMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // БЛОК 4: Рекомендации эксперта по устранению
        if (combination.causes.isNotEmpty()) {
            DetailSectionHeader("РЕКОМЕНДАЦИИ ПО УСТРАНЕНИЮ")
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    combination.causes.forEachIndexed { index, cause ->
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Вероятность причины в процентах
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
                            // Описание конкретного действия
                            Text(
                                text = "ДЕЙСТВИЕ: ${cause.action}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AppColors.ContentDetail
                            )
                        }
                        if (index < combination.causes.size - 1) {
                            HorizontalDivider(
                                color = AppColors.SurfaceMedium,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            }
        }

        // БЛОК 5: Заключение эксперта (выделенный блок)
        if (combination.clubExpertNote.isNotEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppColors.Warning.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        "ЗАКЛЮЧЕНИЕ ЭКСПЕРТА", 
                        style = MaterialTheme.typography.labelSmall, 
                        fontWeight = FontWeight.Bold, 
                        color = AppColors.Warning
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = combination.clubExpertNote,
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

/** Вспомогательная функция для отрисовки заголовка секции */
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

/** Вспомогательная функция для отрисовки строки списка с иконкой */
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
