// Файл: ui/screens/home/widgets/StatusCircleButton.kt
package com.alexbar3073.bluetoothcar.ui.screens.home.widgets

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.data.bluetooth.CompactDisplayType
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionStatusInfo
import com.alexbar3073.bluetoothcar.ui.components.TopBarButton

/**
 * ТЕГ: Home/Widgets/StatusButton
 * 
 * ФАЙЛ: ui/screens/home/widgets/StatusCircleButton.kt
 * 
 * МЕСТОНАХОЖДЕНИЕ: ui/screens/home/widgets/
 * 
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ: Круглая кнопка-индикатор статуса Bluetooth подключения.
 * Визуализирует текущее состояние (подключено, поиск, ошибка) и позволяет инициировать переподключение.
 * 
 * ОТВЕТСТВЕННОСТЬ: Индикация статуса через иконки и цвета, обработка клика для повторного подключения.
 * 
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Stateless компонент, построенный на базе унифицированного TopBarButton.
 * 
 * КЛЮЧЕВОЙ ПРИНЦИП: Визуальная симметрия и идентичность кнопкам Топбара.
 * 
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ: Вызывается из HomeScreen.kt. Использует TopBarButton для базы.
 */

/**
 * Компактная кнопка статуса для Топбара.
 * 
 * @param connectionStatusInfo Информация о текущем статусе (иконка, цвет, тип отображения).
 * @param onClick Обработка нажатия на кнопку.
 * @param modifier Модификатор оформления.
 */
@Composable
fun StatusCircleButton(
    connectionStatusInfo: ConnectionStatusInfo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Анимация для циклического прогресс-индикатора при подключении
    val infiniteTransition = rememberInfiniteTransition(label = "status_progress")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress_value"
    )

    // Использование унифицированной базы кнопки Топбара (32/28 dp)
    TopBarButton(
        onClick = onClick,
        modifier = modifier
    ) {
        // Отрисовка контента (размер 16dp для полного соответствия кнопке настроек)
        when (connectionStatusInfo.compactDisplayType) {
            CompactDisplayType.ICON -> {
                Icon(
                    imageVector = connectionStatusInfo.compactIcon,
                    contentDescription = connectionStatusInfo.displayName,
                    tint = connectionStatusInfo.iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            CompactDisplayType.PROGRESS_INDICATOR -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    progress = { progress },
                    strokeWidth = 2.dp,
                    color = connectionStatusInfo.iconColor
                )
            }
            CompactDisplayType.CUSTOM_ANIMATION -> {
                Icon(
                    imageVector = connectionStatusInfo.compactIcon,
                    contentDescription = connectionStatusInfo.displayName,
                    tint = connectionStatusInfo.iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
