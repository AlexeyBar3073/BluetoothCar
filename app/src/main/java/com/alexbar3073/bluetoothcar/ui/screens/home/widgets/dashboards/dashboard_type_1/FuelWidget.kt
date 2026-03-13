package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.*

/**
 * Виджет индикатора топлива (левый сектор большой окружности).
 *
 * КОНЦЕПЦИЯ:
 *   1. Внешняя белая дуга
 *   2. Метки с шагом 2.5% с иерархией
 *   3. Цифры у больших меток (0, 25, 50, 75, 100) с реальными литрами
 *   4. Заливка топливом основным цветом дашборда
 *
 * @param modifier Модификатор для настройки размера и позиции
 * @param carData Данные автомобиля (содержат уровень топлива в литрах)
 * @param appSettings Настройки приложения (содержат емкость бака)
 * @param geometry Геометрические параметры дашборда
 */
@Composable
internal fun FuelWidget(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?,
    geometry: Geometry
) {
    val fuelTankCapacity = appSettings?.fuelTankCapacity ?: 60f

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Радиус большой окружности (от ширины с учетом margin)
            val outerRadius = (geometry.width - 2 * geometry.margin) / 2f
            val cy = geometry.center.y

            // Расстояние от центра до верхней и нижней границ
            val dyTop = geometry.margin - cy
            val dyBottom = (geometry.height - geometry.margin) - cy

            // Углы от вертикальной оси
            val angleFromVerticalTop = acos((dyTop / outerRadius).coerceIn(-1f, 1f))
            val angleFromVerticalBottom = acos((dyBottom / outerRadius).coerceIn(-1f, 1f))

            // Углы для Compose Canvas
            val startAngleDeg = 90f + Math.toDegrees(angleFromVerticalBottom.toDouble()).toFloat()
            val endAngleDeg = 90f + Math.toDegrees(angleFromVerticalTop.toDouble()).toFloat()
            val sweepAngle = endAngleDeg - startAngleDeg

            // Радиус для рисок (на том же месте, где была бы шкала)
            val tickRadius = outerRadius - geometry.outerStrokeWidth / 2f - geometry.gapScale

            // === 1. ВНЕШНЯЯ БЕЛАЯ ДУГА ===
            drawArc(
                color = Color.White,
                startAngle = startAngleDeg,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    geometry.center.x - outerRadius,
                    geometry.center.y - outerRadius
                ),
                size = Size(outerRadius * 2, outerRadius * 2),
                style = Stroke(width = geometry.outerStrokeWidth)
            )

            // === 2. ЗАЛИВКА ТОПЛИВОМ ===
            val fuelPercent = (carData.fuel / fuelTankCapacity).coerceIn(0f, 1f)
            val fuelSweep = sweepAngle * fuelPercent

            drawArc(
                color = geometry.ringColor,
                startAngle = startAngleDeg,
                sweepAngle = fuelSweep,
                useCenter = false,
                topLeft = Offset(
                    geometry.center.x - tickRadius,
                    geometry.center.y - tickRadius
                ),
                size = Size(tickRadius * 2, tickRadius * 2),
                style = Stroke(width = geometry.tickLarge)
            )

            // === 3. МЕТКИ ШКАЛЫ ===
            // Проценты от 0 до 100 с шагом 2.5
            for (step in 0..40) {
                val percent = step * 2.5f
                val angle = startAngleDeg + (percent / 100f) * sweepAngle
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val cosA = cos(rad)
                val sinA = sin(rad)

                // Определяем тип метки по иерархии
                val tickLength = when {
                    percent % 25f == 0f -> geometry.tickLarge        // 0, 25, 50, 75, 100
                    percent % 12.5f == 0f -> geometry.tickMedium      // 12.5, 37.5, 62.5, 87.5
                    else -> geometry.tickSmall
                }

                val tickStroke = when (tickLength) {
                    geometry.tickLarge -> 2f * geometry.unit
                    geometry.tickMedium -> 1.5f * geometry.unit
                    else -> 1f * geometry.unit
                }

                val start = Offset(
                    geometry.center.x + tickRadius * cosA,
                    geometry.center.y + tickRadius * sinA
                )
                val end = Offset(
                    geometry.center.x + (tickRadius - tickLength) * cosA,
                    geometry.center.y + (tickRadius - tickLength) * sinA
                )

                drawLine(
                    color = geometry.ringColor,
                    start = start,
                    end = end,
                    strokeWidth = tickStroke,
                    cap = StrokeCap.Round
                )

                // === 4. ЦИФРЫ ДЛЯ БОЛЬШИХ МЕТОК ===
                if (percent % 25f == 0f) {
                    val liters = (percent / 100f * fuelTankCapacity).toInt()
                    val text = liters.toString()  // только цифры, без "л"

                    val textPaint = android.graphics.Paint().apply {
                        color = geometry.ringColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = geometry.textSizePx * 0.8f
                        isAntiAlias = true
                    }

                    // Позиция для текста (чуть дальше от конца метки)
                    val textRadius = tickRadius - tickLength - geometry.textOffsetFromTick
                    val textX = geometry.center.x + textRadius * cosA
                    val textY = geometry.center.y + textRadius * sinA

                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        translate(textX, textY)

                        // Поворачиваем текст на угол метки, и если текст в нижней половине (угол между 90° и 270°),
                        // добавляем 180° чтобы он не был вверх ногами
                        val finalAngle = if (angle in 90f..270f) angle + 180f else angle

                        rotate(finalAngle)

                        val fm = textPaint.fontMetrics
                        val baseline = -(fm.ascent + fm.descent) / 2f
                        drawText(text, 0f, baseline, textPaint)
                        restore()
                    }
                }
            }
        }
    }
}