// Файл: MainActivity.kt
package com.alexbar3073.bluetoothcar

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexbar3073.bluetoothcar.core.CoreModule
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.navigation.SetupNavigation
import com.alexbar3073.bluetoothcar.ui.screens.PermissionsScreen
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModelFactory
import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException

/**
 * ТЕГ: Главная активность / MainActivity
 *
 * ФАЙЛ: MainActivity.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: /
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Главная точка входа в приложение BluetoothCar. Отвечает за инициализацию ядра (CoreModule),
 * управление жизненным циклом приложения, запрос критических разрешений и выбор темы оформления.
 * Использует Jetpack Compose для отрисовки всего UI.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Инициализация CoreModule и ожидание готовности AppController.
 * 2. Обработка разрешений Bluetooth и уведомлений.
 * 3. Реактивное управление темой приложения на основе настроек пользователя.
 * 4. Контейнер для графа навигации.
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Single Activity Architecture (MVVM)
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Поэтапная инициализация (Core -> Permissions -> UI) для предотвращения ошибок доступа к ресурсам.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: CoreModule.kt (инициализация), SharedViewModel.kt (бизнес-логика), SetupNavigation.kt (навигация).
 * - Вызывается из: Android OS при запуске.
 * - Взаимодействует: BluetoothCarTheme.kt (применение тем).
 */

class MainActivity : ComponentActivity() {

    companion object {
        /** Тег для логирования событий активности */
        private const val TAG = "MainActivity"

        /**
         * Вспомогательный метод для логирования обычных событий.
         * @param message Текст сообщения.
         */
        private fun log(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            println("$timestamp [$TAG] $message")
        }

        /**
         * Вспомогательный метод для логирования ошибок.
         * @param message Текст ошибки.
         */
        private fun logError(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            System.err.println("$timestamp [$TAG] ERROR: $message")
        }
    }

    /**
     * Точка входа в жизненный цикл Activity.
     * Здесь настраивается Compose контент и запускается процесс инициализации.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log("=== ЗАПУСК ПРИЛОЖЕНИЯ ===")

        setContent {
            // Состояния для управления этапами загрузки (вынесено выше для ключей remember)
            var coreModuleState by remember { mutableStateOf<CoreModuleState>(CoreModuleState.NOT_STARTED) }
            var appControllerState by remember { mutableStateOf<AppControllerState>(AppControllerState.WAITING) }

            // 1. Получаем ссылку на AppController.
            // Ключ appControllerState заставляет remember пересчитаться после завершения инициализации.
            val appController = remember(appControllerState) { 
                if (CoreModule.isInitialized()) CoreModule.getAppController() else null 
            }
            
            // 2. Подписываемся на поток настроек. 
            // Благодаря реактивности, тема обновится сразу после того, как appController станет доступен.
            val settings by if (appController != null) {
                appController.appSettings.collectAsStateWithLifecycle()
            } else {
                remember { mutableStateOf(AppSettings()) }
            }

            // 3. Оборачиваем всё приложение в тему, передавая выбранный пользователем режим
            BluetoothCarTheme(themeMode = settings.selectedTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current
                    var hasPermissions by remember { mutableStateOf(checkAllBluetoothPermissions(context)) }

                    // Лаунчер для запроса группы разрешений
                    val permissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestMultiplePermissions()
                    ) { permissions ->
                        hasPermissions = permissions.values.all { it }
                        if (hasPermissions) {
                            log("Все разрешения Bluetooth получены")
                        } else {
                            log("Не все разрешения получены")
                        }
                    }

                    // ШАГ 1: Инициализация CoreModule при старте
                    LaunchedEffect(Unit) {
                        if (coreModuleState == CoreModuleState.NOT_STARTED) {
                            try {
                                initializeCoreModule(context)
                                coreModuleState = CoreModuleState.INITIALIZED
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                logError("Ошибка инициализации ядра: ${e.message}")
                                coreModuleState = CoreModuleState.ERROR
                            }
                        }
                    }

                    // ШАГ 2: Ожидание готовности бизнес-логики (AppController)
                    LaunchedEffect(coreModuleState) {
                        if (coreModuleState == CoreModuleState.INITIALIZED &&
                            appControllerState == AppControllerState.WAITING) {
                            try {
                                waitForAppControllerInitialization()
                                appControllerState = AppControllerState.READY
                            } catch (e: Exception) {
                                if (e is CancellationException) throw e
                                logError("Ошибка инициализации логики: ${e.message}")
                                appControllerState = AppControllerState.ERROR
                            }
                        }
                    }

                    // Отрисовка UI в зависимости от текущего состояния инициализации
                    when {
                        // Состояние: Загрузка ядра
                        coreModuleState == CoreModuleState.NOT_STARTED -> {
                            LoadingScreen(message = "Инициализация ядра приложения...")
                        }

                        // Состояние: Загрузка логики
                        coreModuleState == CoreModuleState.INITIALIZED &&
                                appControllerState == AppControllerState.WAITING -> {
                            LoadingScreen(message = "Инициализация бизнес-логики...")
                        }

                        // Состояние: Критическая ошибка
                        appControllerState == AppControllerState.ERROR || coreModuleState == CoreModuleState.ERROR -> {
                            ErrorScreen(
                                message = "Ошибка инициализации приложения",
                                onRetry = {
                                    coreModuleState = CoreModuleState.NOT_STARTED
                                    appControllerState = AppControllerState.WAITING
                                }
                            )
                        }

                        // Состояние: Ожидание разрешений
                        appControllerState == AppControllerState.READY && !hasPermissions -> {
                            PermissionsScreen(
                                onRequestPermissions = {
                                    permissionLauncher.launch(getAllRequiredPermissions())
                                },
                                onSkip = {
                                    log("Пользователь пропустил разрешения")
                                    hasPermissions = true
                                }
                            )
                        }

                        // Состояние: Готово к работе
                        appControllerState == AppControllerState.READY && hasPermissions -> {
                            MainApplicationScreen(context = context)
                        }
                    }
                }
            }
        }
    }

    /**
     * Выполняет первичную инициализацию CoreModule.
     * @param context Контекст приложения.
     */
    private suspend fun initializeCoreModule(context: android.content.Context) {
        log("Инициализация CoreModule...")
        try {
            // Вызов статического метода инициализации синглтона
            CoreModule.initialize(context)
            log("CoreModule успешно инициализирован")
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("Ошибка инициализации CoreModule: ${e.message}")
            throw e
        }
    }

    /**
     * Ожидает, пока AppController внутри CoreModule закончит свои внутренние процессы.
     * Использует механизм опроса состояния Flow.
     */
    private suspend fun waitForAppControllerInitialization() {
        log("Ожидание инициализации AppController...")
        try {
            // Пытаемся получить экземпляр контроллера
            val appController = CoreModule.getAppController()
            if (appController == null) {
                throw IllegalStateException("AppController не создан CoreModule")
            }

            // Реализуем таймаут ожидания в 10 секунд
            val startTime = System.currentTimeMillis()
            val timeout = 10000L 

            while (System.currentTimeMillis() - startTime < timeout) {
                // Проверяем флаг инициализации из контроллера
                if (appController.isInitialized.value) {
                    log("AppController инициализирован успешно")
                    return
                }
                // Небольшая задержка перед следующей проверкой для экономии ресурсов
                delay(100)
            }

            // Если за 10 секунд не инициализировались — выбрасываем исключение
            throw IllegalStateException("Таймаут ожидания инициализации AppController")

        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("Ошибка ожидания AppController: ${e.message}")
            throw e
        }
    }

    /**
     * Компонент экрана загрузки со спиннером.
     * @param message Текст, поясняющий этап загрузки.
     */
    @Composable
    private fun LoadingScreen(message: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            @Suppress("DEPRECATION")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    /**
     * Компонент экрана ошибки с кнопкой повтора.
     * @param message Описание возникшей ошибки.
     * @param onRetry Действие при нажатии кнопки "Повторить".
     */
    @Composable
    private fun ErrorScreen(message: String, onRetry: () -> Unit) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Ошибка",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = message,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                androidx.compose.material3.Button(onClick = onRetry) {
                    Text("Повторить")
                }
            }
        }
    }

    /**
     * Основная точка входа в UI после успешной инициализации и получения разрешений.
     * Создает SharedViewModel и запускает навигацию.
     * @param context Контекст для создания фабрики ViewModel.
     */
    @Composable
    private fun MainApplicationScreen(context: android.content.Context) {
        log("Запуск основного приложения")

        // 1. Создаем фабрику для SharedViewModel (без remember, согласно ТЗ по жизненному циклу)
        val viewModelFactory = SharedViewModelFactory(context)

        // 2. Инициализируем ViewModel через стандартный провайдер
        val sharedViewModel: SharedViewModel = viewModel(
            factory = viewModelFactory
        )

        // 3. Запускаем навигационный граф
        SetupNavigation(
            sharedViewModel = sharedViewModel,
            context = context
        )
    }

    /**
     * Обработка завершения работы Activity.
     * Выполняет очистку ресурсов, если приложение действительно закрывается.
     */
    override fun onDestroy() {
        super.onDestroy()
        log("Завершение работы приложения")

        // Очищаем ресурсы CoreModule только если это не поворот экрана
        if (isFinishing()) {
            try {
                CoreModule.cleanup()
                log("CoreModule очищен (реальное завершение приложения)")
            } catch (e: Exception) {
                logError("Ошибка очистки CoreModule: ${e.message}")
            }
        } else {
            log("Изменение конфигурации (поворот экрана), CoreModule сохраняется")
        }
    }

    /**
     * Перечисление состояний инициализации CoreModule.
     */
    private sealed class CoreModuleState {
        object NOT_STARTED : CoreModuleState()
        object INITIALIZED : CoreModuleState()
        object ERROR : CoreModuleState()
    }

    /**
     * Перечисление состояний готовности AppController.
     */
    private sealed class AppControllerState {
        object WAITING : AppControllerState()
        object READY : AppControllerState()
        object ERROR : AppControllerState()
    }

    /**
     * Формирует список всех разрешений, необходимых приложению в зависимости от версии Android.
     * @return Массив строк с названиями разрешений.
     */
    private fun getAllRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        // Для Android 12 (API 31) и выше нужны специфичные Bluetooth разрешения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        } else {
            // Для старых версий достаточно классических разрешений
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Разрешение на уведомления для Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    /**
     * Выполняет проверку статуса всех необходимых разрешений.
     * @param context Контекст для проверки.
     * @return true, если все разрешения даны пользователем.
     */
    private fun checkAllBluetoothPermissions(context: android.content.Context): Boolean {
        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScan = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasConnect = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasNotifications = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else true

            hasScan && hasConnect && hasLocation && hasNotifications

        } else {
            val hasLocation = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasBluetooth = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            val hasBluetoothAdmin = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            hasLocation && hasBluetooth && hasBluetoothAdmin
        }

        log("Результат проверки разрешений: $result")
        return result
    }
}
