// Файл: core/CoreModule.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context
import com.alexbar3073.bluetoothcar.data.logging.AppLogger

/**
 * ТЕГ: Core+Init+CoreModule
 *
 * ФАЙЛ: core/CoreModule.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Модуль инициализации ядра приложения. Содержит точку входа для инициализации
 * всех core-компонентов при запуске приложения.
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Координирует инициализацию ServiceLocator и AppController
 * 2. Обеспечивает правильный порядок инициализации компонентов
 * 3. Предоставляет единую точку входа для инициализации ядра
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Service Locator / Singleton
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Единая точка входа для управления жизненным циклом ядра
 *
 * СВЯЗИ:
 * 1. Использует: ServiceLocator.kt, AppController.kt
 * 2. Вызывается из: MainActivity.kt при запуске приложения
 * 3. Взаимодействует: со всеми компонентами системы через AppController
 */
object CoreModule {

    private const val TAG = "CoreModule"

    // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Сохраняем AppController между вызовами. 
    // Volatile гарантирует видимость изменений для всех потоков.
    @Volatile
    private var appControllerInstance: AppController? = null

    /**
     * Инициализировать все core-компоненты приложения.
     * Должен вызываться один раз при запуске приложения (в onCreate MainActivity).
     *
     * @param context Контекст приложения (ApplicationContext предпочтительнее)
     * @return AppController - главный координатор системы
     */
    @Synchronized
    fun initialize(context: Context): AppController {
        AppLogger.logInfo("Начало инициализации ядра приложения", TAG)

        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Если AppController уже создан - возвращаем его
        if (appControllerInstance != null) {
            AppLogger.logInfo("AppController уже создан, возвращаем существующий экземпляр", TAG)
            return appControllerInstance!!
        }

        // 1. Инициализируем ServiceLocator
        AppLogger.logInfo("Шаг 1: Инициализация ServiceLocator", TAG)
        ServiceLocator.initialize(context)

        // 2. Получаем SettingsRepository из ServiceLocator
        AppLogger.logInfo("Шаг 2: Получение SettingsRepository", TAG)
        val settingsRepository = ServiceLocator.getSettingsRepository()

        // 3. Создаем AppController (главный координатор системы)
        AppLogger.logInfo("Шаг 3: Создание AppController", TAG)
        val appController = AppController(context.applicationContext, settingsRepository)

        // 4. Регистрируем AppController в ServiceLocator
        AppLogger.logInfo("Шаг 4: Регистрация AppController в ServiceLocator", TAG)
        ServiceLocator.registerAppController(appController)

        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Сохраняем ссылку на AppController
        appControllerInstance = appController

        AppLogger.logInfo("Инициализация ядра приложения завершена успешно", TAG)
        return appController
    }

    /**
     * Получить AppController из ServiceLocator.
     *
     * @return AppController
     * @throws IllegalStateException если ядро не инициализировано
     */
    fun getAppController(): AppController {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Возвращаем сохраненный AppController или выбрасываем ошибку
        return appControllerInstance ?: throw IllegalStateException("CoreModule не инициализирован")
    }

    /**
     * Проверить, инициализировано ли ядро приложения.
     *
     * @return true если ядро инициализировано, false в противном случае
     */
    fun isInitialized(): Boolean {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Проверяем наличие валидной ссылки на экземпляр контроллера
        return appControllerInstance != null
    }

    /**
     * Очистить все core-компоненты.
     * Вызывается при завершении работы приложения или для тестирования.
     */
    @Synchronized
    fun cleanup() {
        AppLogger.logInfo("Очистка ядра приложения", TAG)

        try {
            // 1. Получаем и очищаем AppController
            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Используем сохраненный экземпляр для освобождения ресурсов
            appControllerInstance?.cleanup()

            // 2. Очищаем ссылку на AppController
            appControllerInstance = null

            // 3. Очищаем ServiceLocator
            ServiceLocator.clear()

            AppLogger.logInfo("Ядро приложения очищено", TAG)
        } catch (e: IllegalStateException) {
            AppLogger.logWarning("Ядро приложения уже очищено или не инициализировано", TAG)
        }
    }
}
