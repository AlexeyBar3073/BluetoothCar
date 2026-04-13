// Файл: ui/screens/settings/dialogs/OtaDialog.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alexbar3073.bluetoothcar.data.bluetooth.OtaManager
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * ТЕГ: OTA/Dialog
 * 
 * ФАЙЛ: ui/screens/settings/dialogs/OtaDialog.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/settings/dialogs/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Диалоговое окно процесса OTA обновления прошивки.
 * Визуализирует прогресс, ошибки и успешное завершение.
 * 
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Отображение процентов выполнения.
 * 2. Визуализация номера текущего пакета из общего количества.
 * 3. Вывод сообщений об ошибках (двигатель запущен, файл неверный).
 * 4. Запрет закрытия во время критической фазы передачи (опционально).
 */
@Composable
fun OtaDialog(
    state: OtaManager.OtaState,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {
            // Разрешаем закрытие только если процесс не в фазе отправки
            if (state !is OtaManager.OtaState.Sending) {
                onDismiss()
            }
        },
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Заголовок
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = AppColors.PrimaryBlue
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "ОБНОВЛЕНИЕ БК",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary
                    )
                }

                // Контент
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when (state) {
                        is OtaManager.OtaState.Idle, 
                        is OtaManager.OtaState.Validating,
                        is OtaManager.OtaState.WaitingForInit -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = AppColors.PrimaryBlue)
                                Spacer(modifier = Modifier.height(16.dp))
                                val statusText = when(state) {
                                    is OtaManager.OtaState.WaitingForInit -> "Ожидание готовности БК..."
                                    else -> "Подготовка к обновлению..."
                                }
                                Text(
                                    statusText,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextSecondary
                                )
                            }
                        }
                        
                        is OtaManager.OtaState.Sending -> {
                            OtaProgressContent(state)
                        }

                        is OtaManager.OtaState.WaitingForReboot -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(vertical = 16.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = AppColors.PrimaryBlue,
                                    strokeWidth = 6.dp,
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Text(
                                    "Прошивка передана.",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.TextPrimary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Ожидание перезагрузки БК...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = AppColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        is OtaManager.OtaState.Success -> {
                            OtaStatusContent(
                                icon = Icons.Default.CheckCircle,
                                color = AppColors.Success,
                                title = "Успешно!",
                                message = "Обновление завершено.\nНовая версия: ${state.newVersion}"
                            )
                        }
                        
                        is OtaManager.OtaState.Error -> {
                            OtaStatusContent(
                                icon = Icons.Default.Error,
                                color = AppColors.Error,
                                title = "Ошибка",
                                message = state.message
                            )
                        }
                    }
                }

                // Кнопки
                if (state is OtaManager.OtaState.Success || state is OtaManager.OtaState.Error) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AppColors.PrimaryBlue,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                "ЗАКРЫТЬ",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OtaProgressContent(state: OtaManager.OtaState.Sending) {
    val progressPercent = (state.progress * 100).toInt()
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Линейный индикатор передачи
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = AppColors.PrimaryBlue,
            trackColor = AppColors.SurfaceMedium,
            strokeCap = StrokeCap.Round
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Текстовая информация под индикатором
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (state.currentPacket >= state.totalPackets) 
                        "Все данные переданы. Завершение..." 
                    else 
                        "Передача данных...",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary
                )
                Text(
                    text = if (state.currentPacket >= state.totalPackets)
                        "Ожидание подтверждения от БК"
                    else
                        "Пакет ${state.currentPacket} из ${state.totalPackets}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AppColors.TextTertiary
                )
            }
            
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryBlue
            )
        }
    }
}

@Composable
private fun OtaStatusContent(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    message: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextSecondary,
            textAlign = TextAlign.Center
        )
    }
}
