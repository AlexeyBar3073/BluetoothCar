// Файл: core/CoreModule.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context

/**
 * ФАЙЛ: core/CoreModule.kt
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
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Использует: ServiceLocator.kt, AppController.kt
 * 2. Вызывается из: MainActivity.kt при запуске приложения
 * 3. Инициализирует: все core-компоненты в правильном порядке
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.02 19:00 UTC: Создание файла
 * - 2026.02.06 14:50: ДОБАВЛЕНО СОХРАНЕНИЕ APPCONTROLLER
 *   1. Добавлено поле appControllerInstance для сохранения AppController между вызовами
 *   2. Метод initialize() теперь проверяет наличие существующего AppController
 *   3. Метод getAppController() возвращает сохраненный экземпляр
 *   4. Метод cleanup() очищает ссылку на AppController
 *   5. Соответствует жизненному циклу из дополнения к ТЗ
 */
object CoreModule {

    // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Сохраняем AppController между вызовами
    private var appControllerInstance: AppController? = null

    /**
     * Инициализировать все core-компоненты приложения.
     * Должен вызываться один раз при запуске приложения (в onCreate MainActivity).
     *
     * @param context Контекст приложения (ApplicationContext предпочтительнее)
     * @return AppController - главный координатор системы
     */
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
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Возвращаем сохраненный AppController
        return appControllerInstance ?: throw IllegalStateException("CoreModule не инициализирован")
    }

    /**
     * Проверить, инициализировано ли ядро приложения.
     *
     * @return true если ядро инициализировано, false в противном случае
     */
    fun isInitialized(): Boolean {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Проверяем наличие сохраненного AppController
        return appControllerInstance != null
    }

    /**
     * Очистить все core-компоненты.
     * Вызывается при завершении работы приложения или для тестирования.
     */
    fun cleanup() {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
        println("$timestamp [CoreModule] Очистка ядра приложения")

        try {
            // 1. Получаем и очищаем AppController
            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Используем сохраненный экземпляр
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