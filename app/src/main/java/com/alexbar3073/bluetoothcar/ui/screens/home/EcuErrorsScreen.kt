// Файл: ui/screens/home/EcuErrorsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.database.entities.EcuCombinationEntity
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Экран/ОшибкиЭБУ/Список
 *
 * ФАЙЛ: ui/screens/home/EcuErrorsScreen.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Экран отображения списка текущих ошибок ЭБУ и экспертных комбинаций.
 * Позволяет пользователю увидеть все активные ошибки и выявленные сложные проблемы.
 *
 * ОТВЕТСТВЕННОСТЬ: Отображение списка активных ошибок и комбинаций.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuErrorsScreen(
    navController: NavController,
    viewModel: SharedViewModel
) {
    // Получаем список активных ошибок и комбинаций из ViewModel
    val errors by viewModel.activeEcuErrors.collectAsStateWithLifecycle()
    val combinations by viewModel.activeCombinations.collectAsStateWithLifecycle()

    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                CompactTopBar(
                    title = "ОШИБКИ ЭБУ",
                    titlePainterIcon = painterResource(id = R.drawable.ic_engine_48),
                    titleIconTint = AppColors.BluetoothDeviceConnected,
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
                if (errors.isEmpty() && combinations.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Активных ошибок не обнаружено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextSecondary
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 1. ОТОБРАЖАЕМ КОМБИНАЦИИ (Экспертные заключения имеют высший приоритет)
                        items(combinations) { combination ->
                            EcuCombinationCard(
                                combination = combination,
                                onClick = {
                                    navController.navigate("ecu_combination_detail/${combination.id}")
                                }
                            )
                        }

                        // 2. ОТОБРАЖАЕМ ОДИНОЧНЫЕ ОШИБКИ
                        items(errors) { error ->
                            EcuErrorCard(
                                error = error,
                                onClick = {
                                    navController.navigate("ecu_error_detail/${error.code}")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Карточка комбинации ошибок.
 * Выделена синим контуром и иконкой звезды для привлечения внимания.
 */
@Composable
private fun EcuCombinationCard(
    combination: EcuCombinationEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.SurfaceLight
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.PrimaryBlue.copy(alpha = 0.5f))
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        AppColors.PrimaryBlue.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = AppColors.PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = combination.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = combination.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ContentDetail,
                    maxLines = 2
                )
            }
        }
    }
}

/**
 * Карточка одиночной ошибки.
 */
@Composable
private fun EcuErrorCard(
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
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = AppColors.SurfaceLight
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        AppColors.SurfaceMedium,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = error.code,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = error.shortDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.ContentDetail,
                    maxLines = 2
                )
            }
        }
    }
}
