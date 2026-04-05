// Файл: core/ServiceLocator.kt
package com.alexbar3073.bluetoothcar.core

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.database.AppDatabase
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.repository.SettingsRepository
import java.util.concurrent.atomic.AtomicBoolean

/**
 * ТЕГ: Core/DI/ServiceLocator
 *
 * ФАЙЛ: core/ServiceLocator.kt
 *
 * МЕСТОНАХОЖДЕНИЕ: core/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА И ПРИНЦИП РАБОТЫ:
 * Service Locator (контейнер зависимостей) для централизованного управления
 * зависимостями в приложении. Работает по принципу реестра объектов, где каждый
 * компонент может запросить необходимую ему зависимость по ключу (имени класса).
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Устраняет жесткие зависимости между компонентами системы.
 * 2. Управляет жизненным циклом singleton-объектов и провайдеров.
 * 3. Обеспечивает потокобезопасный доступ к общим ресурсам (репозитории, сервисы, БД).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП: Service Locator / Dependency Injection
 *
 * КЛЮЧЕВОЙ ПРИНЦИП: Централизованное управление жизненным циклом и доступом к зависимостям.
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * - Использует: AppController.kt, AppBluetoothService.kt, SettingsRepository.kt, AppDatabase.kt.
 * - Вызывается из: CoreModule.kt (через initialize).
 * - Взаимодействует: со всеми компонентами системы, предоставляя им зависимости.
 */

object ServiceLocator {

    private const val TAG = "ServiceLocator"

    /** Хранилище экземпляров сервисов, где ключ — имя класса, значение — объект */
    private val services = mutableMapOf<String, Any>()
    
    /** Объекты синхронизации для обеспечения потокобезопасности при доступе к конкретным сервисам */
    private val locks = mutableMapOf<String, Any>()
    
    /** Атомарный флаг, предотвращающий повторную инициализацию локатора */
    private val isInitialized = AtomicBoolean(false)

    /**
     * Инициализировать ServiceLocator с контекстом приложения.
     * Создает базовые зависимости, необходимые для старта приложения.
     *
     * @param context Контекст приложения (ApplicationContext)
     */
    fun initialize(context: Context) {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Быстрая проверка флага инициализации
        if (isInitialized.get()) {
            AppLogger.logInfo("ServiceLocator уже инициализирован, пропускаем повторную инициализацию", TAG)
            return
        }

        synchronized(this) {
            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Double-checked locking для безопасности
            if (isInitialized.get()) return

            val appContext = context.applicationContext

            // 1. Создаем репозиторий настроек и сохраняем его в контейнере
            val settingsRepository = SettingsRepository(appContext)
            register(SettingsRepository::class.java.name, settingsRepository)

            // 2. Инициализируем базу данных Room
            val database = AppDatabase.build(appContext)
            register(AppDatabase::class.java.name, database)

            // 3. Регистрируем класс Bluetooth-сервиса для возможности создания Intent-ов
            register(AppBluetoothService::class.java.name, AppBluetoothService::class.java)

            // 4. Инициализируем провайдер Bluetooth, отвечающий за связь с Service
            val bluetoothProvider = BluetoothServiceProvider(appContext)
            register(BluetoothServiceProvider::class.java.name, bluetoothProvider)

            // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Фиксируем успешное завершение инициализации
            isInitialized.set(true)

            AppLogger.logInfo("ServiceLocator инициализирован с контекстом приложения", TAG)
        }
    }

    /**
     * Асинхронно подключиться к Bluetooth сервису через специализированный провайдер.
     * 
     * @param context Контекст приложения
     * @param onReady Коллбэк, который получит доступ к сервису после подключения
     */
    fun bindBluetoothService(context: Context, onReady: (AppBluetoothService) -> Unit) {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Извлекаем провайдер и делегируем ему процесс биндинга
        val provider = resolve<BluetoothServiceProvider>(BluetoothServiceProvider::class.java.name)
        provider.bind(onReady)
    }

    /**
     * Зарегистрировать новый сервис или объект в контейнере.
     *
     * @param key Уникальный ключ сервиса
     * @param service Экземпляр объекта для регистрации
     */
    fun <T : Any> register(key: String, service: T) {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Блокируем доступ по конкретному ключу для безопасности записи
        synchronized(getLock(key)) {
            // Добавляем объект в карту сервисов
            services[key] = service
            AppLogger.logInfo("Зарегистрирован сервис: $key", TAG)
        }
    }

    /**
     * Получить зарегистрированный сервис из контейнера.
     *
     * @param key Ключ сервиса
     * @return Объект типа T
     * @throws IllegalStateException если сервис не найден в контейнере
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> resolve(key: String): T {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Блокируем чтение для исключения состояния гонки
        synchronized(getLock(key)) {
            // Возвращаем объект с приведением типа или бросаем исключение
            return services[key] as? T
                ?: throw IllegalStateException("Сервис не зарегистрирован: $key")
        }
    }

    /**
     * Получить сервис или создать его, если он еще не зарегистрирован.
     *
     * @param key Ключ сервиса
     * @param factory Лямбда-выражение для создания объекта
     * @return Экземпляр сервиса
     */
    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getOrCreate(key: String, factory: () -> T): T {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Синхронизируем доступ по ключу
        synchronized(getLock(key)) {
            // Проверяем наличие, если нет — создаем через фабрику и регистрируем
            return (services[key] as? T) ?: run {
                val service = factory()
                services[key] = service
                AppLogger.logInfo("Создан и зарегистрирован сервис: $key", TAG)
                service
            }
        }
    }

    /**
     * Проверить наличие сервиса в контейнере.
     *
     * @param key Ключ для поиска
     * @return true если сервис существует
     */
    fun contains(key: String): Boolean {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Безопасно проверяем существование ключа в мапе
        synchronized(getLock(key)) {
            return services.containsKey(key)
        }
    }

    /**
     * Удалить конкретный сервис из контейнера.
     *
     * @param key Ключ удаляемого сервиса
     */
    fun remove(key: String) {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Синхронизируем удаление по ключу
        synchronized(getLock(key)) {
            services.remove(key)
            AppLogger.logInfo("Удален сервис: $key", TAG)
        }
    }

    /**
     * Полная очистка ServiceLocator и деинициализация всех провайдеров.
     */
    fun clear() {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Блокируем весь объект на время глобальной очистки
        synchronized(this) {
            AppLogger.logInfo("Начало очистки ServiceLocator", TAG)
            
            // 1. Проверяем наличие Bluetooth-провайдера и вызываем его очистку (unbind)
            if (contains(BluetoothServiceProvider::class.java.name)) {
                val provider = resolve<BluetoothServiceProvider>(BluetoothServiceProvider::class.java.name)
                provider.cleanup()
            }
            
            // 2. Очищаем все коллекции ссылок и объектов блокировки
            services.clear()
            locks.clear()
            
            // 3. Сбрасываем флаг инициализации для возможности повторного запуска
            isInitialized.set(false)
            AppLogger.logInfo("Все сервисы удалены из ServiceLocator", TAG)
        }
    }

    // ========== ТИПОБЕЗОПАСНЫЕ МЕТОДЫ ДОСТУПА ==========

    /**
     * Упрощенный доступ к SettingsRepository.
     */
    fun getSettingsRepository(): SettingsRepository {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Извлекаем репозиторий из контейнера по его имени
        return resolve(SettingsRepository::class.java.name)
    }

    /**
     * Упрощенный доступ к базе данных приложения.
     */
    fun getDatabase(): AppDatabase {
        return resolve(AppDatabase::class.java.name)
    }

    /**
     * Получить ссылку на класс Bluetooth-сервиса.
     */
    fun getBluetoothServiceClass(): Class<AppBluetoothService> {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Извлекаем ранее зарегистрированный объект Class
        return resolve(AppBluetoothService::class.java.name)
    }

    /**
     * Зарегистрировать главный координатор приложения.
     */
    fun registerAppController(appController: AppController) {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Сохраняем экземпляр контроллера для общего доступа
        register(AppController::class.java.name, appController)
    }

    /**
     * Получить экземпляр AppController.
     */
    fun getAppController(): AppController {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Извлекаем контроллер из общего хранилища
        return resolve(AppController::class.java.name)
    }

    // ========== СЛУЖЕБНЫЕ МЕТОДЫ ==========

    /**
     * Создать или получить объект блокировки для конкретного ключа.
     */
    private fun getLock(key: String): Any {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Атомарно получаем существующий или создаем новый объект lock
        return locks.getOrPut(key) { Any() }
    }
}

/**
 * Провайдер, управляющий асинхронным жизненным циклом AppBluetoothService.
 * Отвечает за безопасное подключение (bind) и отключение (unbind) от Android Service.
 */
class BluetoothServiceProvider(private val context: Context) {

    private val providerTag = "BluetoothServiceProvider"

    /** Ссылка на активный экземпляр сервиса (доступна только при наличии соединения) */
    @Volatile
    private var boundService: AppBluetoothService? = null
    
    /** Объект для управления соединением с системным сервисом */
    private var serviceConnection: ServiceConnection? = null

    /**
     * Инициировать процесс подключения к Bluetooth-сервису.
     * 
     * @param onReady Коллбэк, вызываемый по завершении процесса биндинга
     */
    fun bind(onReady: (AppBluetoothService) -> Unit) {
        // 1. Проверяем, не забиндин ли сервис уже сейчас
        boundService?.let {
            onReady(it)
            return
        }

        // 2. Создаем анонимную реализацию ServiceConnection
        serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                // Извлекаем сервис из переданного Binder-а
                val binder = service as AppBluetoothService.BluetoothBinder
                val bluetoothService = binder.getService()
                
                // Сохраняем ссылку для последующего использования
                boundService = bluetoothService
                
                AppLogger.logInfo("AppBluetoothService успешно забиндин", providerTag)
                onReady(bluetoothService)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                // Обнуляем ссылку при непредвиденном разрыве соединения
                boundService = null
                AppLogger.logWarning("AppBluetoothService отсоединился", providerTag)
            }
        }

        // 3. Гарантируем, что сервис запущен в системе, прежде чем биндиться к нему
        AppBluetoothService.start(context)
        
        // 4. Выполняем системный вызов bindService
        val intent = Intent(context, AppBluetoothService::class.java)
        context.bindService(intent, serviceConnection!!, Context.BIND_AUTO_CREATE)
    }

    /**
     * Корректно разорвать связь с сервисом для предотвращения утечек памяти.
     */
    fun cleanup() {
        // СОГЛАСНО ЖИЗНЕННОМУ ЦИКЛУ: Проверяем наличие активного соединения перед unbind
        serviceConnection?.let {
            try {
                // Выполняем системную процедуру отвязки от сервиса
                context.unbindService(it)
                AppLogger.logInfo("unbindService успешно выполнен", providerTag)
            } catch (e: Exception) {
                // Логируем исключение, если сервис уже был удален или не был привязан
                AppLogger.logError("Ошибка при попытке unbind: ${e.message}", providerTag)
            }
        }
        // Обнуляем все ресурсы
        serviceConnection = null
        boundService = null
    }
}
