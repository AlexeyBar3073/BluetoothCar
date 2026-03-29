// Файл: MainActivity.kt
package com.alexbar3073.bluetoothcar

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.alexbar3073.bluetoothcar.core.CoreModule
import com.alexbar3073.bluetoothcar.navigation.SetupNavigation
import com.alexbar3073.bluetoothcar.ui.screens.PermissionsScreen
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * ФАЙЛ: MainActivity.kt
 * МЕСТОНАХОЖДЕНИЕ: корневая папка проекта
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Главная активность приложения BluetoothCar. Точка входа в приложение.
 *
 * ИЗМЕНЕНИЯ ДЛЯ КОРРЕКТНОЙ ИНИЦИАЛИЗАЦИИ:
 * 1. УДАЛЕНЫ жесткие задержки (delay)
 * 2. Добавлено ожидание инициализации AppController через StateFlow
 * 3. Правильная последовательность: CoreModule → AppController → SharedViewModel
 * 4. Улучшена обработка ошибок инициализации
 *
 * ПРАВИЛЬНАЯ ПОСЛЕДОВАТЕЛЬНОСТЬ:
 * 1. Инициализация CoreModule (создает AppController)
 * 2. Ожидание, пока AppController.isInitialized станет true
 * 3. Запрос разрешений (если нужно)
 * 4. Создание SharedViewModel (только когда AppController готов)
 * 5. Запуск основного приложения
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Инициализирует: CoreModule.kt → AppController.kt
 * 2. Ожидает: AppController.isInitialized StateFlow
 * 3. Создает: SharedViewModel через SharedViewModelFactory
 * 4. Отображает: UI через SetupNavigation
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * ... [существующая история] ...
 * - 2026.02.03 12:15 UTC: ИСПРАВЛЕНИЕ ИНИЦИАЛИЗАЦИИ СОГЛАСНО MVVM
 *   1. Удалены все жесткие задержки (delay)
 *   2. Добавлено ожидание AppController.isInitialized StateFlow
 *   3. SharedViewModel создается ТОЛЬКО после готовности AppController
 *   4. Улучшена обработка ошибок инициализации
 *   5. Добавлены статусы инициализации для лучшего UX
 * - 2026.02.06 14:55: ИСПРАВЛЕНИЕ СОЗДАНИЯ ФАБРИКИ
 *   1. Убрано remember {} при создании SharedViewModelFactory
 *   2. Фабрика создается напрямую для правильной работы при повороте экрана
 *   3. Удален дублирующий метод logWarning()
 *   4. Соответствует жизненному циклу из дополнения к ТЗ
 * - 2026.02.06 15:30: ИСПРАВЛЕНИЕ ОЧИСТКИ ПРИ ПОВОРОТЕ ЭКРАНА
 *   1. Добавлена проверка isFinishing() перед очисткой CoreModule
 *   2. CoreModule очищается только при реальном завершении приложения
 *   3. При повороте экрана CoreModule сохраняется, данные не теряются
 */
class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"

        private fun log(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            println("$timestamp [$TAG] $message")
        }

        private fun logError(message: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
            System.err.println("$timestamp [$TAG] ERROR: $message")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        log("=== ЗАПУСК ПРИЛОЖЕНИЯ ===")

        setContent {
            BluetoothCarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val context = LocalContext.current

                    // Состояния инициализации
                    var coreModuleState by remember { mutableStateOf<CoreModuleState>(CoreModuleState.NOT_STARTED) }
                    var appControllerState by remember { mutableStateOf<AppControllerState>(AppControllerState.WAITING) }
                    var hasPermissions by remember { mutableStateOf(checkAllBluetoothPermissions(context)) }

                    // Лаунчер для разрешений
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

                    // Эффект для инициализации CoreModule
                    LaunchedEffect(Unit) {
                        if (coreModuleState == CoreModuleState.NOT_STARTED) {
                            initializeCoreModule(context)
                            coreModuleState = CoreModuleState.INITIALIZED
                        }
                    }

                    // Эффект для ожидания инициализации AppController
                    LaunchedEffect(coreModuleState) {
                        if (coreModuleState == CoreModuleState.INITIALIZED &&
                            appControllerState == AppControllerState.WAITING) {
                            waitForAppControllerInitialization()
                            appControllerState = AppControllerState.READY
                        }
                    }

                    // Отображение соответствующего экрана
                    when {
                        // 1. Инициализация CoreModule
                        coreModuleState == CoreModuleState.NOT_STARTED -> {
                            LoadingScreen(message = "Инициализация ядра приложения...")
                        }

                        // 2. Инициализация AppController
                        coreModuleState == CoreModuleState.INITIALIZED &&
                                appControllerState == AppControllerState.WAITING -> {
                            LoadingScreen(message = "Инициализация бизнес-логики...")
                        }

                        // 3. Ошибка инициализации
                        appControllerState == AppControllerState.ERROR -> {
                            ErrorScreen(
                                message = "Ошибка инициализации приложения",
                                onRetry = {
                                    coreModuleState = CoreModuleState.NOT_STARTED
                                    appControllerState = AppControllerState.WAITING
                                }
                            )
                        }

                        // 4. Проверка разрешений
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

                        // 5. Основное приложение
                        appControllerState == AppControllerState.READY && hasPermissions -> {
                            MainApplicationScreen(context = context)
                        }
                    }
                }
            }
        }
    }

    /**
     * Инициализация CoreModule.
     */
    private suspend fun initializeCoreModule(context: android.content.Context) {
        log("Инициализация CoreModule...")
        try {
            CoreModule.initialize(context)
            log("CoreModule успешно инициализирован")
        } catch (e: Exception) {
            logError("Ошибка инициализации CoreModule: ${e.message}")
            throw e
        }
    }

    /**
     * Ожидание инициализации AppController через его StateFlow.
     */
    private suspend fun waitForAppControllerInitialization() {
        log("Ожидание инициализации AppController...")
        try {
            // Получаем AppController
            val appController = CoreModule.getAppController()
            if (appController == null) {
                throw IllegalStateException("AppController не создан CoreModule")
            }

            // Ждем, пока isInitialized станет true
            // Используем timeout 10 секунд
            val startTime = System.currentTimeMillis()
            val timeout = 10000L // 10 секунд

            while (System.currentTimeMillis() - startTime < timeout) {
                if (appController.isInitialized.value) {
                    log("AppController инициализирован успешно")
                    return
                }
                delay(100) // Проверяем каждые 100 мс
            }

            // Timeout
            throw IllegalStateException("Таймаут ожидания инициализации AppController")

        } catch (e: Exception) {
            logError("Ошибка ожидания AppController: ${e.message}")
            throw e
        }
    }

    /**
     * Экран загрузки.
     */
    @Composable
    private fun LoadingScreen(message: String) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
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
     * Экран ошибки.
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
     * Основной экран приложения.
     */
    @Composable
    private fun MainApplicationScreen(context: android.content.Context) {
        log("Запуск основного приложения")

        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Создаем фабрику без remember {}
        // При повороте экрана будет создана новая фабрика, но AppController останется тем же
        val viewModelFactory = SharedViewModelFactory(context)

        // Создаем SharedViewModel через фабрику
        // Теперь AppController гарантированно инициализирован
        val sharedViewModel: SharedViewModel = viewModel(
            factory = viewModelFactory
        )

        // Передаем SharedViewModel в навигацию
        SetupNavigation(
            sharedViewModel = sharedViewModel,
            context = context
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        log("Завершение работы приложения")

        // Очищаем CoreModule ТОЛЬКО при реальном завершении приложения
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
     * Состояние инициализации CoreModule.
     */
    private sealed class CoreModuleState {
        object NOT_STARTED : CoreModuleState()
        object INITIALIZED : CoreModuleState()
        object ERROR : CoreModuleState()
    }

    /**
     * Состояние инициализации AppController.
     */
    private sealed class AppControllerState {
        object WAITING : AppControllerState()
        object READY : AppControllerState()
        object ERROR : AppControllerState()
    }

    /**
     * Определяет ВСЕ необходимые разрешения Bluetooth.
     */
    private fun getAllRequiredPermissions(): Array<String> {
        val permissions = mutableListOf<String>()
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        } else {
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // РАЗРЕШЕНИЕ НА УВЕДОМЛЕНИЯ: Необходимо для работы Foreground Service на Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        return permissions.toTypedArray()
    }

    /**
     * Проверяет, получены ли ВСЕ необходимые разрешения Bluetooth.
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

            // Добавлена проверка уведомлений для Android 13+
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
