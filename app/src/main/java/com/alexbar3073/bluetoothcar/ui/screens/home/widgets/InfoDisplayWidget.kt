// InfoDisplayWidget.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Данные для отображения в информационном дисплее.
 * Можно легко расширять под новые параметры автомобиля.
 */
data class CarInfo(
    val temperature: Int = 90,      // °C
    val fuelLevel: Int = 75,      // %
    val oilPressure: Float = 3.2f, // bar
    val gear: String = "D",        // текущая передача
    val range: Int = 420          // запас хода, км
)

/**
 * Информационный дисплей — отображает ключевые параметры автомобиля в компактном виде.
 *
 * @param modifier Модификатор для настройки внешнего вида/размера (первый опциональный параметр)
 * @param carInfo Данные автомобиля для отображения
 */
@Composable
fun InfoDisplayWidget(
    modifier: Modifier = Modifier,
    carInfo: CarInfo = CarInfo()
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Заголовок блока
            Text(
                "Информация",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary
            )

            HorizontalDivider( // Исправлено: Divider → HorizontalDivider
                modifier = Modifier.fillMaxWidth(0.8f),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            )

            // Сетка параметров (2 колонки)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Левая колонка
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoItem(
                        label = "Темп.",
                        value = "${carInfo.temperature}°C",
                        valueColor = if (carInfo.temperature > 100) Color.Red else Color.Green
                    )
                    InfoItem(
                        label = "Топливо",
                        value = "${carInfo.fuelLevel}%",
                        valueColor = if (carInfo.fuelLevel < 20) Color.Red else Color.Green
                    )
                }

                // Правая колонка
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoItem(
                        label = "Давление",
                        value = "${carInfo.oilPressure} bar",
                        valueColor = Color.Blue
                    )
                    InfoItem(
                        label = "Передача",
                        value = carInfo.gear,
                        valueColor = if (carInfo.gear == "N") Color.Yellow else Color.Green
                    )
                    InfoItem(
                        label = "Запас хода",
                        value = "${carInfo.range} км",
                        valueColor = Color.Gray
                    )
                }
            } // Закрытие Row
        } // Закрытие Column
    } // Закрытие Card
} // Закрытие функции InfoDisplayWidget


@Composable
fun InfoItem(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
            fontSize = 12.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
