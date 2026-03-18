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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.R
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import kotlin.math.*

@Composable
internal fun FuelWidget(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?,
    geometry: Geometry
) {

    val fuelTankCapacity = appSettings?.fuelTankCapacity ?: 60f
    val currentFuel = carData.fuel.coerceIn(0f, fuelTankCapacity)

    // ========== СТАРТОВАЯ АНИМАЦИЯ ==========
    var startupFinished by rememberSaveable { mutableStateOf(false) }
    val startupAnim = remember { Animatable(0f) }
    val transitionAnim = remember { Animatable(0f) }

    LaunchedEffect(startupFinished) {
        if (!startupFinished) {
            startupAnim.animateTo(
                1f,
                animationSpec = tween(1300, easing = FastOutSlowInEasing)
            )
            startupAnim.animateTo(
                0f,
                animationSpec = tween(5000, easing = LinearOutSlowInEasing)
            )
            transitionAnim.animateTo(
                1f,
                animationSpec = tween(1500, easing = FastOutSlowInEasing)
            )
            startupFinished = true
        }
    }

    // ========== ПОДГОТОВКА ДАННЫХ ==========
    val displayFuel = if (!startupFinished) {
        if (transitionAnim.value > 0f) {
            val targetFuel = (currentFuel / fuelTankCapacity).coerceIn(0f, 1f)
            targetFuel * transitionAnim.value
        } else {
            startupAnim.value
        }
    } else {
        (currentFuel / fuelTankCapacity).coerceIn(0f, 1f)
    }

    val animatedFuel by animateFloatAsState(
        targetValue = displayFuel,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = 90f),
        label = "fuel"
    )

    // Иконка заправки
    val fuelIcon = painterResource(R.drawable.fuel_50)

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            val outerRadius = (geometry.width - 2 * geometry.margin) / 2f
            val cy = geometry.center.y

            val dyTop = geometry.margin - cy
            val dyBottom = (geometry.height - geometry.margin) - cy

            val angleFromVerticalTop = acos((dyTop / outerRadius).coerceIn(-1f, 1f))
            val angleFromVerticalBottom = acos((dyBottom / outerRadius).coerceIn(-1f, 1f))

            val startAngleDeg = 90f + Math.toDegrees(angleFromVerticalBottom.toDouble()).toFloat()
            val endAngleDeg = 90f + Math.toDegrees(angleFromVerticalTop.toDouble()).toFloat()
            val sweepAngle = endAngleDeg - startAngleDeg

            val tickRadius = outerRadius - geometry.outerStrokeWidth / 2f - geometry.gapScale

            // === УРОВЕНЬ ТОПЛИВА И УГЛЫ ===
            val fuelPercent = animatedFuel
            val fuelAngle = startAngleDeg + fuelPercent * sweepAngle
            val fuelNormInRange = (fuelAngle - startAngleDeg) / sweepAngle
            val startNorm = startAngleDeg / 360f
            val endNorm = endAngleDeg / 360f
            val glowCenterNorm = startNorm + fuelNormInRange * (endNorm - startNorm)

            // === 1. ВНЕШНЯЯ БЕЛАЯ ДУГА С ДИНАМИЧЕСКИМ ЦЕНТРОМ СВЕЧЕНИЯ ===
            val sweepBrush = Brush.sweepGradient(
                colorStops = arrayOf(
                    Pair(0f, Color.Transparent),
                    Pair(startNorm, Color.Transparent),
                    Pair(glowCenterNorm - 0.05f, Color.White.copy(alpha = 0.3f)),
                    Pair(glowCenterNorm, Color.White),
                    Pair(glowCenterNorm + 0.05f, Color.White.copy(alpha = 0.3f)),
                    Pair(endNorm, Color.Transparent),
                    Pair(1f, Color.Transparent)
                ),
                center = geometry.center
            )

            drawArc(
                brush = sweepBrush,
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

            // === 2. МЕТКИ И ЦИФРЫ ===
            for (step in 0..40) {
                val percent = step * 2.5f
                val angle = startAngleDeg + (percent / 100f) * sweepAngle
                val rad = Math.toRadians(angle.toDouble()).toFloat()
                val cosA = cos(rad)
                val sinA = sin(rad)

                val tickLength = when {
                    percent % 25f == 0f -> geometry.tickLarge
                    percent % 12.5f == 0f -> geometry.tickMedium
                    else -> geometry.tickSmall
                }
                val tickStroke = when (tickLength) {
                    geometry.tickLarge -> 2f * geometry.unit
                    geometry.tickMedium -> 1.5f * geometry.unit
                    else -> 1f * geometry.unit
                }

                val startTick = Offset(
                    geometry.center.x + tickRadius * cosA,
                    geometry.center.y + tickRadius * sinA
                )
                val endTick = Offset(
                    geometry.center.x + (tickRadius - tickLength) * cosA,
                    geometry.center.y + (tickRadius - tickLength) * sinA
                )

                drawLine(
                    color = geometry.ringColor,
                    start = startTick,
                    end = endTick,
                    strokeWidth = tickStroke,
                    cap = StrokeCap.Round
                )

                // Цифры только для 0 и 100
                if (percent % 100f == 0f) {
                    val text = when (percent.toInt()) {
                        0 -> "0"
                        100 -> "1"
                        else -> ""
                    }
                    val textPaint = android.graphics.Paint().apply {
                        color = geometry.ringColor.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                        this.textSize = geometry.textSizePx
                        isAntiAlias = true
                    }
                    // Исправлен отступ: теперь как в спидометре (geometry.textOffsetFromTick без множителя)
                    val textRadius = tickRadius - tickLength - geometry.textOffsetFromTick
                    val textX = geometry.center.x + textRadius * cosA
                    val textY = geometry.center.y + textRadius * sinA

                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        translate(textX, textY)
                        val fm = textPaint.fontMetrics
                        val baseline = -(fm.ascent + fm.descent) / 2f
                        drawText(text, 0f, baseline, textPaint)
                        restore()
                    }
                }

                // Иконка заправки на месте 50%
                if (abs(percent - 50f) < 0.1f) {
                    // Отступ между иконкой и риской оставляем как был (1.25f), так как исправлять не нужно
                    val iconRadius = tickRadius - tickLength - geometry.textOffsetFromTick * 1.25f
                    val iconX = geometry.center.x + iconRadius * cosA
                    val iconY = geometry.center.y + iconRadius * sinA
                    val iconSize = geometry.textSizePx * 1.2f

                    translate(left = iconX - iconSize / 2f, top = iconY - iconSize / 2f) {
                        with(fuelIcon) {
                            draw(
                                size = Size(iconSize, iconSize),
                                colorFilter = ColorFilter.tint(geometry.ringColor)
                            )
                        }
                    }
                }
            }

            // === 3. GLOW ===
            // Выравниваем разницу между шкалой и началом свечения так же, как в спидометре
            val speedoDiff = geometry.scaleRadius - geometry.ringRadius
            val edgeRadius = tickRadius - speedoDiff
            
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
                startAngle = startAngleDeg,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(
                    geometry.center.x - strokeRadius,
                    geometry.center.y - strokeRadius
                ),
                size = Size(strokeRadius * 2, strokeRadius * 2),
                style = Stroke(width = glowDepth)
            )

            // === 4. ТЕКСТ С ОСТАТКОМ ТОПЛИВА ===
            val fuelText = "${currentFuel.toInt()} л"

            val fuelPaint = android.graphics.Paint().apply {
                // Цвет как у единиц измерения скорости (км/ч) - Color.Gray
                color = Color.Gray.toArgb()
                textAlign = android.graphics.Paint.Align.LEFT
                textSize = geometry.textSizePx
                isAntiAlias = true
            }

            // Координаты центра цифры "1"
            val angle1 = endAngleDeg
            val rad1 = Math.toRadians(angle1.toDouble()).toFloat()
            // Исправлен отступ: теперь совпадает с цифрой 1
            val r1 = tickRadius - geometry.tickLarge - geometry.textOffsetFromTick
            val x1 = geometry.center.x + r1 * cos(rad1)
            val y1 = geometry.center.y + r1 * sin(rad1)

            // Позиционируем на том же вертикальном уровне (Y) как и цифра "1"
            val targetY = y1
            // Сдвигаем вправо, чтобы не перекрывать цифру "1" (этот отступ не меняем)
            val targetX = x1 + geometry.textSizePx * 0.8f

            drawContext.canvas.nativeCanvas.apply {
                save()
                val fm = fuelPaint.fontMetrics
                // Центрируем текст по вертикали относительно targetY (так же как и цифру "1")
                val baseline = targetY - (fm.ascent + fm.descent) / 2f
                drawText(fuelText, targetX, baseline, fuelPaint)
                restore()
            }

            // === 5. СТРЕЛКА ===
            val fuelRad = Math.toRadians(fuelAngle.toDouble()).toFloat()
            val cosFuel = cos(fuelRad)
            val sinFuel = sin(fuelRad)

            val glowStartRadius = edgeRadius
            val scaleOuterRadius = tickRadius

            val tip = Offset(
                geometry.center.x + scaleOuterRadius * cosFuel,
                geometry.center.y + scaleOuterRadius * sinFuel
            )

            val start = Offset(
                geometry.center.x + glowStartRadius * cosFuel,
                geometry.center.y + glowStartRadius * sinFuel
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        geometry.ringColor.copy(alpha = 0f),
                        geometry.ringColor.copy(alpha = 0.5f)
                    ),
                    start = start,
                    end = tip
                ),
                start = start,
                end = tip,
                strokeWidth = 5f * geometry.unit,
                cap = StrokeCap.Round
            )

            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White),
                    start = start,
                    end = tip
                ),
                start = start,
                end = tip,
                strokeWidth = 1.5f * geometry.unit,
                cap = StrokeCap.Round
            )
        }
    }
}
