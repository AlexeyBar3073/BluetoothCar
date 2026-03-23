package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_1

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.LayoutDirection
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.*

@Composable
internal fun TemperatureWidget(
    modifier: Modifier = Modifier,
    carData: CarData,
    geometry: Geometry
) {
    // Диапазон температур: 40°C - 140°C
    val minTemp = 40f
    val maxTemp = 140f

    val coolantTemp = carData.coolantTemp.coerceIn(minTemp, maxTemp)
    val transTemp = carData.transmissionTemp.coerceIn(minTemp, maxTemp)

    // ========== СТАРТОВАЯ АНИМАЦИЯ ==========
    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) }
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(1f, animationSpec = tween(1300, easing = FastOutSlowInEasing))
            startupAnim.animateTo(0f, animationSpec = tween(5000, easing = LinearOutSlowInEasing))
            transitionAnim.animateTo(1f, animationSpec = tween(1500, easing = FastOutSlowInEasing))
            startupFinished = true
        }
    }

    // ========== ПОДГОТОВКА ДАННЫХ ==========
    fun getDisplayProgress(currentValue: Float): Float {
        val targetProgress = (currentValue - minTemp) / (maxTemp - minTemp)
        return if (!startupFinished) {
            if (transitionAnim.value > 0f) targetProgress * transitionAnim.value else startupAnim.value
        } else {
            targetProgress
        }
    }

    val animatedCoolantProgress by animateFloatAsState(
        targetValue = getDisplayProgress(coolantTemp),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "coolant"
    )

    val animatedTransProgress by animateFloatAsState(
        targetValue = getDisplayProgress(transTemp),
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "trans"
    )

    // Используем новую иконку
    val coolantIcon = painterResource(R.drawable.engine_coolant_new)
    val transIcon = painterResource(R.drawable.oil_temperature_new)

    // ========== ГЕОМЕТРИЯ ДУГИ (СИММЕТРИЧНО FUEL) ==========
    val arcGeometry = remember(geometry.width, geometry.height) {
        val outerRadius = (geometry.width - 2 * geometry.margin) / 2f
        val cy = geometry.center.y
        val dyTop = geometry.margin - cy
        val dyBottom = (geometry.height - geometry.margin) - cy

        val angleFromVerticalTop = acos((dyTop / outerRadius).coerceIn(-1f, 1f))
        val angleFromVerticalBottom = acos((dyBottom / outerRadius).coerceIn(-1f, 1f))

        val startAngle = 90f - Math.toDegrees(angleFromVerticalBottom.toDouble()).toFloat()
        val endAngle = 90f - Math.toDegrees(angleFromVerticalTop.toDouble()).toFloat()
        val totalSweep = endAngle - startAngle // Отрицательное значение

        val tickRadius = outerRadius - geometry.outerStrokeWidth / 2f - geometry.gapScale
        
        val gapAngle = Math.toDegrees((geometry.tickLarge / tickRadius).toDouble()).toFloat()
        val individualSweep = (abs(totalSweep) - gapAngle) / 2f

        TempArcGeometry(
            startAngle = startAngle,
            totalSweep = totalSweep,
            outerRadius = outerRadius,
            tickRadius = tickRadius,
            gapAngle = gapAngle,
            individualSweep = individualSweep
        )
    }

    // ========== КЭШИРОВАНИЕ БИТМАПА ==========
    val bitmapKey = remember(geometry.width, geometry.height, geometry.ringColor) {
        Triple(geometry.width, geometry.height, geometry.ringColor)
    }
    var tempScaleBitmap by remember(bitmapKey) { mutableStateOf<ImageBitmap?>(null) }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            // 1. Статическая шкала
            drawImage(
                image = tempScaleBitmap ?: buildTempScaleBitmap(geometry, arcGeometry, coolantIcon, transIcon).also {
                    tempScaleBitmap = it
                },
                topLeft = Offset.Zero
            )

            // 2. ДИНАМИЧЕСКИЕ ДУГИ И СТРЕЛКИ
            
            // --- COOLANT (ВЕРХНЯЯ) ---
            val coolantStart = arcGeometry.startAngle - arcGeometry.individualSweep - arcGeometry.gapAngle
            drawDynamicArc(
                progress = animatedCoolantProgress,
                startAngle = coolantStart,
                sweepAngle = -arcGeometry.individualSweep,
                geometry = geometry,
                arcGeo = arcGeometry
            )

            // --- TRANSMISSION (НИЖНЯЯ) ---
            drawDynamicArc(
                progress = animatedTransProgress,
                startAngle = arcGeometry.startAngle,
                sweepAngle = -arcGeometry.individualSweep,
                geometry = geometry,
                arcGeo = arcGeometry
            )
        }
    }
}

private data class TempArcGeometry(
    val startAngle: Float,
    val totalSweep: Float,
    val outerRadius: Float,
    val tickRadius: Float,
    val gapAngle: Float,
    val individualSweep: Float
)

private fun DrawScope.drawDynamicArc(
    progress: Float,
    startAngle: Float,
    sweepAngle: Float,
    geometry: Geometry,
    arcGeo: TempArcGeometry
) {
    val currentAngle = startAngle + progress * sweepAngle
    
    val sweepBrush = Brush.sweepGradient(
        colorStops = arrayOf(
            0f to Color.Transparent,
            (currentAngle / 360f).coerceIn(0f, 1f) to Color.White,
            (startAngle / 360f).coerceIn(0f, 1f) to Color.Transparent,
            1f to Color.Transparent
        ),
        center = geometry.center
    )

    drawArc(
        brush = sweepBrush,
        startAngle = startAngle,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = Offset(geometry.center.x - arcGeo.outerRadius, geometry.center.y - arcGeo.outerRadius),
        size = Size(arcGeo.outerRadius * 2, arcGeo.outerRadius * 2),
        style = Stroke(width = geometry.outerStrokeWidth)
    )

    drawTempNeedle(currentAngle, geometry, arcGeo)
}

private fun DrawScope.drawTempNeedle(
    angle: Float,
    geometry: Geometry,
    arcGeo: TempArcGeometry
) {
    val rad = Math.toRadians(angle.toDouble()).toFloat()
    val cosA = cos(rad)
    val sinA = sin(rad)

    val speedoDiff = geometry.scaleRadius - geometry.ringRadius
    val glowStartRadius = arcGeo.tickRadius - speedoDiff
    val scaleOuterRadius = arcGeo.tickRadius

    val tip = Offset(geometry.center.x + scaleOuterRadius * cosA, geometry.center.y + scaleOuterRadius * sinA)
    val start = Offset(geometry.center.x + glowStartRadius * cosA, geometry.center.y + glowStartRadius * sinA)

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(geometry.ringColor.copy(alpha = 0f), geometry.ringColor.copy(alpha = 0.5f)),
            start = start, end = tip
        ),
        start = start, end = tip, strokeWidth = 5f * geometry.unit, cap = StrokeCap.Round
    )

    drawLine(
        brush = Brush.linearGradient(
            colors = listOf(Color.Transparent, Color.White),
            start = start, end = tip
        ),
        start = start, end = tip, strokeWidth = 1.5f * geometry.unit, cap = StrokeCap.Round
    )
}

private fun buildTempScaleBitmap(
    geometry: Geometry,
    arcGeo: TempArcGeometry,
    coolantIcon: androidx.compose.ui.graphics.painter.Painter,
    transIcon: androidx.compose.ui.graphics.painter.Painter
): ImageBitmap {
    val bitmap = ImageBitmap(geometry.width.toInt(), geometry.height.toInt())
    val composeCanvas = Canvas(bitmap)
    val drawScope = CanvasDrawScope()

    drawScope.draw(
        density = geometry.density,
        layoutDirection = LayoutDirection.Ltr,
        canvas = composeCanvas,
        size = Size(geometry.width, geometry.height)
    ) {
        val textPaint = android.graphics.Paint().apply {
            color = geometry.ringColor.toArgb()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = geometry.textSizePx
            isAntiAlias = true
        }

        // Шкалы
        drawSubScale(this, geometry, arcGeo, arcGeo.startAngle, -arcGeo.individualSweep, textPaint, icon = transIcon, isCoolant = false)
        val coolantStart = arcGeo.startAngle - arcGeo.individualSweep - arcGeo.gapAngle
        drawSubScale(this, geometry, arcGeo, coolantStart, -arcGeo.individualSweep, textPaint, icon = coolantIcon, isCoolant = true)

        // GLOW
        val speedoDiff = geometry.scaleRadius - geometry.ringRadius
        val edgeRadius = arcGeo.tickRadius - speedoDiff
        val glowDepth = geometry.glowWidth * 1.3f
        val gradientOuterRadius = edgeRadius + glowDepth
        val edgeStop = edgeRadius / gradientOuterRadius
        val rangeGlow = 1f - edgeStop

        val gradientBrush = Brush.radialGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                edgeStop to geometry.ringColor,
                (edgeStop + rangeGlow * 0.25f) to geometry.ringColor.copy(alpha = 0.6f),
                (edgeStop + rangeGlow * 0.6f) to geometry.ringColor.copy(alpha = 0.25f),
                1f to Color.Transparent
            ),
            center = geometry.center,
            radius = gradientOuterRadius
        )

        val strokeRadius = edgeRadius + glowDepth * 0.65f
        
        drawArc(
            brush = gradientBrush,
            startAngle = arcGeo.startAngle,
            sweepAngle = -arcGeo.individualSweep,
            useCenter = false,
            topLeft = Offset(geometry.center.x - strokeRadius, geometry.center.y - strokeRadius),
            size = Size(strokeRadius * 2, strokeRadius * 2),
            style = Stroke(width = glowDepth)
        )
        
        drawArc(
            brush = gradientBrush,
            startAngle = coolantStart,
            sweepAngle = -arcGeo.individualSweep,
            useCenter = false,
            topLeft = Offset(geometry.center.x - strokeRadius, geometry.center.y - strokeRadius),
            size = Size(strokeRadius * 2, strokeRadius * 2),
            style = Stroke(width = glowDepth)
        )
    }
    return bitmap
}

private fun drawSubScale(
    drawScope: DrawScope,
    geometry: Geometry,
    arcGeo: TempArcGeometry,
    startAngle: Float,
    sweepAngle: Float,
    textPaint: android.graphics.Paint,
    icon: androidx.compose.ui.graphics.painter.Painter,
    isCoolant: Boolean
) {
    for (i in 0..10) {
        val percent = i * 10f
        val angle = startAngle + (percent / 100f) * sweepAngle
        val rad = Math.toRadians(angle.toDouble()).toFloat()
        val cosA = cos(rad)
        val sinA = sin(rad)

        val tickLength = when {
            percent == 0f || percent == 50f || percent == 100f -> geometry.tickLarge
            else -> geometry.tickSmall
        }
        
        val tickStroke = when (tickLength) {
            geometry.tickLarge -> 2f * geometry.unit
            else -> 1f * geometry.unit
        }

        val startTick = Offset(geometry.center.x + arcGeo.tickRadius * cosA, geometry.center.y + arcGeo.tickRadius * sinA)
        val endTick = Offset(geometry.center.x + (arcGeo.tickRadius - tickLength) * cosA, geometry.center.y + (arcGeo.tickRadius - tickLength) * sinA)

        drawScope.drawLine(
            color = geometry.ringColor,
            start = startTick, end = endTick,
            strokeWidth = tickStroke,
            cap = StrokeCap.Round
        )

        if (percent == 0f || percent == 50f || percent == 100f) {
            val textRadius = arcGeo.tickRadius - tickLength - geometry.textOffsetFromTick - 6f * geometry.unit
            val itemX = geometry.center.x + textRadius * cosA
            val itemY = geometry.center.y + textRadius * sinA

            if (percent == 50f) {
                val iconSize = geometry.textSizePx * 1.3f
                drawScope.translate(left = itemX - iconSize / 2f, top = itemY - iconSize / 2f) {
                    with(icon) {
                        draw(size = Size(iconSize, iconSize), colorFilter = ColorFilter.tint(geometry.ringColor))
                    }
                }
            } else {
                val tempValue = (40 + (percent / 100f) * 100).toInt()
                drawScope.drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(itemX, itemY)
                    val fm = textPaint.fontMetrics
                    val baseline = -(fm.ascent + fm.descent) / 2f
                    drawText(tempValue.toString(), 0f, baseline, textPaint)
                    restore()
                }
            }
        }
    }
}
