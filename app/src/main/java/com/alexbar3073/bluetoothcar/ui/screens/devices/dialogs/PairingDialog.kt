package com.alexbar3073.bluetoothcar.ui.screens.devices.dialogs

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import kotlinx.coroutines.delay

/**
 * Состояния процесса сопряжения для UI.
 */
sealed class PairingState {
    object Idle : PairingState()
    object Pairing : PairingState()
    object Connected : PairingState()
    data class Failed(val message: String) : PairingState()
}

/**
 * ФАЙЛ: PairingDialog.kt
 */
@Composable
fun PairingDialog(
    pairingState: PairingState,
    deviceName: String,
    onDismiss: () -> Unit,
    onRetry: () -> Unit,
    onCancel: () -> Unit
) {
    BluetoothCarTheme {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = MaterialTheme.shapes.large,
                color = AppColors.DialogBackground,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Иконка состояния
                    val (icon, iconColor, iconBackground) = when (pairingState) {
                        is PairingState.Pairing -> Triple(
                            Icons.Default.Bluetooth,
                            AppColors.PrimaryBlue,
                            AppColors.TransparentPrimary
                        )

                        is PairingState.Connected -> Triple(
                            Icons.Default.BluetoothConnected,
                            Color(0xFF4CAF50),
                            Color(0xFFE8F5E9)
                        )

                        is PairingState.Failed -> Triple(
                            Icons.Default.Error,
                            AppColors.Error,
                            AppColors.Error.copy(alpha = 0.1f)
                        )

                        else -> Triple(
                            Icons.Default.Bluetooth,
                            AppColors.TextSecondary,
                            AppColors.SurfaceMedium
                        )
                    }

                    // Анимация для процесса сопряжения
                    var rotation by remember { mutableFloatStateOf(0f) }

                    LaunchedEffect(pairingState is PairingState.Pairing) {
                        if (pairingState is PairingState.Pairing) {
                            while (true) {
                                rotation += 30f
                                if (rotation >= 360f) rotation = 0f
                                delay(100)
                            }
                        }
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = iconBackground,
                                shape = androidx.compose.foundation.shape.CircleShape
                            )
                    ) {
                        val animatedRotation by animateFloatAsState(
                            targetValue = if (pairingState is PairingState.Pairing) rotation else 0f,
                            label = "rotation"
                        )

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier
                                .size(40.dp)
                                .rotate(animatedRotation)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Заголовок
                    Text(
                        text = when (pairingState) {
                            is PairingState.Pairing -> "СОПРЯЖЕНИЕ"
                            is PairingState.Connected -> "СОПРЯЖЕНИЕ УСПЕШНО"
                            is PairingState.Failed -> "ОШИБКА СОПРЯЖЕНИЯ"
                            else -> "СОПРЯЖЕНИЕ"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Имя устройства
                    Text(
                        text = deviceName,
                        fontSize = 16.sp,
                        color = AppColors.PrimaryBlue,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Сообщение состояния
                    Text(
                        text = when (pairingState) {
                            is PairingState.Pairing ->
                                "Идет сопряжение с устройством...\nЭто может занять несколько секунд."

                            is PairingState.Connected ->
                                "Устройство успешно сопряжено!\nТеперь можно подключаться."

                            is PairingState.Failed ->
                                pairingState.message

                            else -> "Подготовка к сопряжению..."
                        },
                        fontSize = 14.sp,
                        color = when (pairingState) {
                            is PairingState.Connected -> Color(0xFF2E7D32)
                            is PairingState.Failed -> AppColors.Error
                            else -> AppColors.TextSecondary
                        },
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Кнопки в зависимости от состояния
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (pairingState) {
                            is PairingState.Pairing -> {
                                Button(
                                    onClick = onCancel,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.Error,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ОТМЕНА")
                                }
                            }

                            is PairingState.Failed -> {
                                OutlinedButton(
                                    onClick = onCancel,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = AppColors.TextPrimary
                                    ),
                                    border = ButtonDefaults.outlinedButtonBorder(enabled = true)
                                        .copy(
                                            width = 1.dp
                                        )
                                ) {
                                    Text("ЗАКРЫТЬ")
                                }

                                Button(
                                    onClick = onRetry,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.PrimaryBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("ПОВТОРИТЬ")
                                }
                            }

                            is PairingState.Connected -> {
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.PrimaryBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("ГОТОВО")
                                }
                            }

                            else -> {
                                Button(
                                    onClick = onCancel,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = AppColors.PrimaryBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text("ЗАКРЫТЬ")
                                }
                            }
                        }
                    }

                    // Индикатор прогресса для сопряжения
                    if (pairingState is PairingState.Pairing) {
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = AppColors.PrimaryBlue
                        )
                    }
                }
            }
        }
    }
}
