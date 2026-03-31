// Файл: app/src/main/java/com/alexbar3073/bluetoothcar/core/CoreModule.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context

/**
 * ТЕГ: Core+Init+CoreModule
 *
 * ФАЙЛ: CoreModule.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: app/src/main/java/com/alexbar3073/bluetoothcar/core/
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
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
        println("$timestamp [CoreModule] Начало инициализации ядра приложения")

        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Если AppController уже создан - возвращаем его
        if (appControllerInstance != null) {
            println("$timestamp [CoreModule] AppController уже создан, возвращаем существующий экземпляр")
            return appControllerInstance!!
        }

        // 1. Инициализируем ServiceLocator
        println("$timestamp [CoreModule] Шаг 1: Инициализация ServiceLocator")
        ServiceLocator.initialize(context)

        // 2. Получаем SettingsRepository из ServiceLocator
        println("$timestamp [CoreModule] Шаг 2: Получение SettingsRepository")
        val settingsRepository = ServiceLocator.getSettingsRepository()

        // 3. Создаем AppController (главный координатор системы)
        println("$timestamp [CoreModule] Шаг 3: Создание AppController")
        val appController = AppController(context.applicationContext, settingsRepository)

        // 4. Регистрируем AppController в ServiceLocator
        println("$timestamp [CoreModule] Шаг 4: Регистрация AppController в ServiceLocator")
        ServiceLocator.registerAppController(appController)

        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Сохраняем ссылку на AppController
        appControllerInstance = appController

        println("$timestamp [CoreModule] Инициализация ядра приложения завершена успешно")
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
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
        println("$timestamp [CoreModule] Очистка ядра приложения")

        try {
            // 1. Получаем и очищаем AppController
            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Используем сохраненный экземпляр для освобождения ресурсов
            appControllerInstance?.cleanup()

            // 2. Очищаем ссылку на AppController
            appControllerInstance = null

            // 3. Очищаем ServiceLocator
            ServiceLocator.clear()

            println("$timestamp [CoreModule] Ядро приложения очищено")
        } catch (e: IllegalStateException) {
            println("$timestamp [CoreModule] Ядро приложения уже очищено или не инициализировано")
        }
    }
}
