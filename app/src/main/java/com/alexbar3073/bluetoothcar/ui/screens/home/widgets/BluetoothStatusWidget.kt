// Файл: ui/screens/home/widgets/BluetoothStatusWidget.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo

/**
 * ФАЙЛ: ui/screens/home/widgets/BluetoothStatusWidget.kt
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/widgets/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Виджет для отображения состояния Bluetooth соединения.
 * Использует готовые данные из ConnectionStatusInfo.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * ТОЛЬКО ИСПОЛЬЗУЕТ готовые данные из ConnectionStatusInfo без дополнительных запросов.
 *
 * @param modifier Модификатор для настройки виджета
 * @param connectionStatusInfo Полная структура данных о статусе подключения (гарантированно не null)
 * @param errorMessage Сообщение об ошибке (если есть)
 */
@Composable
fun BluetoothStatusWidget(
    modifier: Modifier = Modifier,
    connectionStatusInfo: ConnectionStatusInfo,
    errorMessage: String? = null
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = connectionStatusInfo.backgroundColor // Используем готовый цвет фона
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Иконка состояния - используем готовую иконку
            Icon(
                imageVector = connectionStatusInfo.icon,
                contentDescription = connectionStatusInfo.displayName,
                tint = connectionStatusInfo.iconColor, // Используем готовый цвет иконки
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Текст состояния - используем готовое отображаемое имя
            Text(
                text = connectionStatusInfo.displayName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = connectionStatusInfo.textColor // Используем готовый цвет текста
            )

            // Дополнительная информация
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = Color(0xFFC62828), // Красный для ошибок
                    maxLines = 2
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = connectionStatusInfo.description, // Используем готовое описание
                    fontSize = 14.sp,
                    color = Color(0xFF616161) // Серый для описаний
                )
            }
        }
    }
}