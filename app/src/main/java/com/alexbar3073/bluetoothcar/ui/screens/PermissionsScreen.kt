// ui/screens/PermissionsScreen.kt
package com.alexbar3073.bluetoothcar.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onRequestPermissions: () -> Unit,
    onSkip: () -> Unit
) {
    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()) // Добавляем прокрутку
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Иконка
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "Разрешения",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Заголовок
            Text(
                text = "Необходимые разрешения",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Объяснение для пользователя
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Для чего нужны разрешения?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Приложение использует Bluetooth для:\n" +
                                "• Поиска доступных устройств\n" +
                                "• Подключения к автомобилю\n" +
                                "• Отправки команд управления",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Детали разрешений
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Требуемые разрешения:",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Динамическое описание в зависимости от версии Android
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // Android 12+
                        PermissionItem(
                            title = "Bluetooth поиск (BLUETOOTH_SCAN)",
                            description = "Позволяет находить Bluetooth устройства рядом"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionItem(
                            title = "Bluetooth подключение (BLUETOOTH_CONNECT)",
                            description = "Позволяет подключаться к устройствам"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionItem(
                            title = "Точная геолокация",
                            description = "Необходима для работы Bluetooth на Android"
                        )
                    } else {
                        // Android 6.0-11
                        PermissionItem(
                            title = "Точная геолокация",
                            description = "Обязательное требование для поиска Bluetooth устройств"
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PermissionItem(
                            title = "Разрешения Bluetooth",
                            description = "Для управления Bluetooth адаптером"
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Важное примечание
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Важно",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Без этих разрешений приложение сможет работать только с тестовыми данными",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Кнопки действий
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Основная кнопка - запрос разрешений
                Button(
                    onClick = onRequestPermissions,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "Запросить все разрешения",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Вторичная кнопка - пропустить
                OutlinedButton(
                    onClick = onSkip,
                    modifier = Modifier.fillMaxWidth(0.8f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Text(
                        text = "Продолжить без разрешений",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Пояснение для кнопки "Пропустить"
            Text(
                text = "Вы сможете протестировать приложение с демо-устройствами",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp)
            )
        }
    }
}

@Composable
fun PermissionItem(
    title: String,
    description: String
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PermissionsScreenPreview() {
    BluetoothCarTheme {
        PermissionsScreen(
            onRequestPermissions = {},
            onSkip = {}
        )
    }
}
