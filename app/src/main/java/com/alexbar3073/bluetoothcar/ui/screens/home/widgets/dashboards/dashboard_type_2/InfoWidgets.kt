package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_2

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InfoTopPanel(voltage: Float, odometer: Float) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(Color(0xFF0F161E).copy(alpha = 0.8f), RoundedCornerShape(8.dp))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Вольтметр", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "%.1f B".format(voltage), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "ODO", color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "%,.0f км".format(odometer).replace(',', ' '), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun ConsumptionWidget(consumption: Float) {
    Box(
        modifier = Modifier
            .background(Color(0xFF0F161E), RoundedCornerShape(20.dp))
            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "%.1f л/100 км".format(consumption),
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun SmallGaugeWidget(
    label: String,
    value: Float,
    maxValue: Float,
    unit: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(4.dp)
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2, size.height / 2)
                val radius = size.minDimension / 2 - 4.dp.toPx()

                // Шкала
                val startAngle = 150f
                val sweepAngle = 240f
                
                drawArc(
                    color = Color.White.copy(alpha = 0.1f),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // Деления
                for (i in 0..10) {
                    val angle = startAngle + (i / 10f) * sweepAngle
                    val angleRad = Math.toRadians(angle.toDouble())
                    val innerRadius = radius - (if (i % 5 == 0) 6.dp.toPx() else 3.dp.toPx())
                    
                    drawLine(
                        color = Color.White.copy(alpha = 0.4f),
                        start = Offset(
                            (center.x + innerRadius * cos(angleRad)).toFloat(),
                            (center.y + innerRadius * sin(angleRad)).toFloat()
                        ),
                        end = Offset(
                            (center.x + radius * cos(angleRad)).toFloat(),
                            (center.y + radius * sin(angleRad)).toFloat()
                        ),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Стрелка
                val needleAngle = startAngle + (value.coerceIn(0f, maxValue) / maxValue) * sweepAngle
                val needleRad = Math.toRadians(needleAngle.toDouble())
                val needleLength = radius - 5.dp.toPx()
                
                drawLine(
                    color = Color(0xFFE88A1A),
                    start = center,
                    end = Offset(
                        (center.x + needleLength * cos(needleRad)).toFloat(),
                        (center.y + needleLength * sin(needleRad)).toFloat()
                    ),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                drawCircle(Color(0xFFE88A1A), radius = 4.dp.toPx())
            }
            
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.offset(y = (-15).dp)
            )
        }
        
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "%.0f".format(value),
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = unit,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp
            )
        }
    }
}

@Composable
fun RangeSliderWidget(remainingRange: Float, maxRange: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val yPos = size.height / 2
                val startX = 0f
                val endX = size.width
                
                // Основная линия шкалы
                drawLine(
                    color = Color.White.copy(alpha = 0.3f),
                    start = Offset(startX, yPos),
                    end = Offset(endX, yPos),
                    strokeWidth = 1.dp.toPx()
                )

                // Деления
                val steps = 10
                for (i in 0..steps) {
                    val x = (i.toFloat() / steps) * size.width
                    drawLine(
                        color = Color.White.copy(alpha = 0.3f),
                        start = Offset(x, yPos - 5.dp.toPx()),
                        end = Offset(x, yPos + 5.dp.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                // Оранжевый сегмент (текущий запас хода)
                val rangeWidth = (remainingRange / maxRange).coerceIn(0f, 1f) * size.width
                drawLine(
                    color = Color(0xFFE88A1A),
                    start = Offset(startX, yPos),
                    end = Offset(rangeWidth, yPos),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                
                // Маркер текущего значения
                drawCircle(
                    color = Color(0xFFE88A1A),
                    radius = 4.dp.toPx(),
                    center = Offset(rangeWidth, yPos)
                )
                
                // Светящийся эффект для маркера (упрощенно)
                drawCircle(
                    color = Color(0xFFE88A1A).copy(alpha = 0.3f),
                    radius = 8.dp.toPx(),
                    center = Offset(rangeWidth, yPos)
                )
            }
            
            // Подписи значений
            Text(
                text = "0 км",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomStart)
            )
            
            val rangePos = (remainingRange / maxRange).coerceIn(0.1f, 0.9f)
            Text(
                text = "%.0f км".format(remainingRange),
                color = Color(0xFFE88A1A),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth(rangePos)
                    .wrapContentWidth(Alignment.End)
            )

            Text(
                text = "%.0f км".format(maxRange),
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        
        Text(
            text = "Possible range and remaining range",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
