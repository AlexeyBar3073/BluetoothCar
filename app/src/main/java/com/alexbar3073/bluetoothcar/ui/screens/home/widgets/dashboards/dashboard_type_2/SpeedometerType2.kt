package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.graphics.Paint
import android.graphics.Typeface
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SpeedometerType2(
    modifier: Modifier = Modifier,
    speed: Float
) {
    val density = LocalDensity.current
    val labelTextSize = with(density) { 14.sp.toPx() }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 20.dp.toPx()
            
            // 1. Внешнее свечение/тень (имитация объема)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C3E50), Color(0xFF0A1118)),
                    center = center,
                    radius = radius + 10.dp.toPx()
                ),
                radius = radius + 10.dp.toPx(),
                alpha = 0.5f
            )

            // 2. Основной фон круга с текстурным градиентом
            drawCircle(
                brush = Brush.radialGradient(
                    0.0f to Color(0xFF1B2E3D),
                    0.7f to Color(0xFF0F1B26),
                    1.0f to Color(0xFF080D12),
                    center = center,
                    radius = radius
                ),
                radius = radius
            )

            // 3. Тонкий светлый кант
            drawCircle(
                color = Color.White.copy(alpha = 0.15f),
                radius = radius,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Шкала (150 до 390 градусов)
            val startAngle = 150f
            val sweepAngle = 240f
            val maxSpeedValue = 220f

            for (i in 0..220 step 10) {
                val currentAngle = startAngle + (i / maxSpeedValue) * sweepAngle
                val angleRad = Math.toRadians(currentAngle.toDouble())
                
                val isMajor = i % 20 == 0
                val tickLength = if (isMajor) 16.dp.toPx() else 8.dp.toPx()
                val tickWidth = if (isMajor) 2.5.dp.toPx() else 1.dp.toPx()
                
                val innerPoint = Offset(
                    (center.x + (radius - tickLength) * cos(angleRad)).toFloat(),
                    (center.y + (radius - tickLength) * sin(angleRad)).toFloat()
                )
                val outerPoint = Offset(
                    (center.x + radius * cos(angleRad)).toFloat(),
                    (center.y + radius * sin(angleRad)).toFloat()
                )

                drawLine(
                    color = if (i > 180) Color(0xFFE74C3C).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.7f),
                    start = innerPoint,
                    end = outerPoint,
                    strokeWidth = tickWidth,
                    cap = StrokeCap.Butt
                )
                
                // Числа на шкале
                if (isMajor) {
                    val textRadius = radius - tickLength - 15.dp.toPx()
                    val x = (center.x + textRadius * cos(angleRad)).toFloat()
                    val y = (center.y + textRadius * sin(angleRad)).toFloat()
                    
                    drawContext.canvas.nativeCanvas.drawText(
                        i.toString(),
                        x,
                        y + (labelTextSize / 3), // Центрирование по вертикали
                        Paint().apply {
                            color = android.graphics.Color.WHITE
                            alpha = 200
                            textSize = labelTextSize
                            textAlign = Paint.Align.CENTER
                            typeface = Typeface.DEFAULT_BOLD
                        }
                    )
                }
            }

            // 5. Внутренний декоративный диск
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF14202B), Color(0xFF0A1118)),
                    center = center,
                    radius = radius * 0.55f
                ),
                radius = radius * 0.55f
            )
            
            // 6. Стрелка
            val needleValue = speed.coerceIn(0f, maxSpeedValue)
            val needleAngle = startAngle + (needleValue / maxSpeedValue) * sweepAngle
            val needleRad = Math.toRadians(needleAngle.toDouble())
            val needleLength = radius - 5.dp.toPx()
            
            // Тень стрелки
            val shadowOffset = 3.dp.toPx()
            drawLine(
                color = Color.Black.copy(alpha = 0.5f),
                start = center,
                end = Offset(
                    (center.x + needleLength * cos(needleRad)).toFloat() + shadowOffset,
                    (center.y + needleLength * sin(needleRad)).toFloat() + shadowOffset
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Основное тело стрелки (Оранжевый градиент)
            drawLine(
                color = Color(0xFFE88A1A),
                start = center,
                end = Offset(
                    (center.x + needleLength * cos(needleRad)).toFloat(),
                    (center.y + needleLength * sin(needleRad)).toFloat()
                ),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round
            )
            
            // Центр стрелки (Колпачок)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF2C3E50), Color(0xFF000000)),
                    center = center,
                    radius = 12.dp.toPx()
                ),
                radius = 12.dp.toPx()
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.2f),
                radius = 12.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )
        }

        // Текстовые метки
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-25).dp)
        ) {
            Text(
                text = "км/ч",
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "АКПП",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        
        // Передача
        Text(
            text = "D3",
            color = Color.White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-45).dp)
        )
    }
}
