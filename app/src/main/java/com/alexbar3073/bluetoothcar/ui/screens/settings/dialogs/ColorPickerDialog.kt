// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/ui/screens/settings/dialogs/ColorPickerDialog.kt
package com.alexbar3073.bluetoothcar.ui.screens.settings.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.CarData
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4.DashboardType4CombinedGauge
import com.alexbar3073.bluetoothcar.ui.screens.home.widgets.dashboards.dashboard_type_4.DashboardType4Geometry
import com.alexbar3073.bluetoothcar.ui.theme.AppColors
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme

/**
 * ТЕГ: Диалог выбора цвета
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Реализация профессионального диалога выбора цвета (HSV Picker) для настройки 
 * основного цвета оформления приборов. Включает в себя панель насыщенности/яркости, 
 * слайдер оттенка и живой предпросмотр на базе комбинированного прибора.
 * 
 * СВЯЗЬ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Вызывается из: WidgetsSection.kt (при нажатии на пункт выбора цвета).
 * 2. Использует: DashboardType4CombinedGauge.kt для отображения результата в реальном времени.
 * 3. Взаимодействует: AppSettings для получения цветов по умолчанию и текущего.
 */

/**
 * Основная функция-обертка для вызова диалога.
 * @param appSettings Объект настроек приложения.
 * @param onDismiss Закрытие без сохранения.
 * @param onColorSelected Применение выбранного цвета.
 */
@Composable
fun ColorPickerDialog(
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        ColorPickerContent(
            appSettings = appSettings,
            onDismiss = onDismiss,
            onColorSelected = onColorSelected
        )
    }
}

/**
 * Основное содержимое диалога выбора цвета.
 * Содержит область предпросмотра и инструменты управления HSV.
 */
@Composable
fun ColorPickerContent(
    appSettings: AppSettings,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    val initialColor = Color(appSettings.currentDashboardColor)
    val defaultColor = Color(appSettings.defaultDashboardColor)

    // Состояние HSV для текущего подбора
    val hsv = remember {
        val hsvArray = FloatArray(3)
        android.graphics.Color.colorToHSV(initialColor.toArgb(), hsvArray)
        mutableStateListOf(hsvArray[0], hsvArray[1], hsvArray[2])
    }

    // Результирующий цвет подбора
    val selectedColor = remember(hsv[0], hsv[1], hsv[2]) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], hsv[2])))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(0.95f)
            .fillMaxHeight(0.85f)
            .padding(8.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A24))
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // --- ЛЕВАЯ ЧАСТЬ: Предпросмотр ---
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.3f))
                    .border(1.dp, AppColors.WhiteAlpha10, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                val density = LocalDensity.current
                val geometry = remember(selectedColor) {
                    DashboardType4Geometry.fromSize(
                        size = Size(240f * density.density, 240f * density.density),
                        density = density,
                        ringColor = selectedColor
                    )
                }

                DashboardType4CombinedGauge(
                    modifier = Modifier.size(220.dp),
                    carData = CarData(fuel = 45f, coolantTemp = 90f, transmissionTemp = 85f),
                    appSettings = appSettings,
                    geometry = geometry
                )
            }

            // --- ПРАВАЯ ЧАСТЬ: Управление цветом ---
            Column(
                modifier = Modifier
                    .weight(1.2f)
                    .fillMaxHeight()
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SaturationValuePanel(
                        hue = hsv[0],
                        saturation = hsv[1],
                        value = hsv[2],
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onChanged = { s, v -> hsv[1] = s; hsv[2] = v }
                    )

                    VerticalHueBar(
                        hue = hsv[0],
                        modifier = Modifier.width(32.dp).fillMaxHeight(),
                        onHueChanged = { hsv[0] = it }
                    )
                }

                // --- НИЖНЯЯ ПАНЕЛЬ КНОПОК ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("ОТМЕНА", color = AppColors.TextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // СБРОС: устанавливает значения HSV из дефолтного цвета настроек
                    Button(
                        onClick = {
                            val hsvArray = FloatArray(3)
                            android.graphics.Color.colorToHSV(defaultColor.toArgb(), hsvArray)
                            hsv[0] = hsvArray[0]
                            hsv[1] = hsvArray[1]
                            hsv[2] = hsvArray[2]
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = defaultColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("СБРОС", color = Color.White)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onColorSelected(selectedColor)
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(40.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("ПРИМЕНИТЬ", color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * Панель выбора Насыщенности (Saturation) и Яркости (Value).
 */
@Composable
fun SaturationValuePanel(
    hue: Float,
    saturation: Float,
    value: Float,
    modifier: Modifier = Modifier,
    onChanged: (Float, Float) -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val s = (change.position.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onChanged(s, v)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val s = (offset.x / size.width).coerceIn(0f, 1f)
                    val v = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onChanged(s, v)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val hsvColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hsvColor)))
            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))

            val x = saturation * size.width
            val y = (1f - value) * size.height
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Вертикальный слайдер выбора Тона (Hue).
 */
@Composable
fun VerticalHueBar(
    hue: Float,
    modifier: Modifier = Modifier,
    onHueChanged: (Float) -> Unit
) {
    val rainbowColors = remember {
        listOf(Color.Red, Color.Magenta, Color.Blue, Color.Cyan, Color.Green, Color.Yellow, Color.Red)
    }

    Box(
        modifier = modifier
            .clip(CircleShape)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val h = (change.position.y / size.height).coerceIn(0f, 1f) * 360f
                    onHueChanged(h)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = (offset.y / size.height).coerceIn(0f, 1f) * 360f
                    onHueChanged(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(brush = Brush.verticalGradient(rainbowColors))
            val y = (hue / 360f) * size.height
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(size.width / 2, y),
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

/**
 * Превью диалога выбора цвета в ландшафтной ориентации.
 */
@Preview(showBackground = true, widthDp = 800, heightDp = 480, backgroundColor = 0xFF0A0A0F)
@Composable
fun ColorPickerDialogPreview() {
    BluetoothCarTheme {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            ColorPickerContent(
                appSettings = AppSettings(),
                onDismiss = {},
                onColorSelected = {}
            )
        }
    }
}
