// Файл: ui/screens/ecu_errors/EcuErrorsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens.ecu_errors

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
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
import com.alexbar3073.bluetoothcar.data.database.entities.EcuErrorEntity
import com.alexbar3073.bluetoothcar.ui.components.CompactTopBar
import com.alexbar3073.bluetoothcar.ui.components.TopBarButton
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.theme.verticalGradientBackground
import com.alexbar3073.bluetoothcar.ui.viewmodels.EcuDiagnosticItem
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel

/**
 * ТЕГ: Экран/ОшибкиЭБУ/Список
 *
 * ФАЙЛ: ui/screens/ecu_errors/EcuErrorsScreen.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/ecu_errors/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Экран отображения списка текущих ошибок ЭБУ.
 * Группирует ошибки в блоки с единым стилем, аналогично экрану настроек.
 *
 * ОТВЕТСТВЕННОСТЬ: Отображение списка активных ошибок в сгруппированном виде.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: MVVM / Jetpack Compose Screen
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Группировка связанных данных в карточки для улучшения читаемости.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * Использует: SharedViewModel.kt, EcuErrorEntity.kt
 * Вызывается из: SetupNavigation.kt
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcuErrorsScreen(
    navController: NavController,
    viewModel: SharedViewModel
) {
    // Получаем список всех диагностических элементов (одиночные + комбинации) из ViewModel.
    val diagnosticItems by viewModel.allDiagnosticItems.collectAsStateWithLifecycle()

    BluetoothCarTheme(themeMode = "dark") {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                // Компактная верхняя панель с заголовком и кнопкой назад
                CompactTopBar(
                    title = "ОШИБКИ ЭБУ",
                    titlePainterIcon = painterResource(id = R.drawable.ic_engine_48),
                    titleIconTint = AppColors.BluetoothDeviceConnected,
                    navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
                    onNavigationClick = { navController.popBackStack() },
                    rightContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Кнопка обновления списка ошибок (kl_get_dtc)
                            TopBarButton(
                                icon = Icons.Default.Refresh,
                                onClick = { viewModel.sendJsonCommand("""{"command":"kl_get_dtc"}""") },
                                contentDescription = "Обновить"
                            )
                            // Кнопка очистки ошибок в ЭБУ (kl_clear_dtc)
                            TopBarButton(
                                icon = Icons.Default.DeleteSweep,
                                onClick = { viewModel.sendJsonCommand("""{"command":"kl_clear_dtc"}""") },
                                contentDescription = "Очистить",
                                tint = AppColors.Error
                            )
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(verticalGradientBackground())
                    .padding(paddingValues)
            ) {
                if (diagnosticItems.isEmpty()) {
                    // Состояние пустого списка: ошибок нет
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Активных ошибок не обнаружено",
                            style = MaterialTheme.typography.bodyLarge,
                            color = AppColors.TextSecondary
                        )
                    }
                } else {
                    // Единый список диагностических элементов (одиночные и комбинации)
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Единая карточка со всеми найденными элементами
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                colors = CardDefaults.cardColors(containerColor = AppColors.SurfaceLight),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                    diagnosticItems.forEachIndexed { index, item ->
                                        when (item) {
                                            is EcuDiagnosticItem.Error -> {
                                                val error = item.error
                                                EcuErrorItem(
                                                    error = error,
                                                    onClick = {
                                                        // Переход к детальному описанию (единый экран для всех типов)
                                                        navController.navigate("ecu_error_detail/${error.code}")
                                                    }
                                                )
                                            }
                                        }
                                        
                                        // Разделитель между элементами внутри одной карточки
                                        if (index < diagnosticItems.size - 1) {
                                            HorizontalDivider(
                                                color = AppColors.SurfaceMedium,
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Элемент списка для отображения одиночной ошибки ЭБУ.
 * 
 * @param error Сущность ошибки из БД.
 * @param onClick Обработчик нажатия на элемент.
 */
@Composable
private fun EcuErrorItem(
    error: EcuErrorEntity,
    onClick: () -> Unit
) {
    // Определение цвета иконки в зависимости от приоритета ошибки
    // Приоритет 1 - критическая (красный), 2 - предупреждение (желтый)
    val iconColor = when {
        error.priority <= 1 -> AppColors.Error
        error.priority <= 2 -> AppColors.Warning
        else -> AppColors.TextPrimary
    }

    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            // Круглая подложка под иконку
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

            // Текстовая информация об ошибке
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
