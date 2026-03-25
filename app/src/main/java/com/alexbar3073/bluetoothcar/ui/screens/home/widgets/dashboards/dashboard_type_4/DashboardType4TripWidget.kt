// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/screens/home/widgets/dashboards/dashboard_type_4/DashboardType4TripWidget.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * ТЕГ: Виджет поездки Тип 4
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Отображение данных о пробеге (Trip A/B, Total), запасе хода и расходе топлива.
 * Поддерживает переключение между Trip A и Trip B коротким нажатием
 * и сброс значения длительным нажатием.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun TripWidget(
    modifier: Modifier = Modifier,
    carData: CarData,
    appSettings: AppSettings?,
    geometry: DashboardType4Geometry,
    onTripReset: (String) -> Unit = {}
) {
    var showTripB by remember { mutableStateOf(false) }

    // Расчеты данных
    val fuelTankCapacity = appSettings?.fuelTankCapacity ?: 60f
    val consumption = if (carData.fuelConsumption > 0f) carData.fuelConsumption else 8.5f
    val maxPossibleRange = (fuelTankCapacity / consumption) * 100f
    val remainingRange = if (carData.remainingRange > 0f) carData.remainingRange else (carData.fuel / consumption) * 100f
    val rangeProgress = (remainingRange / maxPossibleRange).coerceIn(0f, 1f)

    val density = geometry.density.density
    
    // РАЗМЕРЫ ШРИФТОВ
    val valueFontSize = (geometry.textSizePx / density).sp
    val tripTotalValueFontSize = (geometry.textSizePx * 0.8f / density).sp
    val smallValueFontSize = (geometry.textSizePx * 0.7f / density).sp
    val unitFontSize = (geometry.textSizePx * 0.35f / density).sp
    val labelFontSize = (geometry.textSizePx * 0.48f / density).sp

    // ПАРАМЕТРЫ ГЕОМЕТРИИ (В DP)
    val marginDp = (geometry.margin / density).dp
    val tickMediumDp = (geometry.tickMedium / density).dp
    val tickLargeDp = (geometry.tickLarge / density).dp
    val gapHeightDp = ((geometry.textOffsetFromTick - geometry.textSizePx / 2f) / density).dp

    val valueAlpha = 0.8f

    // СТИЛИ ТЕКСТА
    val tightLabelStyle = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Top,
            trim = LineHeightStyle.Trim.FirstLineTop
        ),
        fontWeight = FontWeight.Bold,
        fontSize = labelFontSize,
        color = AppColors.TextTertiary,
        textAlign = TextAlign.Center
    )

    val tightValueStyle = TextStyle(
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Top,
            trim = LineHeightStyle.Trim.FirstLineTop
        ),
        fontWeight = FontWeight.Normal,
        fontSize = valueFontSize,
        color = Color.White.copy(alpha = valueAlpha),
        textAlign = TextAlign.Center
    )

    val tripTotalValueStyle = tightValueStyle.copy(fontSize = tripTotalValueFontSize)
    val smallValueStyle = tightValueStyle.copy(fontSize = smallValueFontSize)

    Box(modifier = modifier.fillMaxSize()) {
        // 1. ДЕКОРАТИВНЫЕ ГРАНИЦЫ
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = geometry.outerStrokeWidth
            val radius = geometry.tripArcRadius
            val screenCenter = Offset(x = size.width / 2f, y = -geometry.height / 2f)
            val dyTop = geometry.height / 2f
            val dyBottom = size.height - strokeWidth / 2f + dyTop
            val dxLine = sqrt(radius * radius - dyBottom * dyBottom)
            val arcRect = Rect(left = screenCenter.x - radius, top = screenCenter.y - radius, right = screenCenter.x + radius, bottom = screenCenter.y + radius)

            val angleBottomLeft = Math.toDegrees(atan2(dyBottom, -dxLine).toDouble()).toFloat()
            val angleTopLeft = Math.toDegrees(atan2(dyTop, geometry.margin - size.width / 2f).toDouble()).toFloat()
            val angleBottomRight = Math.toDegrees(atan2(dyBottom, dxLine).toDouble()).toFloat()
            val angleTopRight = Math.toDegrees(atan2(dyTop, size.width - geometry.margin - size.width / 2f).toDouble()).toFloat()

            drawArc(brush = Brush.sweepGradient(0.0f to Color.Transparent, (angleBottomLeft / 360f) to Color.White, (angleTopLeft / 360f) to Color.Transparent, center = screenCenter),
                startAngle = angleBottomLeft, sweepAngle = angleTopLeft - angleBottomLeft, useCenter = false, topLeft = arcRect.topLeft, size = arcRect.size, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            drawArc(brush = Brush.sweepGradient(0.0f to Color.Transparent, (angleTopRight / 360f) to Color.Transparent, (angleBottomRight / 360f) to Color.White, center = screenCenter),
                startAngle = angleTopRight, sweepAngle = angleBottomRight - angleTopRight, useCenter = false, topLeft = arcRect.topLeft, size = arcRect.size, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
            drawLine(color = Color.White, start = Offset(x = screenCenter.x - dxLine, y = size.height - strokeWidth / 2f), end = Offset(x = screenCenter.x + dxLine, y = size.height - strokeWidth / 2f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }

        // 2. КОНТЕНТ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = (geometry.margin / density).dp + 32.dp)
                .padding(top = 0.dp, bottom = 4.dp),
            verticalArrangement = Arrangement.Top
        ) {
            // ЛИНИЯ-ГРАНИЦА
            Canvas(modifier = Modifier.fillMaxWidth().height(1.5.dp)) {
                drawRect(brush = Brush.horizontalGradient(0f to Color.White.copy(alpha = 0.1f), 0.5f to Color.White, 1f to Color.White.copy(alpha = 0.1f)))
            }

            Spacer(modifier = Modifier.height(marginDp))

            Box(modifier = Modifier.fillMaxWidth()) {
                
                // --- TRIP (Слева) ---
                Column(modifier = Modifier.align(Alignment.TopStart), horizontalAlignment = Alignment.Start) {
                    Text(text = if (showTripB) "TRIP B, км" else "TRIP A, км", style = tightLabelStyle)
                    Spacer(modifier = Modifier.height(marginDp))
                    Text(
                        text = "%.1f".format(if (showTripB) carData.tripB else carData.tripA),
                        style = tripTotalValueStyle,
                        modifier = Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { showTripB = !showTripB },
                            onLongClick = {
                                val command = if (showTripB) "{\"trip_b\":\"reset\"}" else "{\"trip_a\":\"reset\"}"
                                onTripReset(command)
                            }
                        )
                    )
                }

                // --- TOTAL (Справа) ---
                Column(modifier = Modifier.align(Alignment.TopEnd), horizontalAlignment = Alignment.End) {
                    Text(text = "TOTAL, км", style = tightLabelStyle)
                    Spacer(modifier = Modifier.height(marginDp))
                    Text(text = "${carData.odometer.toInt()}", style = tripTotalValueStyle)
                }

                // --- ЦЕНТРАЛЬНАЯ ЧАСТЬ (Индикатор и его метки) ---
                Text(text = "FUEL, л", style = tightLabelStyle, modifier = Modifier.align(BiasAlignment(-0.75f, -1f)))
                Text(text = "AVG, л/100", style = tightLabelStyle, modifier = Modifier.align(BiasAlignment(0.75f, -1f)))

                // СЕГМЕНТНЫЙ ИНДИКАТОР
                Box(modifier = Modifier.fillMaxWidth(0.5f).height(tickLargeDp).align(Alignment.TopCenter)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val segments = 12
                        val gap = 1.5f * geometry.unit
                        val segW = (size.width - (segments - 1) * gap) / segments
                        val baseColor = Color.White
                        
                        for (i in 0 until segments) {
                            val startX = i * (segW + gap)
                            val endX = startX + segW
                            val currentProgress = rangeProgress * size.width
                            
                            val isFull = endX <= currentProgress
                            val isPartial = startX < currentProgress && endX > currentProgress
                            
                            if (isFull) {
                                // Полностью активный сектор
                                drawRect(color = baseColor.copy(alpha = 0.8f), topLeft = Offset(startX, 0f), size = Size(segW, tickMediumDp.toPx()))
                            } else if (isPartial) {
                                // Частично активный сектор (тот самый "таящий")
                                val partialWidth = currentProgress - startX
                                // Отрисовываем активную часть
                                drawRect(color = baseColor.copy(alpha = 0.8f), topLeft = Offset(startX, 0f), size = Size(partialWidth, tickMediumDp.toPx()))
                                // Отрисовываем неактивную часть
                                drawRect(color = baseColor.copy(alpha = 0.15f), topLeft = Offset(startX + partialWidth, 0f), size = Size(segW - partialWidth, tickMediumDp.toPx()))
                            } else {
                                // Неактивный сектор
                                drawRect(color = baseColor.copy(alpha = 0.15f), topLeft = Offset(startX, 0f), size = Size(segW, tickMediumDp.toPx()))
                            }
                        }
                        
                        val rH = geometry.tickLarge
                        drawLine(color = baseColor.copy(alpha = 0.8f), start = Offset(0f, 0f), end = Offset(0f, rH), strokeWidth = 1.2f * geometry.unit)
                        drawLine(color = baseColor.copy(alpha = 0.8f), start = Offset(size.width, 0f), end = Offset(size.width, rH), strokeWidth = 1.2f * geometry.unit)
                    }
                }

                // --- ТРЕУГОЛЬНИК (Теперь строго совпадает с прогрессом индикатора) ---
                Box(modifier = Modifier.align(Alignment.TopCenter).padding(top = tickLargeDp).fillMaxWidth(0.5f).height(gapHeightDp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val baseColor = Color.White
                        val triangleX = rangeProgress * size.width
                        val triangleSize = 8.dp.toPx()
                        
                        val path = Path().apply {
                            moveTo(triangleX, 0f)
                            lineTo(triangleX + triangleSize / 2f, size.height)
                            lineTo(triangleX - triangleSize / 2f, size.height)
                            close()
                        }
                        drawPath(path, color = baseColor.copy(alpha = 0.8f))
                    }
                }

                // --- НИЖНИЙ РЯД (Fuel, 0, Rem, Max, Avg) ---
                val bottomRowOffset = tickLargeDp + gapHeightDp
                Box(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(top = bottomRowOffset)) {
                    Text(text = "${carData.fuel.toInt()}", style = tightValueStyle, modifier = Modifier.align(BiasAlignment(-0.75f, -1f)))
                    Text(text = "0", style = tightValueStyle, modifier = Modifier.align(BiasAlignment(-0.5f, -1f)))
                    
                    val isVisible = rangeProgress > 0.18f && rangeProgress < 0.82f
                    if (isVisible) {
                        Text(
                            text = buildAnnotatedString {
                                append("${remainingRange.toInt()}")
                                withStyle(tightValueStyle.copy(fontSize = unitFontSize, color = AppColors.TextSecondary.copy(alpha = valueAlpha)).toSpanStyle()) { append(" км") }
                            },
                            style = smallValueStyle,
                            modifier = Modifier.align(BiasAlignment(-0.5f + rangeProgress, -1f))
                        )
                    }

                    Text(text = "${maxPossibleRange.toInt()}", style = tightValueStyle, modifier = Modifier.align(BiasAlignment(0.5f, -1f)))
                    Text(text = "%.1f".format(consumption), style = tightValueStyle, modifier = Modifier.align(BiasAlignment(0.75f, -1f)))
                }
            }
        }
    }
}
