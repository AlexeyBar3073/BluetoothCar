// Файл: ui/screens/settings/dialogs/OtaDialog.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
    AlertDialog(
        onDismissRequest = {
            // Разрешаем закрытие только если процесс не в фазе отправки
            if (state !is OtaManager.OtaState.Sending) {
                onDismiss()
            }
        },
        containerColor = AppColors.SurfaceLight,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.SystemUpdate,
                    contentDescription = null,
                    tint = AppColors.PrimaryBlue
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ОБНОВЛЕНИЕ БК",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (state) {
                    is OtaManager.OtaState.Idle, is OtaManager.OtaState.Validating -> {
                        CircularProgressIndicator(color = AppColors.PrimaryBlue)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Подготовка к обновлению...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AppColors.TextSecondary
                        )
                    }
                    
                    is OtaManager.OtaState.Sending -> {
                        OtaProgressContent(state)
                    }
                    
                    is OtaManager.OtaState.Success -> {
                        OtaStatusContent(
                            icon = Icons.Default.CheckCircle,
                            color = AppColors.Success,
                            title = "Успешно!",
                            message = "Прошивка передана. Дождитесь перезагрузки БК."
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
        },
        confirmButton = {
            if (state is OtaManager.OtaState.Success || state is OtaManager.OtaState.Error) {
                TextButton(onClick = onDismiss) {
                    Text("ЗАКРЫТЬ", color = AppColors.PrimaryBlue)
                }
            }
        }
    )
}

@Composable
private fun OtaProgressContent(state: OtaManager.OtaState.Sending) {
    val progressPercent = (state.progress * 100).toInt()
    
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            CircularProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxSize(),
                color = AppColors.PrimaryBlue,
                strokeWidth = 8.dp,
                trackColor = AppColors.SurfaceMedium,
            )
            Text(
                text = "$progressPercent%",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = AppColors.PrimaryBlue
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Передача данных...",
            style = MaterialTheme.typography.bodyMedium,
            color = AppColors.TextPrimary
        )
        
        Text(
            text = "Пакет ${state.currentPacket} из ${state.totalPackets}",
            style = MaterialTheme.typography.labelSmall,
            color = AppColors.TextTertiary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp),
            color = AppColors.PrimaryBlue,
            trackColor = AppColors.SurfaceMedium
        )
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
