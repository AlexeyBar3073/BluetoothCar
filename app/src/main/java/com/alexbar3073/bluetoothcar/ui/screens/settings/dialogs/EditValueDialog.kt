package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

/**
 * Диалог для редактирования числовых значений настроек
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditValueDialog(
    data: EditDialogData,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var currentValue by remember { mutableStateOf(data.currentValue.toString()) }
    var error by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f) // 90% ширины экрана
                .wrapContentHeight() // Автоматическая высота
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AppColors.SurfaceLight,
                contentColor = AppColors.TextPrimary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Заголовок
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // Поле ввода
                OutlinedTextField(
                    value = currentValue,
                    onValueChange = { newValue ->
                        if (newValue.matches(Regex("^\\d*\\.?\\d*\$")) || newValue.isEmpty()) {
                            currentValue = newValue
                            error = null
                        }
                    },
                    label = {
                        Text(
                            "Значение (${data.unit})",
                            color = AppColors.TextSecondary
                        )
                    },
                    singleLine = true,
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(
                                error!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        } else {
                            Text(
                                "Диапазон: ${data.minValue} - ${data.maxValue} ${data.unit}",
                                color = AppColors.TextTertiary,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = AppColors.SurfaceMedium,
                        unfocusedContainerColor = AppColors.SurfaceMedium,
                        focusedTextColor = AppColors.TextPrimary,
                        unfocusedTextColor = AppColors.TextPrimary,
                        focusedLabelColor = AppColors.TextSecondary,
                        unfocusedLabelColor = AppColors.TextTertiary,
                        cursorColor = AppColors.PrimaryBlue,
                        focusedIndicatorColor = AppColors.PrimaryBlue,
                        unfocusedIndicatorColor = AppColors.SurfaceMedium,
                        errorIndicatorColor = MaterialTheme.colorScheme.error,
                        errorLabelColor = MaterialTheme.colorScheme.error,
                        errorTextColor = MaterialTheme.colorScheme.error
                    ),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Кнопки
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.padding(end = 12.dp)
                    ) {
                        Text(
                            "Отмена",
                            color = AppColors.TextSecondary,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Button(
                        onClick = {
                            when {
                                currentValue.isEmpty() -> {
                                    error = "Введите значение"
                                }

                                currentValue.toFloatOrNull() == null -> {
                                    error = "Некорректное число"
                                }

                                currentValue.toFloat() < data.minValue -> {
                                    error = "Минимальное значение: ${data.minValue}"
                                }

                                currentValue.toFloat() > data.maxValue -> {
                                    error = "Максимальное значение: ${data.maxValue}"
                                }

                                else -> {
                                    onConfirm(currentValue.toFloat())
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.PrimaryBlue,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            "Сохранить",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}