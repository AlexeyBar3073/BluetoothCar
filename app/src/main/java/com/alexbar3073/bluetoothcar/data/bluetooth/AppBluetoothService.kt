// ФАЙЛ: data/bluetooth/AppBluetoothService.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.alexbar3073.bluetoothcar.data.logging.AppLogger
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import java.io.Closeable
import java.io.IOException
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow

/**
 * ТЕГ: Низкоуровневый Bluetooth сервис (Android Foreground Service)
 * ФАЙЛ: C:/Project/BluetoothCar/app/src/main/java/com/alexbar3073/bluetoothcar/data/bluetooth/AppBluetoothService.kt
 * МЕСТОНАХОЖДЕНИЕ: data/bluetooth/
 *
 * НАЗНАЧЕНИЕ ФАЙЛА:
 * Низкоуровневая обертка над системным Android Bluetooth API, реализованная как Android Service.
 * Предоставляет унифицированный интерфейс для работы с адаптером, поиска устройств, 
 * управления сокетами и потоками данных.
 * 
 * КЛЮЧЕВАЯ ФУНКЦИЯ: Обеспечивает работу Bluetooth-соединения в фоновом режиме через 
 * механизмы Foreground Service. Осуществляет конвертирование объектов системного уровня 
 * Android (BluetoothDevice) в доменные объекты приложения (BluetoothDeviceData).
 *
 * ОТВЕТСТВЕННОСТЬ:
 * 1. Инициализация и проверка состояния Bluetooth адаптера.
 * 2. Управление системным поиском устройств (Discovery).
 * 3. Установление и разрыв RFCOMM соединений.
 * 4. Мониторинг системных событий (состояние адаптера, подключение/отключение ACL).
 * 5. Чтение и запись данных в Bluetooth сокет.
 * 6. Преобразование системных моделей в доменные модели приложения.
 * 7. Управление уведомлением для работы в переднем плане (Foreground).
 *
 * АРХИТЕКТУРНЫЙ ПРИНЦИП:
 * - Скрытие сложности Android API за простым интерфейсом.
 * - Реактивность через Kotlin Flow для потоков данных.
 * - Использование атомарных флагов для предотвращения состояний гонки.
 * - Единственный источник истины для состояния подключения (проверка сокета).
 * - Жизненный цикл управляется системой Android (Service).
 *
 * СВЯЗИ С ДРУГИМИ ФАЙЛАМИ:
 * 1. Используется: BluetoothConnectionManager.kt и его помощниками (CFC, DAM, CSM, DSH).
 * 2. Использует: AppLogger.kt для диагностического вывода.
 * 3. Использует: BluetoothDeviceData как модель данных устройства.
 */
class AppBluetoothService : Service() {

    /** 
     * Binder для предоставления интерфейса сервиса клиентам. 
     */
    private val binder = BluetoothBinder()

    /** 
     * Scope для выполнения корутин в контексте жизненного цикла сервиса. 
     */
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Класс для связи сервиса с клиентами (например, MainActivity или BCM).
     */
    inner class BluetoothBinder : Binder() {
        /**
         * Получить экземпляр сервиса.
         */
        fun getService(): AppBluetoothService = this@AppBluetoothService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    companion object {
        /** Тег для логирования действий сервиса */
        private const val TAG = "AppBluetoothService"
        
        /** Стандартный UUID для последовательного порта (SPP) */
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

        /** ID канала уведомлений */
        private const val CHANNEL_ID = "bluetooth_service_channel"
        
        /** ID уведомления сервиса */
        private const val NOTIFICATION_ID = 1

        /**
         * Глобальный экземпляр сервиса (Singleton-подобный доступ).
         * Заполняется в onCreate и очищается в onDestroy.
         */
        @Volatile
        private var instance: AppBluetoothService? = null

        /**
         * Получить текущий экземпляр сервиса.
         * ПРЕДУПРЕЖДЕНИЕ: Может вернуть null, если сервис еще не создан системой.
         */
        fun getInstance(): AppBluetoothService? = instance

        /**
         * Запустить сервис.
         * 
         * @param context Контекст для запуска интента.
         */
        fun start(context: Context) {
            val intent = Intent(context, AppBluetoothService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Остановить сервис.
         * 
         * @param context Контекст для остановки интента.
         */
        fun stop(context: Context) {
            val intent = Intent(context, AppBluetoothService::class.java)
            context.stopService(intent)
        }
    }

    // ========== СОСТОЯНИЯ И КОНТЕКСТ ==========

    /** 
     * Атомарный флаг активности мониторинга соединения. 
     * Предотвращает множественную регистрацию BroadcastReceiver-ов.
     */
    private val isMonitoringConnection = AtomicBoolean(false)

    // ========== BLUETOOTH КОМПОНЕНТЫ ==========

    /** Системный Bluetooth адаптер */
    private var bluetoothAdapter: BluetoothAdapter? = null

    /** Контекст приложения (сохраняем для совместимости, хотя сервис сам является контекстом) */
    private var internalContext: Context? = null

    // ========== ПОДКЛЮЧЕНИЕ ==========

    /** MAC-адрес текущего подключенного устройства */
    private var currentConnectionAddress: String? = null

    /** Текущий Bluetooth сокет */
    private var bluetoothSocket: BluetoothSocket? = null

    /** 
     * Публичное свойство состояния подключения. 
     * Проверяет реальное состояние сокета через системный API.
     */
    val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true

    // ========== МОНИТОРИНГ СОЕДИНЕНИЯ ==========

    /** Callback для уведомления об изменении состояния соединения */
    private var connectionStateCallback: ((Boolean, BluetoothDeviceData?) -> Unit)? = null

    /** BroadcastReceiver для системных событий ACL_CONNECTED / ACL_DISCONNECTED */
    private var connectionBroadcastReceiver: BroadcastReceiver? = null

    // ========== МОНИТОРИНГ СОСТОЯНИЯ BLUETOOTH ==========

    /** Потокобезопасная коллекция callback-функций для мониторинга состояния адаптера */
    private val bluetoothStateCallbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()

    /** BroadcastReceiver для мониторинга включения/выключения Bluetooth адаптера */
    private var bluetoothStateReceiver: BroadcastReceiver? = null

    // ========== ПОИСК УСТРОЙСТВ ==========

    /** Callback для уведомления о найденном устройстве */
    private var deviceFoundCallback: ((String) -> Unit)? = null

    /** Callback для уведомления о завершении поиска */
    private var discoveryFinishedCallback: (() -> Unit)? = null

    /** Callback для уведомления об ошибке поиска */
    private var discoveryErrorCallback: ((String) -> Unit)? = null

    /** BroadcastReceiver для событий системного поиска (Discovery) */
    private var discoveryReceiver: BroadcastReceiver? = null

    // ========== ПОТОКИ ДАННЫХ ==========

    /** Scope для асинхронного чтения данных из сокета */
    private var dataScope: CoroutineScope? = null

    /** Job процесса прослушивания данных */
    private var dataListeningJob: Job? = null

    /** Атомарный флаг активности прослушивания данных */
    private val isListeningForData = AtomicBoolean(false)

    // ========== ЖИЗНЕННЫЙ ЦИКЛ СЕРВИСА ==========

    override fun onCreate() {
        super.onCreate()
        instance = this
        internalContext = this
        AppLogger.logInfo("Создание AppBluetoothService", TAG)
        createNotificationChannel()
        
        // При создании сразу переходим в "тихий" Foreground режим, чтобы сервис не убили
        startInForeground("Ожидание подключения...")
        
        initializeBluetoothAdapter()
        startBluetoothStateMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        AppLogger.logInfo("Запуск AppBluetoothService (onStartCommand)", TAG)
        // START_STICKY позволяет системе перезапустить сервис, если он будет убит
        return START_STICKY
    }

    override fun onDestroy() {
        AppLogger.logInfo("Уничтожение AppBluetoothService", TAG)
        cleanupSocket()
        unregisterBluetoothStateReceiver()
        unregisterConnectionBroadcastReceiver()
        unregisterDiscoveryReceiver()
        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }

    /**
     * Установить контекст для работы с сервисом.
     * СОХРАНЕНО ДЛЯ СОВМЕСТИМОСТИ. В режиме Service использует собственный контекст.
     * 
     * @param context Контекст приложения.
     */
    fun setContext(context: Context) {
        AppLogger.logInfo("Метод setContext вызван в режиме Service (установка внутреннего контекста)", TAG)
        this.internalContext = context
    }

    // ========== УПРАВЛЕНИЕ FOREGROUND РЕЖИМОМ ==========

    /**
     * Создать канал уведомлений для Foreground Service.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Bluetooth Car Connection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Обеспечивает стабильную связь с Bluetooth-устройством в фоне"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }

    /**
     * Создать уведомление для Foreground Service.
     * 
     * @param content Текст уведомления.
     */
    private fun createNotification(content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Связь с Bluetooth-каром")
            .setContentText(content)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setOngoing(true)
            .build()
    }

    /**
     * Запустить/обновить сервис в режиме Foreground.
     * 
     * @param message Сообщение для уведомления.
     */
    private fun startInForeground(message: String) {
        val notification = createNotification(message)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            AppLogger.logInfo("Сервис переведен в Foreground режим: $message", TAG)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка запуска Foreground режима: ${e.message}", TAG)
        }
    }

    /**
     * Остановить режим Foreground (не рекомендуется для активного соединения).
     */
    fun stopForegroundMode() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        AppLogger.logInfo("Foreground режим остановлен", TAG)
    }

    /**
     * Инициализировать Bluetooth адаптер.
     * Вызывается из: onCreate().
     */
    private fun initializeBluetoothAdapter() {
        try {
            // Получаем адаптер через стандартный метод (поддержка API 24+)
            @Suppress("DEPRECATION")
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                AppLogger.logError("Bluetooth не поддерживается на данном устройстве", TAG)
            } else {
                AppLogger.logInfo("Bluetooth адаптер успешно инициализирован", TAG)
            }
        } catch (e: Exception) {
            AppLogger.logError("Ошибка при инициализации Bluetooth адаптера: ${e.message}", TAG)
        }
    }

    // ========== ПУБЛИЧНЫЙ API ==========

    /**
     * Проверить физическое наличие Bluetooth модуля.
     * Вызывается из: ConnectionFeasibilityChecker.kt.
     * 
     * @return true если адаптер доступен.
     */
    fun bluetoothAdapterIsAvailable(): Boolean = bluetoothAdapter != null

    /**
     * Проверить, включен ли Bluetooth адаптер в данный момент.
     * Вызывается из: BCM и вспомогательных классов.
     * 
     * @return true если Bluetooth включен.
     */
    fun bluetoothAdapterIsEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    /**
     * Получить список всех сопряженных устройств.
     * Вызывается из: BluetoothConnectionManager.kt и AppController.kt.
     * 
     * @return Список доменных моделей BluetoothDeviceData.
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceData>? {
        return try {
            val adapter = bluetoothAdapter ?: return null
            if (!adapter.isEnabled) return null

            // Проверяем наличие разрешений перед доступом к сопряженным устройствам
            if (!hasBluetoothConnectPermission()) {
                AppLogger.logError("Отсутствует разрешение BLUETOOTH_CONNECT для получения устройств", TAG)
                return emptyList()
            }

            // Обертка в try-catch для обработки SecurityException на Android 12+
            val pairedDevices = adapter.bondedDevices
            pairedDevices?.map { androidDeviceToDomain(it) } ?: emptyList()
        } catch (e: SecurityException) {
            AppLogger.logError("Ошибка безопасности при получении устройств: ${e.message}", TAG)
            emptyList()
        } catch (e: Exception) {
            AppLogger.logError("Ошибка при получении сопряженных устройств: ${e.message}", TAG)
            emptyList()
        }
    }

    /**
     * Проверить, сопряжено ли конкретное устройство по его адресу.
     * Вызывается из: ConnectionFeasibilityChecker.kt.
     * 
     * @param address MAC-адрес устройства.
     * @return true если устройство сопряжено.
     */
    fun isDevicePaired(address: String): Boolean {
        // Проверяем разрешения перед поиском в списке сопряженных
        if (!hasBluetoothConnectPermission()) return false
        return getAndroidDeviceByAddress(address) != null
    }

    // ========== МЕТОДЫ ДЛЯ ПОИСКА УСТРОЙСТВ ==========

    /**
     * Начать системный поиск устройств (Discovery).
     * Вызывается из: DeviceAvailabilityMonitor.kt.
     * 
     * @param onDeviceFound Callback при нахождении устройства.
     * @param onDiscoveryFinished Callback при завершении поиска.
     * @param onDiscoveryError Callback при возникновении ошибки.
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery(
        onDeviceFound: (String) -> Unit,
        onDiscoveryFinished: () -> Unit,
        onDiscoveryError: (String) -> Unit
    ) {
        // Инициализируем колбеки
        this.deviceFoundCallback = onDeviceFound
        this.discoveryFinishedCallback = onDiscoveryFinished
        this.discoveryErrorCallback = onDiscoveryError

        // Проверяем комплекс разрешений (Location для API < 31, Scan для API 31+)
        if (!hasDiscoveryPermissions()) {
            onDiscoveryError("Отсутствуют разрешения для поиска устройств")
            return
        }

        val adapter = bluetoothAdapter ?: run {
            onDiscoveryError("Bluetooth адаптер недоступен")
            return
        }

        if (!adapter.isEnabled) {
            onDiscoveryError("Bluetooth выключен")
            return
        }

        // Регистрируем Receiver для получения результатов поиска из системы
        registerDiscoveryReceiver()

        // Запускаем системный процесс Discovery
        val started = try {
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            onDiscoveryError("Ошибка безопасности при запуске поиска: ${e.message}")
            false
        } catch (e: Exception) {
            onDiscoveryError("Сбой при запуске поиска: ${e.message}")
            false
        }

        if (!started) {
            cleanupDiscoveryCallbacks()
            unregisterDiscoveryReceiver()
        }
    }

    /**
     * Остановить системный поиск устройств.
     * Вызывается из: DeviceAvailabilityMonitor.kt.
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        try {
            // Отменяем поиск в системе для высвобождения ресурсов адаптера
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            AppLogger.logError("Ошибка безопасности при остановке поиска: ${e.message}", TAG)
        }
        
        // Очищаем ресурсы поиска и отменяем регистрацию Receiver
        cleanupDiscoveryCallbacks()
        unregisterDiscoveryReceiver()
    }

    /**
     * Зарегистрировать BroadcastReceiver для получения результатов поиска.
     */
    private fun registerDiscoveryReceiver() {
        if (discoveryReceiver != null) return

        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    // Обработка найденного устройства в эфире
                    AndroidBluetoothDevice.ACTION_FOUND -> {
                        @Suppress("DEPRECATION")
                        val device: AndroidBluetoothDevice? = intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                        device?.let {
                            val address = it.address
                            if (!address.isNullOrBlank()) {
                                deviceFoundCallback?.invoke(address)
                            }
                        }
                    }
                    // Обработка завершения процесса поиска системой
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        discoveryFinishedCallback?.invoke()
                        cleanupDiscoveryCallbacks()
                        unregisterDiscoveryReceiver()
                    }
                }
            }
        }

        // Настраиваем фильтр для получения событий поиска
        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        registerReceiver(discoveryReceiver, filter)
    }

    /**
     * Отменить регистрацию Receiver-а поиска.
     */
    private fun unregisterDiscoveryReceiver() {
        discoveryReceiver?.let { unregisterReceiver(it) }
        discoveryReceiver = null
    }

    /**
     * Очистить временные колбеки поиска.
     */
    private fun cleanupDiscoveryCallbacks() {
        deviceFoundCallback = null
        discoveryFinishedCallback = null
        discoveryErrorCallback = null
    }

    // ========== МЕТОДЫ ПОДКЛЮЧЕНИЯ ==========

    /**
     * Установить RFCOMM соединение с устройством.
     * Вызывается из: ConnectionStateManager.kt.
     * 
     * @param deviceData Данные устройства для подключения.
     * @param onConnected Callback при успешном подключении.
     * @param onDisconnected Callback при разрыве или ошибке.
     * @param timeoutMs Время ожидания подключения.
     * @return Пара: успех операции и сообщение об ошибке (если есть).
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(
        deviceData: BluetoothDeviceData,
        onConnected: (BluetoothDeviceData) -> Unit,
        onDisconnected: (BluetoothDeviceData) -> Unit,
        timeoutMs: Long = 10000L
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            // Если соединение уже активно и адрес совпадает — возвращаем успех
            if (isConnected && currentConnectionAddress == deviceData.address) {
                return@withContext Pair(true, null)
            }

            // Получаем системный объект устройства по его MAC-адресу
            val androidDevice = getAndroidDeviceByAddress(deviceData.address)
                ?: return@withContext Pair(false, "Устройство не найдено в списке сопряженных")

            withTimeout(timeoutMs) {
                // Создаем RFCOMM сокет по стандартному UUID для SPP
                @Suppress("DEPRECATION")
                val socket = androidDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                
                // Перед подключением обязательно останавливаем поиск для стабильности
                try { bluetoothAdapter?.cancelDiscovery() } catch (e: SecurityException) {
                    AppLogger.logError("SecurityException при отмене поиска: ${e.message}", TAG)
                }
                
                // Выполняем блокирующее подключение в IO потоке корутины
                socket.connect()
                
                // Сохраняем активный сокет и адрес
                bluetoothSocket = socket
                currentConnectionAddress = deviceData.address
                
                // При успешном подключении обновляем Foreground уведомление
                startInForeground("Подключено к ${deviceData.name}")
                
                onConnected(deviceData)
                Pair(true, null)
            }
        } catch (e: Exception) {
            // При любой ошибке закрываем сокет и уведомляем вызывающую сторону
            cleanupSocket()
            startInForeground("Ошибка подключения к ${deviceData.name}")
            onDisconnected(deviceData)
            Pair(false, e.message ?: "Сбой при установке Bluetooth соединения")
        }
    }

    /**
     * Принудительно разорвать текущее соединение.
     * Вызывается из: BluetoothConnectionManager.kt и CSM.
     */
    fun disconnect() {
        cleanupSocket()
        startInForeground("Соединение разорвано")
    }

    /**
     * Очистить текущий сокет и сбросить переменные состояния.
     */
    private fun cleanupSocket() {
        try {
            // Закрываем сокет и освобождаем системные ресурсы
            bluetoothSocket?.close()
        } catch (e: IOException) {
            AppLogger.logError("Ошибка при закрытии Bluetooth сокета: ${e.message}", TAG)
        }
        bluetoothSocket = null
        currentConnectionAddress = null
        // Останавливаем асинхронное прослушивание данных
        stopDataListening()
    }

    // ========== МОНИТОРИНГ СОСТОЯНИЯ BLUETOOTH АДАПТЕРА ==========

    /**
     * Запустить мониторинг изменения состояния Bluetooth адаптера (вкл/выкл).
     */
    private fun startBluetoothStateMonitoring() {
        if (bluetoothStateReceiver != null) return

        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isEnabled = state == BluetoothAdapter.STATE_ON
                    
                    // Уведомляем всех подписчиков (например, CFC) об изменении состояния
                    bluetoothStateCallbacks.values.forEach { it(isEnabled) }
                }
            }
        }
        registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    /**
     * Подписаться на уведомления об изменении состояния адаптера.
     * Вызывается из: ConnectionFeasibilityChecker.kt.
     * 
     * @param callback Функция обратного вызова.
     * @return Closeable для отмены подписки при уничтожении помощника.
     */
    fun monitorBluetoothState(callback: (Boolean) -> Unit): Closeable {
        val id = UUID.randomUUID().toString()
        bluetoothStateCallbacks[id] = callback
        return Closeable { bluetoothStateCallbacks.remove(id) }
    }

    private fun unregisterBluetoothStateReceiver() {
        bluetoothStateReceiver?.let { unregisterReceiver(it) }
        bluetoothStateReceiver = null
    }

    // ========== МОНИТОРИНГ СОСТОЯНИЯ СОЕДИНЕНИЯ ==========

    /**
     * Начать мониторинг физического статуса подключения (ACL события системы).
     * Вызывается из: ConnectionStateManager.kt после успешного подключения.
     * 
     * @param deviceData Данные устройства для мониторинга.
     * @param callback Callback, уведомляющий о подключении/разрыве.
     */
    fun startConnectionMonitoring(deviceData: BluetoothDeviceData, callback: (Boolean, BluetoothDeviceData?) -> Unit) {
        // Предотвращаем повторную регистрацию Receiver-а
        if (isMonitoringConnection.getAndSet(true)) return
        this.connectionStateCallback = callback
        registerConnectionBroadcastReceiver(deviceData.address)
    }

    /**
     * Зарегистрировать Receiver для отслеживания системных ACL событий.
     * 
     * @param targetAddress MAC-адрес устройства для фильтрации событий.
     */
    private fun registerConnectionBroadcastReceiver(targetAddress: String) {
        connectionBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                @Suppress("DEPRECATION")
                val device: AndroidBluetoothDevice? = intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                // Игнорируем события от других устройств
                if (device?.address != targetAddress) return

                when (intent.action) {
                    // Системное событие успешной установки ACL соединения
                    AndroidBluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val deviceData = androidDeviceToDomain(device)
                        startInForeground("Подключено к ${deviceData.name}")
                        connectionStateCallback?.invoke(true, deviceData)
                    }
                    // Системное событие разрыва ACL соединения
                    AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        cleanupSocket()
                        val deviceData = androidDeviceToDomain(device)
                        startInForeground("Отключено от ${deviceData.name}")
                        connectionStateCallback?.invoke(false, deviceData)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        registerReceiver(connectionBroadcastReceiver, filter)
    }

    /**
     * Отменить регистрацию Receiver-а физического статуса подключения.
     */
    private fun unregisterConnectionBroadcastReceiver() {
        connectionBroadcastReceiver?.let { unregisterReceiver(it) }
        connectionBroadcastReceiver = null
        connectionStateCallback = null
    }

    /**
     * Остановить мониторинг физического статуса подключения.
     * Вызывается из: ConnectionStateManager.kt при остановке.
     */
    fun stopConnectionMonitoring() {
        if (!isMonitoringConnection.getAndSet(false)) return
        unregisterConnectionBroadcastReceiver()
    }

    // ========== ПЕРЕДАЧА ДАННЫХ ==========

    /**
     * Отправить строку данных на устройство через открытый Bluetooth сокет.
     * Вызывается из: DataStreamHandler.kt.
     * 
     * @param data Строка (обычно в формате JSON) для отправки.
     * @return true если данные успешно записаны в выходной поток.
     */
    suspend fun sendData(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = bluetoothSocket ?: return@withContext false
            if (!socket.isConnected) return@withContext false
            
            // Записываем байты строки в OutputStream сокета
            socket.outputStream.write(data.toByteArray(Charsets.UTF_8))
            socket.outputStream.flush()
            true
        } catch (e: Exception) {
            // Любой сбой при записи данных трактуется как потеря связи
            cleanupSocket()
            startInForeground("Ошибка передачи данных")
            false
        }
    }

    /**
     * Запустить асинхронное прослушивание входящих данных из сокета.
     * Вызывается из: DataStreamHandler.kt.
     * 
     * @return Flow с полученными строками данных для реактивной обработки.
     */
    fun startDataListening(): Flow<String> = channelFlow {
        // Защита от запуска нескольких корутин прослушивания на одном сокете
        if (isListeningForData.getAndSet(true)) return@channelFlow
        
        dataScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataListeningJob = dataScope?.launch {
            try {
                val socket = bluetoothSocket ?: return@launch
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                
                // Цикл чтения данных пока флаг активен и сокет сообщает о подключении
                while (isListeningForData.get() && socket.isConnected) {
                    val read = inputStream.read(buffer)
                    if (read > 0) {
                        // Эмитируем полученную строку в холодный поток Flow
                        send(String(buffer, 0, read, Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка при чтении данных из Bluetooth: ${e.message}", TAG)
            } finally {
                isListeningForData.set(false)
            }
        }

        // Очистка ресурсов корутины при закрытии или отмене Flow
        awaitClose { 
            isListeningForData.set(false) 
            dataListeningJob?.cancel()
        }
    }

    /**
     * Остановить процесс асинхронного прослушивания входящих данных.
     * Вызывается из: DataStreamHandler.kt.
     */
    fun stopDataListening() {
        isListeningForData.set(false)
        dataListeningJob?.cancel()
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Найти системный объект устройства по его MAC-адресу в списке сопряженных.
     * 
     * @param address MAC-адрес целевого устройства.
     * @return Android BluetoothDevice или null, если не сопряжено.
     */
    @SuppressLint("MissingPermission")
    private fun getAndroidDeviceByAddress(address: String): AndroidBluetoothDevice? {
        return try {
            // Ищем устройство в списке сопряженных в системном адаптере
            bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == address }
        } catch (e: SecurityException) {
            AppLogger.logError("Ошибка безопасности в getAndroidDeviceByAddress: ${e.message}", TAG)
            null
        }
    }

    /**
     * Преобразовать системный объект BluetoothDevice в доменную модель BluetoothDeviceData.
     * 
     * @param device Системное Android Bluetooth устройство.
     * @return Доменная модель с заполненными данными (имя, адрес, класс).
     */
    private fun androidDeviceToDomain(device: AndroidBluetoothDevice): BluetoothDeviceData {
        return try {
            // Используем фабричный метод модели для корректного маппинга полей и классов
            BluetoothDeviceData.fromAndroidDevice(device)
        } catch (e: SecurityException) {
            BluetoothDeviceData(name = "Нет разрешений", address = device.address)
        }
    }

    /**
     * Проверить наличие разрешения BLUETOOTH_CONNECT (актуально для Android 12+).
     * 
     * @return true если разрешение предоставлено пользователем.
     */
    private fun hasBluetoothConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    /**
     * Проверить комплекс разрешений для поиска устройств (Discovery).
     * Учитывает различия в требованиях API между Android 7 и Android 12.
     * 
     * @return true если все необходимые разрешения предоставлены.
     */
    private fun hasDiscoveryPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScan = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val hasConnect = ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            hasScan && hasConnect
        } else {
            // Для Android 7-11 требуется разрешение на доступ к местоположению
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }
}
