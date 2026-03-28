// Файл: core/ServiceLocator.kt
package com.alexbar3073.bluetoothcar.core

import android.content.Context
import com.alexbar3073.bluetoothcar.data.bluetooth.BluetoothConnectionManager
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository

/**
 * ФАЙЛ: core/ServiceLocator.kt
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Service Locator (контейнер зависимостей) для централизованного управления
 * зависимостями в приложении. Решает проблему жестких связей между компонентами.
 *
 * ПРЕИМУЩЕСТВА:
 * 1. Устраняет жесткие зависимости между компонентами
 * 2. Упрощает тестирование (легко подменять реализации)
 * 3. Управляет жизненным циклом объектов
 * 4. Предотвращает дублирование кода создания объектов
 *
 * ПРИНЦИП РАБОТЫ:
 * - Все зависимости регистрируются при инициализации приложения
 * - Компоненты запрашивают зависимости через ServiceLocator
 * - ServiceLocator управляет созданием и жизненным циклом объектов
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ ПРОЕКТА:
 * 1. Используется: AppController.kt (главный координатор получает зависимости)
 * 2. Используется: MainActivity.kt (инициализирует ServiceLocator)
 * 3. Регистрирует: SettingsRepository, AppBluetoothService, BluetoothConnectionManager
 * 4. Позволяет: SharedViewModelFactory получать зависимости
 *
 * ИСТОРИЯ ИЗМЕНЕНИЙ:
 * - 2026.02.02 18:30 UTC: Создание файла согласно ТЗ и обсуждению
 * - 2026.02.06 14:45: ДОБАВЛЕНА ИДЕМПОТЕНТНОСТЬ ИНИЦИАЛИЗАЦИИ
 *   1. Добавлено поле isInitialized для предотвращения повторной инициализации
 *   2. Метод initialize() теперь проверяет состояние перед выполнением
 *   3. Метод clear() сбрасывает флаг isInitialized
 *   4. Соответствует жизненному циклу из дополнения к ТЗ
 */
object ServiceLocator {

    private val services = mutableMapOf<String, Any>()
    private val locks = mutableMapOf<String, Any>()
    private var isInitialized = false  // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Флаг инициализации

    /**
     * Инициализировать ServiceLocator с контекстом приложения.
     * Должен вызываться один раз при запуске приложения.
     *
     * @param context Контекст приложения (ApplicationContext)
     */
    fun initialize(context: Context) {
        synchronized(this) {
            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Проверка на повторную инициализацию
            if (isInitialized) {
                logInfo("ServiceLocator уже инициализирован, пропускаем повторную инициализацию")
                return
            }

            // 1. Создаем и регистрируем SettingsRepository (singleton)
            val settingsRepository = SettingsRepository(context.applicationContext)
            register(SettingsRepository::class.java.name, settingsRepository)

            // 2. Создаем и регистрируем AppBluetoothService (singleton)
            //    AppBluetoothService является Android Service, поэтому создается особым образом
            register(AppBluetoothService::class.java.name, AppBluetoothService::class.java)

            // 3. AppController будет создан позже, после загрузки настроек
            // 4. BluetoothConnectionManager будет создан AppController'ом

            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Устанавливаем флаг инициализации
            isInitialized = true

            logInfo("ServiceLocator инициализирован с контекстом приложения")
        }
    }

    /**
     * Зарегистрировать сервис в контейнере.
     *
     * @param key Ключ сервиса (обычно полное имя класса)
     * @param service Сервис для регистрации
     */
    fun <T : Any> register(key: String, service: T) {
        synchronized(getLock(key)) {
            services[key] = service
            logInfo("Зарегистрирован сервис: $key")
        }
    }

    /**
     * Получить сервис из контейнера.
     *
     * @param key Ключ сервиса
     * @return Сервис типа T
     * @throws IllegalStateException если сервис не зарегистрирован
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(key: String): T {
        synchronized(getLock(key)) {
            return services[key] as? T
                ?: throw IllegalStateException("Сервис не зарегистрирован: $key")
        }
    }

    /**
     * Получить или создать сервис (lazy initialization).
     *
     * @param key Ключ сервиса
     * @param factory Фабрика для создания сервиса при первом обращении
     * @return Сервис типа T
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrCreate(key: String, factory: () -> T): T {
        synchronized(getLock(key)) {
            return (services[key] as? T) ?: run {
                val service = factory()
                services[key] = service
                logInfo("Создан и зарегистрирован сервис: $key")
                service
            }
        }
    }

    /**
     * Проверить, зарегистрирован ли сервис.
     *
     * @param key Ключ сервиса
     * @return true если сервис зарегистрирован, false в противном случае
     */
    fun contains(key: String): Boolean {
        synchronized(getLock(key)) {
            return services.containsKey(key)
        }
    }

    /**
     * Удалить сервис из контейнера.
     * Полезно для тестирования или пересоздания зависимостей.
     *
     * @param key Ключ сервиса
     */
    fun remove(key: String) {
        synchronized(getLock(key)) {
            services.remove(key)
            logInfo("Удален сервис: $key")
        }
    }

    /**
     * Очистить все зарегистрированные сервисы.
     * Использовать с осторожностью!
     */
    fun clear() {
        synchronized(this) {
            services.clear()
            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Сбрасываем флаг при очистке
            isInitialized = false
            logInfo("Все сервисы удалены из ServiceLocator")
        }
    }

    // ========== ТИПОБЕЗОПАСНЫЕ МЕТОДЫ ДЛЯ КОНКРЕТНЫХ СЕРВИСОВ ==========

    /**
     * Получить SettingsRepository.
     * Гарантированно возвращает экземпляр, так как регистрируется при инициализации.
     */
    fun getSettingsRepository(): SettingsRepository {
        return resolve(SettingsRepository::class.java.name)
    }

    /**
     * Получить класс AppBluetoothService для создания интентов.
     * AppBluetoothService является Android Service, поэтому мы регистрируем класс,
     * а не экземпляр.
     */
    fun getBluetoothServiceClass(): Class<AppBluetoothService> {
        return resolve(AppBluetoothService::class.java.name)
    }

    /**
     * Зарегистрировать AppController.
     * Вызывается после создания AppController в MainActivity.
     *
     * @param appController Экземпляр AppController
     */
    fun registerAppController(appController: AppController) {
        register(AppController::class.java.name, appController)
    }

    /**
     * Получить AppController.
     *
     * @return Экземпляр AppController
     * @throws IllegalStateException если AppController еще не зарегистрирован
     */
    fun getAppController(): AppController {
        return resolve(AppController::class.java.name)
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private fun getLock(key: String): Any {
        return locks.getOrPut(key) { Any() }
    }

    private fun logInfo(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
        println("$timestamp [ServiceLocator] $message")
    }

    private fun logError(message: String) {
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS").format(java.util.Date())
        System.err.println("$timestamp [ServiceLocator] ERROR: $message")
    }
}
