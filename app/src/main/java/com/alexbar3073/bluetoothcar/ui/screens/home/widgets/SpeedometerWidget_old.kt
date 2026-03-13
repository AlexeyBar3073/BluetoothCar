// SpeedometerWidget.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.graphics.drawscope.rotate as drawRotate

@OptIn(ExperimentalTextApi::class)
@Composable
fun SpeedometerWidget_old(
    modifier: Modifier = Modifier,
    currentSpeed: Int,
    minValue: Int = 0,
    maxValue: Int = 220,
    majorStep: Int = 20,
    minorStep: Int = 10,
    smallestStep: Int = 5,
    startAngle: Double = 135.0,
    endAngle: Double = 405.0
) {
    BluetoothCarTheme {
        val textMeasurer = rememberTextMeasurer()

        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Размеры Canvas
                if (size.width == 0f || size.height == 0f) {
                    return@Canvas
                }

                // Определение диаметра
                val canvasSize = minOf(size.width, size.height)
                val diameter = canvasSize * 0.9f
                val outerRadius = diameter / 2f

                // Центр canvas
                val centerX = size.width / 2f
                val centerY = size.height / 2f
                val center = Offset(centerX, centerY)

                // Диаметр и радиус чёрного круга
                val blackDiameter = diameter * 0.82f
                val blackRadius = blackDiameter / 2f

                // Радиус циферблата
                val radiusDial = blackRadius * 0.7f

                // Радиус индикации (для цифр скорости)
                val radiusInfo = blackRadius * 0.4f

                // Основной радиальный градиент (используем цвета из темы напрямую)
                val radialGradient = Brush.radialGradient(
                    colors = AppColors.SpeedometerGradient,
                    center = center,
                    radius = outerRadius
                )

                // Линейный градиент для затухания сверху вниз
                val verticalGradient = Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Black),
                    startY = centerY,
                    endY = centerY + outerRadius * 0.8f
                )

                // Основной слой
                drawCircle(
                    brush = radialGradient,
                    center = center,
                    radius = outerRadius
                )

                // Остальные элементы
                drawCircle(
                    color = Color.Black,
                    center = center,
                    radius = blackRadius
                )

                drawCircle(
                    color = AppColors.SpeedometerDialStroke,
                    center = center,
                    radius = blackRadius,
                    style = Stroke(width = 5.dp.value)
                )

                drawCircle(
                    brush = verticalGradient,
                    center = center,
                    radius = outerRadius
                )

                // Градиент для циферблата с прозрачностью
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = AppColors.SpeedometerGradient.map { it.copy(alpha = 0.5f) },
                        center = center,
                        radius = radiusDial
                    ),
                    center = center,
                    radius = radiusDial
                )

                drawCircle(
                    color = Color.Black,
                    center = center,
                    radius = radiusInfo
                )

                drawCircle(
                    color = AppColors.SpeedometerDialStroke,
                    center = center,
                    radius = radiusDial,
                    style = Stroke(width = 5.dp.value)
                )

                drawCircle(
                    color = AppColors.SpeedometerCenterStroke,
                    center = center,
                    radius = radiusInfo,
                    style = Stroke(width = 5.dp.value)
                )

                // Рисуем шкалу скорости
                drawSpeedScale(
                    center = center,
                    radius = radiusDial,
                    minValue = minValue,
                    maxValue = maxValue,
                    majorStep = majorStep,
                    minorStep = minorStep,
                    smallestStep = smallestStep,
                    startAngle = startAngle,
                    endAngle = endAngle,
                    textMeasurer = textMeasurer
                )

                // Рисуем стрелку на текущей скорости
                drawArrow(
                    center = center,
                    radiusInfo = radiusInfo,
                    radiusDial = radiusDial,
                    currentSpeed = currentSpeed,
                    minValue = minValue,
                    maxValue = maxValue,
                    startAngle = startAngle,
                    endAngle = endAngle
                )

                // Рисуем цифры текущей скорости в центре
                drawCurrentSpeedText(
                    center = center,
                    currentSpeed = currentSpeed,
                    textMeasurer = textMeasurer
                )

                // Треугольник поверх всех элементов
                val trianglePath = Path()
                trianglePath.moveTo(centerX - outerRadius, centerY + outerRadius)
                trianglePath.lineTo(centerX + outerRadius, centerY + outerRadius)
                trianglePath.lineTo(centerX, centerY)
                trianglePath.close()

                drawPath(
                    path = trianglePath,
                    color = Color.Black
                )
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawSpeedScale(
    center: Offset,
    radius: Float,
    minValue: Int,
    maxValue: Int,
    majorStep: Int,
    minorStep: Int,
    smallestStep: Int,
    startAngle: Double,
    endAngle: Double,
    textMeasurer: TextMeasurer
) {
    val totalAngle = endAngle - startAngle
    val totalValues = maxValue - minValue

    // Параметры меток (уменьшены на 25%)
    val majorTickLength = 15.dp.toPx() * 0.75f
    val minorTickLength = 10.dp.toPx() * 0.75f
    val smallestTickLength = 6.dp.toPx() * 0.75f

    val majorTickWidth = 2.dp.toPx()
    val minorTickWidth = 1.dp.toPx()
    val smallestTickWidth = 0.5f.dp.toPx()

    // Используем цвета из темы
    val tickColor = AppColors.SpeedometerTickMajor

    // Параметры текста
    val textStyle = TextStyle(
        color = AppColors.TextPrimary,
        fontSize = 12.sp
    )

    val textDistanceFromCenter = radius + 30.dp.toPx() * 0.75f

    // Сначала рисуем самые мелкие метки (5 км/ч)
    for (value in minValue..maxValue step smallestStep) {
        if (value == minValue) continue
        if (value % minorStep == 0) continue

        val angle = startAngle + (value.toDouble() / totalValues.toDouble()) * totalAngle
        val angleRad = Math.toRadians(angle)
        val cosAngle = cos(angleRad).toFloat()
        val sinAngle = sin(angleRad).toFloat()

        val startPoint = Offset(
            x = center.x + (radius * cosAngle),
            y = center.y + (radius * sinAngle)
        )

        val endPoint = Offset(
            x = center.x + ((radius + smallestTickLength) * cosAngle),
            y = center.y + ((radius + smallestTickLength) * sinAngle)
        )

        drawLine(
            color = AppColors.SpeedometerTickSmall,
            start = startPoint,
            end = endPoint,
            strokeWidth = smallestTickWidth
        )
    }

    // Затем рисуем средние метки (10 км/ч)
    for (value in minValue..maxValue step minorStep) {
        if (value == minValue) continue
        if (value % majorStep == 0) continue

        val angle = startAngle + (value.toDouble() / totalValues.toDouble()) * totalAngle
        val angleRad = Math.toRadians(angle)
        val cosAngle = cos(angleRad).toFloat()
        val sinAngle = sin(angleRad).toFloat()

        val startPoint = Offset(
            x = center.x + (radius * cosAngle),
            y = center.y + (radius * sinAngle)
        )

        val endPoint = Offset(
            x = center.x + ((radius + minorTickLength) * cosAngle),
            y = center.y + ((radius + minorTickLength) * sinAngle)
        )

        drawLine(
            color = AppColors.SpeedometerTickMinor,
            start = startPoint,
            end = endPoint,
            strokeWidth = minorTickWidth
        )
    }

    // Рисуем крупные метки (20 км/ч) и тексты
    for (value in minValue..maxValue step majorStep) {
        val angle = startAngle + (value.toDouble() / totalValues.toDouble()) * totalAngle
        val angleRad = Math.toRadians(angle)
        val cosAngle = cos(angleRad).toFloat()
        val sinAngle = sin(angleRad).toFloat()

        // Рисуем крупную метку (если не 0)
        if (value != minValue) {
            val startPoint = Offset(
                x = center.x + (radius * cosAngle),
                y = center.y + (radius * sinAngle)
            )

            val endPoint = Offset(
                x = center.x + ((radius + majorTickLength) * cosAngle),
                y = center.y + ((radius + majorTickLength) * sinAngle)
            )

            drawLine(
                color = tickColor,
                start = startPoint,
                end = endPoint,
                strokeWidth = majorTickWidth
            )
        }

        // Для всех крупных значений рисуем текст
        val text = value.toString()
        val textLayoutResult = textMeasurer.measure(text, textStyle)

        val textPoint = Offset(
            x = center.x + (textDistanceFromCenter * cosAngle),
            y = center.y + (textDistanceFromCenter * sinAngle)
        )

        val textRotation = angle.toFloat() + 90f

        drawRotate(
            degrees = textRotation,
            pivot = textPoint
        ) {
            val textOffset = Offset(
                x = -textLayoutResult.size.width / 2f,
                y = -textLayoutResult.size.height / 2f
            )

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = textPoint + textOffset
            )
        }
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawCurrentSpeedText(
    center: Offset,
    currentSpeed: Int,
    textMeasurer: TextMeasurer
) {
    // Основной текст скорости (крупный)
    val speedText = currentSpeed.toString()
    val speedTextStyle = TextStyle(
        color = AppColors.TextPrimary,
        fontSize = 36.sp,
        letterSpacing = 0.5.sp
    )

    val speedTextLayout = textMeasurer.measure(speedText, speedTextStyle)

    // Единицы измерения (меньший текст)
    val unitText = "км/ч"
    val unitTextStyle = TextStyle(
        color = AppColors.TextPrimary.copy(alpha = 0.8f),
        fontSize = 16.sp
    )

    val unitTextLayout = textMeasurer.measure(unitText, unitTextStyle)

    // Отступ между цифрой и единицами
    val verticalSpacing = 4.dp.toPx()

    // Общая высота всего текстового блока
    val totalTextHeight = speedTextLayout.size.height + unitTextLayout.size.height + verticalSpacing

    // Начальная Y-координата для выравнивания по центру
    val startY = center.y - totalTextHeight / 2

    // Позиционируем основной текст скорости
    val speedTextPoint = Offset(
        x = center.x - speedTextLayout.size.width / 2f,
        y = startY
    )

    // Рисуем основной текст скорости
    drawText(
        textLayoutResult = speedTextLayout,
        topLeft = speedTextPoint
    )

    // Позиционируем текст единиц измерения
    val unitTextPoint = Offset(
        x = center.x - unitTextLayout.size.width / 2f,
        y = speedTextPoint.y + speedTextLayout.size.height + verticalSpacing
    )

    // Рисуем текст единиц измерения
    drawText(
        textLayoutResult = unitTextLayout,
        topLeft = unitTextPoint
    )
}

private fun DrawScope.drawArrow(
    center: Offset,
    radiusInfo: Float,
    radiusDial: Float,
    currentSpeed: Int,
    minValue: Int,
    maxValue: Int,
    startAngle: Double,
    endAngle: Double
) {
    val totalAngle = endAngle - startAngle
    val totalValues = maxValue - minValue
    val angle = startAngle + (currentSpeed.toDouble() / totalValues.toDouble()) * totalAngle
    val angleRad = Math.toRadians(angle)

    drawClassicArrowV2(center, radiusInfo, radiusDial, angle, angleRad)
}

private fun DrawScope.drawClassicArrowV2(
    center: Offset,
    radiusInfo: Float,
    radiusDial: Float,
    angle: Double,
    angleRad: Double
) {
    val arrowTipRadius = radiusDial - 1.dp.toPx()
    val arrowBaseRadius = radiusInfo + 1.dp.toPx()
    val arrowBaseWidth = 6.dp.toPx()
    val arrowColor = AppColors.SpeedometerArrow

    val cosAngle = cos(angleRad).toFloat()
    val sinAngle = sin(angleRad).toFloat()
    val cosPerpendicular = cos(angleRad + Math.PI / 2).toFloat()
    val sinPerpendicular = sin(angleRad + Math.PI / 2).toFloat()

    val tipPoint = Offset(
        x = center.x + arrowTipRadius * cosAngle,
        y = center.y + arrowTipRadius * sinAngle
    )

    val baseLeftPoint = Offset(
        x = center.x + arrowBaseRadius * cosAngle + (arrowBaseWidth / 2) * cosPerpendicular,
        y = center.y + arrowBaseRadius * sinAngle + (arrowBaseWidth / 2) * sinPerpendicular
    )

    val baseRightPoint = Offset(
        x = center.x + arrowBaseRadius * cosAngle - (arrowBaseWidth / 2) * cosPerpendicular,
        y = center.y + arrowBaseRadius * sinAngle - (arrowBaseWidth / 2) * sinPerpendicular
    )

    val arrowPath = Path().apply {
        moveTo(tipPoint.x, tipPoint.y)
        lineTo(baseLeftPoint.x, baseLeftPoint.y)
        lineTo(baseRightPoint.x, baseRightPoint.y)
        close()
    }

    drawPath(path = arrowPath, color = arrowColor)

    drawPath(
        path = arrowPath,
        color = AppColors.SpeedometerArrowStroke,
        style = Stroke(width = 0.8f.dp.value)
    )
}