// TachometerWidget.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Тахометр — отображает обороты двигателя с анимацией и визуальной индикацией зоны опасности.
 *
 * @param modifier Модификатор для настройки внешнего вида/размера (первый опциональный параметр)
 * @param rpm Текущие обороты двигателя (об/мин)
 * @param trackColor Цвет фона дуги тахометра
 */
@Deprecated("Используйте DashboardType4 для отображения данных автомобиля")
@Composable
fun TachometerWidget( // Если конфликт сохраняется — переименуйте в TachometerWidgetV2 или ModernTachometer
    modifier: Modifier = Modifier,
    rpm: Int,
    trackColor: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    val animatedRpm by animateFloatAsState(
        targetValue = rpm.toFloat(),
        animationSpec = tween(durationMillis = 200),
        label = "rpm_animation"
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Обороты", style = MaterialTheme.typography.bodyMedium)

        Box(modifier = Modifier.size(200.dp)) {
            TachometerCanvas(rpm = animatedRpm, trackColor = trackColor)
            Text(
                "${rpm / 1000}.${(rpm % 1000) / 100}",
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
                color = if (rpm > 5000) Color.Red else Color.Green
            )
        }

        Text("тыс. об/мин", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun TachometerCanvas(rpm: Float, trackColor: Color) {
    Canvas(
        modifier = Modifier.fillMaxSize(),
        contentDescription = "Тахометр: ${rpm.roundToInt()} об/мин"
    ) {
        val center = center
        val radius = size.minDimension / 2 - 20

        // Зона опасности (>5000 об/мин) — полупрозрачный красный сектор
        if (rpm > 5000) {
            drawArc(
                color = Color.Red.copy(alpha = 0.2f),
                startAngle = 150f + (5000 / 7000f) * 60f,
                sweepAngle = (2000 / 7000f) * 60f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = 16f)
            )
        }

        // Основная дуга (0–7000 об/мин)
        drawArc(
            color = trackColor,
            startAngle = 150f,
            sweepAngle = 60f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )

        // Стрелка, указывающая текущие обороты
        val angle = 150f + (rpm / 7000f) * 60f
        val endX = center.x + radius * cos(angle.toRadians())
        val endY = center.y + radius * sin(angle.toRadians())

        drawLine(
            color = if (rpm > 5000) Color.Red else Color.Green,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 4f
        )
    }
}

private fun Float.toRadians() = this * PI.toFloat() / 180f
