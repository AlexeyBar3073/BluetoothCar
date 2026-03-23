package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.cos
import kotlin.math.sin

@Composable
internal fun DashboardType3EngineTemp(
    modifier: Modifier = Modifier,
    carData: CarData,
    geometry: DashboardType3Geometry
) {
    val minTemp = 10f
    val maxTemp = 130f
    val startAngle = 0f
    val sweepAngle = -135f

    val animatedTemp by animateFloatAsState(
        targetValue = carData.coolantTemp.coerceIn(minTemp, maxTemp),
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 80f),
        label = "engine_temp_anim"
    )

    val iconPainter = painterResource(R.drawable.engine_coolant_new)

    val bitmapKey = remember(geometry.width, geometry.height) {
        Pair(geometry.width, geometry.height)
    }

    var backgroundBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }
    var needleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hubRadius = 20f * geometry.unit
            val needleAngle = startAngle + sweepAngle * ((animatedTemp - minTemp) / (maxTemp - minTemp))

            val endNorm = 225f / 360f
            val startNorm = 1.0f
            val currentNeedleNorm = ((needleAngle % 360 + 360) % 360) / 360f

            val arcBrush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0f to Color.Transparent,
                    (endNorm - 0.001f) to Color.Transparent,
                    endNorm to Color.White.copy(alpha = 0.1f),
                    currentNeedleNorm to Color.White,
                    startNorm to Color.White.copy(alpha = 0.1f),
                    1f to Color.Transparent
                ),
                center = geometry.center
            )

            drawArc(
                brush = arcBrush,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(geometry.center.x - geometry.outerRingRadius, geometry.center.y - geometry.outerRingRadius),
                size = Size(geometry.outerRingRadius * 2, geometry.outerRingRadius * 2),
                style = Stroke(width = geometry.outerStrokeWidth)
            )

            drawImage(
                image = backgroundBitmap ?: buildBackgroundBitmap(
                    geometry = geometry,
                    minTemp = minTemp,
                    maxTemp = maxTemp,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    hubRadius = hubRadius,
                    iconPainter = iconPainter
                ).also { backgroundBitmap = it }
            )

            drawEngineValueText(geometry, animatedTemp.toInt(), hubRadius)

            rotate(needleAngle, pivot = geometry.center) {
                drawImage(
                    image = needleBitmap ?: buildNeedleBitmap(
                        geometry = geometry,
                        hubRadius = hubRadius
                    ).also { needleBitmap = it }
                )
            }
        }
    }
}

private fun DrawScope.drawEngineValueText(
    geometry: DashboardType3Geometry,
    temp: Int,
    hubRadius: Float
) {
    val areaLeft = geometry.center.x - geometry.outerRingRadius + 10f * geometry.unit
    val tempText = temp.toString()
    val unitText = "°C"

    drawContext.canvas.nativeCanvas.apply {
        save()
        val valuePaint = android.graphics.Paint().apply {
            color = Color.White.copy(alpha = 0.8f).toArgb()
            textSize = hubRadius * 1.3f
            isAntiAlias = true
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val unitPaint = android.graphics.Paint().apply {
            color = Color.White.copy(alpha = 0.8f).toArgb()
            textSize = valuePaint.textSize * 0.8f
            isAntiAlias = true
        }

        val totalWidth = valuePaint.measureText(tempText) + unitPaint.measureText(unitText) + 2f * geometry.unit
        val startX = areaLeft + 8f * geometry.unit
        // Смещаем текст вверх
        val baselineY = geometry.center.y - hubRadius * 1.1f - (valuePaint.fontMetrics.ascent + valuePaint.fontMetrics.descent) / 2f

        drawText(tempText, startX, baselineY, valuePaint)
        drawText(unitText, startX + valuePaint.measureText(tempText) + 2f * geometry.unit, baselineY, unitPaint)
        restore()
    }
}

private fun buildBackgroundBitmap(
    geometry: DashboardType3Geometry,
    minTemp: Float,
    maxTemp: Float,
    startAngle: Float,
    sweepAngle: Float,
    hubRadius: Float,
    iconPainter: Painter
): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val drawScope = CanvasDrawScope()

    drawScope.draw(geometry.density, LayoutDirection.Ltr, canvas, Size(geometry.width, geometry.height)) {
        val textPaint = android.graphics.Paint().apply {
            color = geometry.ringColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        val baseTextSize = geometry.textSizePx * 0.85f * 1.4f
        val range = maxTemp - minTemp

        for (tempInt in (minTemp.toInt()..maxTemp.toInt() step 2)) {
            val angle = startAngle + sweepAngle * ((tempInt - minTemp) / range)
            val rad = Math.toRadians(angle.toDouble()).toFloat()
            val cos = cos(rad)
            val sin = sin(rad)

            val isLarge = (tempInt - 10) % 40 == 0
            val isMedium = !isLarge && (tempInt - 10) % 20 == 0
            val tickLen = when {
                isLarge -> geometry.tickLarge
                isMedium -> geometry.tickMedium
                else -> geometry.tickSmall * 0.8f
            }

            drawLine(
                color = geometry.ringColor,
                start = Offset(geometry.center.x + cos * geometry.scaleRadius, geometry.center.y + sin * geometry.scaleRadius),
                end = Offset(geometry.center.x + cos * (geometry.scaleRadius - tickLen), geometry.center.y + sin * (geometry.scaleRadius - tickLen)),
                strokeWidth = if (isLarge) geometry.tickLargeWidth else geometry.tickSmallWidth,
                cap = StrokeCap.Round
            )

            if (isLarge || isMedium) {
                textPaint.textSize = if (isLarge) baseTextSize else baseTextSize * 0.7f
                val fontMetrics = textPaint.fontMetrics
                val tRadius = geometry.scaleRadius - tickLen - 12f * geometry.unit
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(geometry.center.x + cos * tRadius, geometry.center.y + sin * tRadius)
                    rotate(angle + 90f)
                    drawText(tempInt.toString(), 0f, -(fontMetrics.ascent + fontMetrics.descent) / 2f, textPaint)
                    restore()
                }
            }
        }

        // Рамка значения смещена ВВЕРХ
        val areaLeft = geometry.center.x - geometry.outerRingRadius + 10f * geometry.unit
        val areaRight = geometry.center.x - hubRadius - 20f * geometry.unit
        if (areaRight > areaLeft) {
            val topY = geometry.center.y - hubRadius * 2.1f
            val bottomY = geometry.center.y - hubRadius * 0.1f
            val areaRect = RoundRect(areaLeft, topY, areaRight, bottomY, CornerRadius(6f * geometry.unit))
            drawPath(Path().apply { addRoundRect(areaRect) }, Color(0xFF080808))
            drawPath(
                path = Path().apply { addRoundRect(areaRect) },
                brush = Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.15f), Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0.15f)),
                    start = Offset(areaRect.left, areaRect.top),
                    end = Offset(areaRect.right, areaRect.bottom)
                ),
                style = Stroke(width = 2.2f * geometry.unit)
            )
        }

        val iconSize = 33.6f * geometry.unit
        val iconRadius = geometry.scaleRadius / 2f
        val iconX = geometry.center.x - iconSize / 2f
        val iconY = geometry.center.y - iconRadius - iconSize / 2f
        translate(iconX, iconY) {
            with(iconPainter) {
                draw(Size(iconSize, iconSize), colorFilter = ColorFilter.tint(geometry.ringColor))
            }
        }
    }
    return bitmap
}

private fun buildNeedleBitmap(
    geometry: DashboardType3Geometry,
    hubRadius: Float
): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val canvas = androidx.compose.ui.graphics.Canvas(bitmap)
    val drawScope = CanvasDrawScope()

    drawScope.draw(geometry.density, LayoutDirection.Ltr, canvas, Size(geometry.width, geometry.height)) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(geometry.ringColor.copy(alpha = 0.4f), Color.Transparent),
                center = geometry.center,
                radius = hubRadius * 3f
            ),
            radius = hubRadius * 3f,
            center = geometry.center
        )

        val needleLength = geometry.scaleRadius - 2f * geometry.unit
        val tip = Offset(geometry.center.x + needleLength, geometry.center.y)
        val start = Offset(geometry.center.x + hubRadius * 0.5f, geometry.center.y)

        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(geometry.ringColor.copy(alpha = 0f), geometry.ringColor.copy(alpha = 0.6f)),
                start = geometry.center,
                end = tip
            ),
            start = start, end = tip, strokeWidth = 4.5f * geometry.unit, cap = StrokeCap.Round
        )
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(Color.Transparent, Color.White),
                start = start, end = tip
            ),
            start = start, end = tip, strokeWidth = 1.3f * geometry.unit, cap = StrokeCap.Round
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF2A2A2A), Color(0xFF0F0F0F)),
                center = geometry.center,
                radius = hubRadius
            ),
            radius = hubRadius,
            center = geometry.center
        )

        drawCircle(
            brush = Brush.sweepGradient(
                colorStops = arrayOf(
                    0.0f to Color.White.copy(alpha = 0.1f),
                    0.25f to Color.White.copy(alpha = 0.45f),
                    0.5f to Color.White.copy(alpha = 0.1f),
                    0.75f to Color.Black.copy(alpha = 0.4f),
                    1.0f to Color.White.copy(alpha = 0.1f)
                ),
                center = geometry.center
            ),
            radius = hubRadius - 0.5f * geometry.unit,
            center = geometry.center,
            style = Stroke(width = 1.5f * geometry.unit)
        )

        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = 1.5f * geometry.unit,
            center = geometry.center
        )
    }
    return bitmap
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun DashboardType3EngineTempPreview() {
    BoxWithConstraints(modifier = Modifier.size(250.dp)) {
        val density = LocalDensity.current
        val geometry = remember(maxWidth, maxHeight) {
            DashboardType3Geometry.fromSize(Size(with(density){maxWidth.toPx()}, with(density){maxHeight.toPx()}), density)
        }
        DashboardType3EngineTemp(
            carData = CarData(coolantTemp = 90f),
            geometry = geometry,
            modifier = Modifier.fillMaxSize()
        )
    }
}
