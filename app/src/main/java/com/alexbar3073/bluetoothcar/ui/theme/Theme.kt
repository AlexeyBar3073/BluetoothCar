// Файл: ui/theme/Theme.kt
package com.alexbar3073.bluetoothcar.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

/** 
 * Высота компактного топбара. 
 * Установлена в 48.dp по требованию пользователя.
 */
val COMPACT_TOP_BAR_HEIGHT = 48.dp

/**
 * Темная цветовая схема (основная для приложения).
 * Основана на темно-синих и угольных оттенках для комфорта в ночное время.
 */
private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Blue40,
    tertiary = Pink80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = Color.White.copy(alpha = 0.12f)
)

/**
 * Светлая цветовая схема.
 * Оптимизирована для использования днем при ярком солнце.
 * Использует высокий контраст и чистые поверхности.
 */
private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = Blue30,
    tertiary = Purple40,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    onSurfaceVariant = Color.Black.copy(alpha = 0.6f),
    outline = Color.Black.copy(alpha = 0.12f)
)

/**
 * Глубокая синяя темная схема.
 */
private val BlueDarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = Blue40,
    tertiary = Blue90,
    background = Blue10,
    surface = Blue20,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Blue95,
    onSurface = Blue90,
)

/**
 * Объект AppColors предоставляет доступ к специфическим цветам приложения,
 * которые выходят за рамки стандартной MaterialTheme.ColorScheme.
 */
object AppColors {
    /** Основной акцентный цвет приложения (адаптивный) */
    val PrimaryBlue: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
    
    /** Прозрачный акцентный цвет для подложек */
    val TransparentPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    
    /** Основной цвет текста */
    val TextPrimary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    
    /** Второстепенный цвет текста */
    val TextSecondary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
    
    /** Третичный (неактивный) цвет текста */
    val TextTertiary: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    
    // Статусные цвета
    val Success = Green80
    val SuccessAlpha = Color(0x224ADE80)
    val Warning = Yellow80
    val WarningAlpha = Color(0x22FACC15)
    val Error = Red80
    val ErrorAlpha = Color(0x22F87171)

    /** Светлая полупрозрачная поверхность */
    val SurfaceLight: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
    
    /** Средняя полупрозрачная поверхность */
    val SurfaceMedium: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    
    /** Темная полупрозрачная поверхность */
    val SurfaceDark: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    // Константы прозрачности белого (оставляем как есть, так как они специфичны)
    val WhiteAlpha10 = Color.White.copy(alpha = 0.1f)
    val WhiteAlpha20 = Color.White.copy(alpha = 0.2f)
    val WhiteAlpha30 = Color.White.copy(alpha = 0.3f)
    val WhiteAlpha60 = Color.White.copy(alpha = 0.6f)
    val WhiteAlpha80 = Color.White.copy(alpha = 0.8f)

    /** Цвета для оформления диалоговых окон */
    val DialogBackground: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface
    val DialogBorder: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline

    /** Градиент для подложек */
    val SurfaceGradient: List<Color> @Composable @ReadOnlyComposable get() = listOf(
        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
    )
    
    val BluetoothDeviceConnected = Blue80
    val BluetoothDeviceAvailable: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    // Цвета для спидометра (оставляем базовые, их можно будет адаптировать позже)
    val SpeedometerGradient = listOf(Blue80, Blue40)
    val SpeedometerDialStroke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val SpeedometerCenterStroke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
    val SpeedometerTickMajor: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
    val SpeedometerTickMinor: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    val SpeedometerTickSmall: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val SpeedometerArrow = Blue80
    val SpeedometerArrowStroke: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
}

/**
 * Возвращает адаптивный градиентный фон на основе текущей цветовой схемы.
 * Решает проблему "черного фона в светлой теме".
 */
@Composable
fun verticalGradientBackground(): Brush {
    val colorScheme = MaterialTheme.colorScheme
    
    // Проверяем, темная ли сейчас тема по цвету фона. 
    val isDark = colorScheme.background.toArgb() == DarkBackground.toArgb() || 
                 colorScheme.background.toArgb() == Blue10.toArgb()
    
    val colors = if (isDark) {
        listOf(
            colorScheme.background,
            colorScheme.background.copy(alpha = 0.8f),
            colorScheme.surface
        )
    } else {
        // Для светлой темы используем мягкий переход от фона к варианту поверхности
        listOf(
            colorScheme.background,
            colorScheme.surfaceVariant.copy(alpha = 0.5f),
            colorScheme.surface
        )
    }
    return Brush.verticalGradient(colors = colors)
}

/**
 * Главная тема приложения.
 * 
 * @param themeMode Режим темы: "dark", "light", "blue_dark".
 * @param dynamicColor Использовать ли динамические цвета (Android 12+). По умолчанию выключено.
 */
@Composable
fun BluetoothCarTheme(
    themeMode: String = "dark",
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    // Определение цветовой схемы
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (themeMode == "light") dynamicLightColorScheme(context) else dynamicDarkColorScheme(context)
        }
        themeMode == "light" -> LightColorScheme
        themeMode == "blue_dark" -> BlueDarkColorScheme
        else -> DarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            
            /** 
             * Синхронизация системного UI.
             * Устанавливаем цвет статус-бара и навигационной панели в цвет фона темы.
             */
            val statusBarColor = colorScheme.background.toArgb()
            
            @Suppress("DEPRECATION")
            window.statusBarColor = statusBarColor
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                @Suppress("DEPRECATION")
                window.navigationBarColor = statusBarColor
            }

            val controller = WindowCompat.getInsetsController(window, view)
            // Иконки (часы, заряд) должны быть темными на светлой теме и наоборот
            val isLightTheme = themeMode == "light"
            controller.isAppearanceLightStatusBars = isLightTheme
            controller.isAppearanceLightNavigationBars = isLightTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
