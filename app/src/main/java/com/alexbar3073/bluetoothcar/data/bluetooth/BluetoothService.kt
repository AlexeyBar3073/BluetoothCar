// Файл: data/bluetooth/BluetoothService.kt
package com.alexbar3073.bluetoothcar.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice as AndroidBluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
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

class BluetoothService {
    companion object {
        private const val TAG = "BluetoothService"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // ========== СОСТОЯНИЯ И КОНТЕКСТ ==========

    /** Текущее состояние сервиса */
    private var currentState = BluetoothServiceState.IDLE

    /** Атомарный флаг активности мониторинга соединения */
    private val isMonitoringConnection = AtomicBoolean(false)

    // ========== BLUETOOTH КОМПОНЕНТЫ ==========

    /** Android Bluetooth адаптер */
    private var bluetoothAdapter: BluetoothAdapter? = null

    /** Контекст приложения для работы с разрешениями и BroadcastReceiver */
    private var context: Context? = null

    // ========== ПОДКЛЮЧЕНИЕ ==========

    /** Адрес текущего подключенного устройства */
    private var currentConnectionAddress: String? = null

    /** Bluetooth сокет для текущего подключения */
    private var bluetoothSocket: BluetoothSocket? = null

    // ========== МОНИТОРИНГ СОЕДИНЕНИЯ ==========

    /** Callback для уведомления об изменении состояния соединения */
    private var connectionStateCallback: ((Boolean, BluetoothDeviceData?) -> Unit)? = null

    /** BroadcastReceiver для мониторинга событий подключения/отключения */
    private var connectionBroadcastReceiver: BroadcastReceiver? = null

    // ========== МОНИТОРИНГ СОСТОЯНИЯ BLUETOOTH ==========

    /** Потокобезопасная коллекция callback-функций для мониторинга состояния Bluetooth */
    private val bluetoothStateCallbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()

    /** BroadcastReceiver для мониторинга включения/выключения Bluetooth */
    private var bluetoothStateReceiver: BroadcastReceiver? = null

    // ========== ПОИСК УСТРОЙСТВ ==========

    /** Callback для уведомления о найденном устройстве при поиске */
    private var deviceFoundCallback: ((String) -> Unit)? = null

    /** Callback для уведомления о завершении поиска устройств */
    private var discoveryFinishedCallback: (() -> Unit)? = null

    /** Callback для уведомления об ошибке поиска устройств */
    private var discoveryErrorCallback: ((String) -> Unit)? = null

    /** BroadcastReceiver для мониторинга событий поиска устройств */
    private var discoveryReceiver: BroadcastReceiver? = null

    // ========== ПОТОКИ ДАННЫХ ==========

    /** Корутин скоуп для прослушивания данных */
    private var dataScope: CoroutineScope? = null

    /** Job для прослушивания данных */
    private var dataListeningJob: Job? = null

    /** Атомарный флаг активности прослушивания данных */
    private var isListeningForData = AtomicBoolean(false)

    /**
     * Установить контекст для работы с разрешениями и BroadcastReceiver.
     */
    fun setContext(context: Context) {
        this.context = context
        initializeBluetoothAdapter(context)
        startBluetoothStateMonitoring() // Запускаем мониторинг при установке контекста
    }

    /**
     * Инициализировать Bluetooth адаптер.
     */
    private fun initializeBluetoothAdapter(context: Context) {
        try {
            @Suppress("DEPRECATION")
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

            if (bluetoothAdapter == null) {
                AppLogger.logError("Bluetooth не поддерживается", TAG)
            } else {
                AppLogger.logInfo("Bluetooth адаптер инициализирован", TAG)
            }
        } catch (e: Exception) {
            AppLogger.logError("Ошибка инициализации: ${e.message}", TAG)
        }
    }

    // ========== ВНУТРЕННИЕ МЕТОДЫ ДЛЯ РАБОТЫ С AndroidBluetoothDevice ==========

    /**
     * Получить AndroidBluetoothDevice по адресу.
     */
    private fun getAndroidDeviceByAddress(address: String): AndroidBluetoothDevice? {
        return try {
            if (!hasBluetoothConnectPermission()) {
                AppLogger.logError("Нет разрешения BLUETOOTH_CONNECT для получения устройства", TAG)
                return null
            }

            val adapter = bluetoothAdapter ?: return null
            if (!adapter.isEnabled) return null

            val pairedDevices = adapter.bondedDevices
            pairedDevices?.firstOrNull { it.address == address }
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при получении устройства: ${e.message}", TAG)
            null
        } catch (e: Exception) {
            AppLogger.logError("Ошибка поиска Android устройства: ${e.message}", TAG)
            null
        }
    }

    /**
     * Преобразовать AndroidBluetoothDevice в BluetoothDeviceData.
     */
    private fun androidDeviceToDomain(androidDevice: AndroidBluetoothDevice): BluetoothDeviceData {
        return try {
            val deviceName = if (hasBluetoothConnectPermission()) {
                androidDevice.name ?: "Неизвестное устройство"
            } else {
                "Неизвестное устройство"
            }

            BluetoothDeviceData(
                address = androidDevice.address,
                name = deviceName
            )
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при преобразовании устройства: ${e.message}", TAG)
            BluetoothDeviceData(
                address = androidDevice.address,
                name = "Неизвестное устройство"
            )
        }
    }

    // ========== ПУБЛИЧНЫЙ API ==========

    /**
     * Проверить наличие Bluetooth адаптера на устройстве.
     */
    fun bluetoothAdapterIsAvailable(): Boolean {
        return try {
            bluetoothAdapter != null
        } catch (e: Exception) {
            AppLogger.logError("Ошибка проверки наличия адаптера Bluetooth: ${e.message}", TAG)
            false
        }
    }

    /**
     * Проверить, включен ли Bluetooth адаптер.
     */
    fun bluetoothAdapterIsEnabled(): Boolean {
        return try {
            bluetoothAdapter?.isEnabled ?: false
        } catch (e: Exception) {
            AppLogger.logError("Ошибка проверки состояния Bluetooth: ${e.message}", TAG)
            false
        }
    }

    /**
     * Получить все сопряженные устройства как BluetoothDeviceData.
     */
    fun getPairedDevices(): List<BluetoothDeviceData>? {
        return try {
            val adapter = bluetoothAdapter ?: return null
            if (!adapter.isEnabled) return null

            if (!hasBluetoothConnectPermission()) {
                AppLogger.logError("Нет разрешения BLUETOOTH_CONNECT для получения устройств", TAG)
                return emptyList()
            }

            val pairedDevices = adapter.bondedDevices
            pairedDevices?.map { androidDeviceToDomain(it) } ?: emptyList()
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при получении устройств: ${e.message}", TAG)
            emptyList()
        } catch (e: Exception) {
            AppLogger.logError("Ошибка получения устройств: ${e.message}", TAG)
            emptyList()
        }
    }

    /**
     * Проверить, сопряжено ли устройство по адресу.
     */
    fun isDevicePaired(address: String): Boolean {
        return try {
            if (!hasBluetoothConnectPermission()) {
                AppLogger.logError("Нет разрешения BLUETOOTH_CONNECT для проверки сопряжения", TAG)
                return false
            }

            getAndroidDeviceByAddress(address) != null
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при проверке сопряжения: ${e.message}", TAG)
            false
        }
    }

    // ========== МЕТОДЫ ДЛЯ ПОИСКА УСТРОЙСТВ ==========

    fun startDiscovery(
        onDeviceFound: (String) -> Unit,
        onDiscoveryFinished: () -> Unit,
        onDiscoveryError: (String) -> Unit
    ) {
        this.deviceFoundCallback = onDeviceFound
        this.discoveryFinishedCallback = onDiscoveryFinished
        this.discoveryErrorCallback = onDiscoveryError

        if (!hasDiscoveryPermissions()) {
            onDiscoveryError("Нет разрешений для поиска устройств")
            cleanupDiscoveryCallbacks()
            return
        }

        val adapter = bluetoothAdapter ?: run {
            onDiscoveryError("Bluetooth адаптер недоступен")
            cleanupDiscoveryCallbacks()
            return
        }

        if (!adapter.isEnabled) {
            onDiscoveryError("Bluetooth выключен")
            cleanupDiscoveryCallbacks()
            return
        }

        registerDiscoveryReceiver()

        val started = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        context!!,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    onDiscoveryError("Нет разрешения BLUETOOTH_SCAN для запуска поиска")
                    false
                } else {
                    adapter.startDiscovery()
                }
            } else {
                adapter.startDiscovery()
            }
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при запуске поиска: ${e.message}", TAG)
            onDiscoveryError("SecurityException при запуске поиска: ${e.message}")
            false
        } catch (e: Exception) {
            AppLogger.logError("Ошибка запуска поиска: ${e.message}", TAG)
            onDiscoveryError("Ошибка запуска поиска: ${e.message}")
            false
        }

        if (!started) {
            onDiscoveryError("Не удалось запустить поиск устройств")
            cleanupDiscoveryCallbacks()
            unregisterDiscoveryReceiver()
        } else {
            AppLogger.logInfo("Системный Bluetooth discovery запущен", TAG)
        }
    }

    fun stopDiscovery() {
        AppLogger.logInfo("Остановка Bluetooth discovery", TAG)

        try {
            bluetoothAdapter?.cancelDiscovery()
            AppLogger.logInfo("Bluetooth discovery остановлен", TAG)
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при остановке поиска: ${e.message}", TAG)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка остановки поиска: ${e.message}", TAG)
        }

        cleanupDiscoveryCallbacks()
        unregisterDiscoveryReceiver()
    }

    private fun registerDiscoveryReceiver() {
        val context = this.context ?: return

        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    AndroidBluetoothDevice.ACTION_FOUND -> {
                        try {
                            @Suppress("DEPRECATION")
                            val device: AndroidBluetoothDevice? = intent.getParcelableExtra(
                                AndroidBluetoothDevice.EXTRA_DEVICE
                            )

                            device?.let {
                                val address = it.address
                                val name = if (hasBluetoothConnectPermission()) {
                                    it.name ?: "Неизвестное устройство"
                                } else {
                                    "Неизвестное устройство"
                                }

                                if (!address.isNullOrBlank()) {
                                    AppLogger.logInfo(
                                        "Найдено устройство: ${name} ($address)",
                                        "BluetoothService"
                                    )
                                    deviceFoundCallback?.invoke(address)
                                }
                            }
                        } catch (e: SecurityException) {
                            AppLogger.logError(
                                "SecurityException при обработке устройства: ${e.message}",
                                "BluetoothService"
                            )
                        } catch (e: Exception) {
                            AppLogger.logError(
                                "Ошибка обработки устройства: ${e.message}",
                                "BluetoothService"
                            )
                        }
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                        AppLogger.logInfo("Система: Поиск устройств начат", "BluetoothService")
                    }

                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                        AppLogger.logInfo("Система: Поиск устройств завершен", "BluetoothService")
                        discoveryFinishedCallback?.invoke()
                        cleanupDiscoveryCallbacks()
                        unregisterDiscoveryReceiver()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }

        try {
            context.registerReceiver(discoveryReceiver, filter)
            AppLogger.logInfo("BroadcastReceiver зарегистрирован для поиска", "BluetoothService")
        } catch (e: Exception) {
            discoveryErrorCallback?.invoke("Ошибка регистрации receiver: ${e.message}")
            cleanupDiscoveryCallbacks()
        }
    }

    private fun unregisterDiscoveryReceiver() {
        discoveryReceiver?.let { receiver ->
            try {
                context?.unregisterReceiver(receiver)
                AppLogger.logInfo("BroadcastReceiver отменен", "BluetoothService")
            } catch (e: IllegalArgumentException) {
                // Receiver уже отменен
            }
        }
        discoveryReceiver = null
    }

    private fun cleanupDiscoveryCallbacks() {
        deviceFoundCallback = null
        discoveryFinishedCallback = null
        discoveryErrorCallback = null
    }

    // ========== МЕТОДЫ ДЛЯ CONNECTIONSTATEMANAGER ==========

    suspend fun connectToDevice(
        deviceData: BluetoothDeviceData,
        onConnected: (BluetoothDeviceData) -> Unit,
        onDisconnected: (BluetoothDeviceData) -> Unit,
        timeoutMs: Long = 10000L
    ): Pair<Boolean, String?> {
        return withContext(Dispatchers.IO) {
            try {
                if (currentState == BluetoothServiceState.CONNECTED && currentConnectionAddress == deviceData.address) {
                    return@withContext Pair(true, null)
                }

                if (currentState == BluetoothServiceState.CONNECTED) {
                    return@withContext Pair(false, "Уже подключено к другому устройству")
                }

                AppLogger.logInfo("Попытка подключения к устройству: ${deviceData.name} (${deviceData.address})", TAG)
                currentState = BluetoothServiceState.CONNECTING

                if (!hasBluetoothConnectPermission()) {
                    return@withContext Pair(false, "Нет разрешения BLUETOOTH_CONNECT")
                }

                val androidDevice = getAndroidDeviceByAddress(deviceData.address)
                if (androidDevice == null) {
                    return@withContext Pair(false, "Устройство не найдено в спаренных")
                }

                try {
                    withTimeout(timeoutMs) {
                        @Suppress("DEPRECATION")
                        val socket = androidDevice.createRfcommSocketToServiceRecord(SPP_UUID)

                        try {
                            bluetoothAdapter?.cancelDiscovery()
                        } catch (e: SecurityException) {
                            AppLogger.logError("SecurityException при отмене поиска перед подключением: ${e.message}", TAG)
                        } catch (e: Exception) {
                            AppLogger.logError("Ошибка отмены поиска перед подключением: ${e.message}", TAG)
                        }

                        socket.connect()

                        bluetoothSocket = socket
                        currentConnectionAddress = deviceData.address
                        currentState = BluetoothServiceState.CONNECTED

                        AppLogger.logInfo("Успешно подключено к устройству: ${deviceData.name}", TAG)
                        onConnected(deviceData)

                        Pair(true, null)
                    }
                } catch (e: TimeoutCancellationException) {
                    AppLogger.logError("Таймаут подключения к устройству: ${deviceData.name}", TAG)
                    cleanupSocket()
                    Pair(false, "Таймаут подключения: ${timeoutMs}ms")
                } catch (e: Exception) {
                    AppLogger.logError("Ошибка подключения: ${e.message}", TAG)
                    cleanupSocket()
                    currentState = BluetoothServiceState.ERROR
                    onDisconnected(deviceData)
                    Pair(false, "Ошибка подключения: ${e.message}")
                }
            } catch (e: SecurityException) {
                AppLogger.logError("SecurityException при подключении: ${e.message}", TAG)
                cleanupSocket()
                currentState = BluetoothServiceState.ERROR
                Pair(false, "SecurityException: ${e.message}")
            } catch (e: IOException) {
                AppLogger.logError("IOException при подключении: ${e.message}", TAG)
                cleanupSocket()
                currentState = BluetoothServiceState.ERROR
                onDisconnected(deviceData)
                Pair(false, "Ошибка ввода-вывода: ${e.message}")
            } catch (e: Exception) {
                AppLogger.logError("Ошибка подключения: ${e.message}", TAG)
                cleanupSocket()
                currentState = BluetoothServiceState.ERROR
                Pair(false, "Ошибка подключения: ${e.message}")
            }
        }
    }

    fun disconnect() {
        try {
            cleanupSocket()
            currentState = BluetoothServiceState.DISCONNECTED
            AppLogger.logInfo("Соединение разорвано", TAG)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка отключения: ${e.message}", TAG)
        }
    }

    private fun cleanupSocket() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            AppLogger.logError("Ошибка закрытия сокета: ${e.message}", TAG)
        }

        bluetoothSocket = null
        currentConnectionAddress = null
        stopDataListening()
    }

    // ========== МОНИТОРИНГ СОСТОЯНИЯ BLUETOOTH ==========

    /**
     * Запустить мониторинг состояния Bluetooth адаптера.
     * Вызывается один раз при инициализации сервиса.
     */
    private fun startBluetoothStateMonitoring() {
        if (bluetoothStateReceiver != null) {
            return // Уже запущен
        }

        val context = this.context ?: return

        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )

                        val isEnabled = when (state) {
                            BluetoothAdapter.STATE_ON -> {
                                AppLogger.logInfo("Системное событие: Bluetooth включен", TAG)
                                true
                            }
                            BluetoothAdapter.STATE_OFF -> {
                                AppLogger.logInfo("Системное событие: Bluetooth выключен", TAG)
                                false
                            }
                            else -> null
                        }

                        // Уведомляем все зарегистрированные callback-функции
                        isEnabled?.let { enabled ->
                            val callbacksSnapshot = bluetoothStateCallbacks.values.toList()
                            callbacksSnapshot.forEach { callback ->
                                try {
                                    callback(enabled)
                                } catch (e: Exception) {
                                    AppLogger.logError("Ошибка в callback мониторинга Bluetooth: ${e.message}", TAG)
                                }
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        try {
            context.registerReceiver(bluetoothStateReceiver, filter)
            AppLogger.logInfo("Мониторинг состояния Bluetooth активирован", TAG)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка регистрации BluetoothStateReceiver: ${e.message}", TAG)
        }
    }

    fun monitorBluetoothState(callback: (Boolean) -> Unit): Closeable {
        val callbackId = UUID.randomUUID().toString()

        // Добавляем callback в коллекцию
        bluetoothStateCallbacks[callbackId] = callback

        // НЕ запускаем мониторинг - он уже запущен при инициализации
        // НЕ вызываем callback немедленно - только при изменении состояния

        AppLogger.logInfo("Добавлен callback мониторинга состояния Bluetooth. ID: $callbackId. Всего: ${bluetoothStateCallbacks.size}", TAG)

        return object : Closeable {
            override fun close() {
                stopBluetoothMonitoring(callbackId)
            }
        }
    }

    private fun stopBluetoothMonitoring(callbackId: String) {
        val removed = bluetoothStateCallbacks.remove(callbackId) != null

        if (removed) {
            AppLogger.logInfo("Удален callback мониторинга состояния Bluetooth. ID: $callbackId. Осталось: ${bluetoothStateCallbacks.size}", TAG)
        }

        // НЕ останавливаем мониторинг даже если callback-ов не осталось
        // Мониторинг работает постоянно
    }

    private fun unregisterBluetoothStateReceiver() {
        val context = this.context ?: return
        val receiver = bluetoothStateReceiver ?: return

        try {
            context.unregisterReceiver(receiver)
            AppLogger.logInfo("BroadcastReceiver состояния Bluetooth отменен", TAG)
        } catch (e: IllegalArgumentException) {
            // Receiver уже отменен
        } catch (e: Exception) {
            AppLogger.logError("Ошибка отмены BluetoothStateReceiver: ${e.message}", TAG)
        }

        bluetoothStateReceiver = null
    }

    // ========== МОНИТОРИНГ СОСТОЯНИЯ СОЕДИНЕНИЯ ==========

    fun startConnectionMonitoring(deviceData: BluetoothDeviceData, callback: (Boolean, BluetoothDeviceData?) -> Unit) {
        if (isMonitoringConnection.get()) {
            AppLogger.logInfo("Мониторинг соединения уже активен", TAG)
            return
        }

        if (!hasBluetoothConnectPermission()) {
            AppLogger.logError("Нет разрешения BLUETOOTH_CONNECT для мониторинга", TAG)
            callback(false, null)
            return
        }

        this.connectionStateCallback = callback
        isMonitoringConnection.set(true)
        registerConnectionBroadcastReceiver(deviceData.address)
        AppLogger.logInfo("Мониторинг соединения активирован для ${deviceData.name}", TAG)
    }

    private fun registerConnectionBroadcastReceiver(targetAddress: String) {
        val context = this.context ?: return

        connectionBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    @Suppress("DEPRECATION")
                    val device: AndroidBluetoothDevice? = intent.getParcelableExtra(
                        AndroidBluetoothDevice.EXTRA_DEVICE
                    )

                    if (device?.address != targetAddress) {
                        return
                    }

                    when (intent.action) {
                        AndroidBluetoothDevice.ACTION_ACL_CONNECTED -> {
                            AppLogger.logInfo("Система: устройство подключено: $targetAddress", TAG)
                            currentState = BluetoothServiceState.CONNECTED
                            currentConnectionAddress = targetAddress
                            val deviceData = BluetoothDeviceData(
                                address = targetAddress,
                                name = device.name ?: "Неизвестное устройство"
                            )
                            connectionStateCallback?.invoke(true, deviceData)
                        }

                        AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            AppLogger.logInfo("Система: устройство отключено: $targetAddress", TAG)
                            currentState = BluetoothServiceState.DISCONNECTED
                            currentConnectionAddress = null
                            val deviceData = BluetoothDeviceData(
                                address = targetAddress,
                                name = device.name ?: "Неизвестное устройство"
                            )
                            connectionStateCallback?.invoke(false, deviceData)
                        }

                        AndroidBluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED -> {
                            AppLogger.logInfo("Система: запрос на отключение устройства: $targetAddress", TAG)
                        }
                    }
                } catch (e: SecurityException) {
                    AppLogger.logError("SecurityException при обработке события: ${e.message}", TAG)
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(AndroidBluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        }

        try {
            context.registerReceiver(connectionBroadcastReceiver, filter)
            AppLogger.logInfo("BroadcastReceiver зарегистрирован для $targetAddress", TAG)
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при регистрации receiver: ${e.message}", TAG)
        } catch (e: Exception) {
            AppLogger.logError("Ошибка регистрации BroadcastReceiver: ${e.message}", TAG)
        }
    }

    fun stopConnectionMonitoring() {
        if (!isMonitoringConnection.get()) {
            return
        }

        unregisterConnectionBroadcastReceiver()
        connectionStateCallback = null
        isMonitoringConnection.set(false)
        AppLogger.logInfo("Мониторинг соединения деактивирован", TAG)
    }

    private fun unregisterConnectionBroadcastReceiver() {
        val context = this.context ?: return
        val receiver = connectionBroadcastReceiver ?: return

        try {
            context.unregisterReceiver(receiver)
            AppLogger.logInfo("BroadcastReceiver отменен", TAG)
        } catch (e: IllegalArgumentException) {
            // Receiver уже отменен
        } catch (e: Exception) {
            AppLogger.logError("Ошибка отмены BroadcastReceiver: ${e.message}", TAG)
        }

        connectionBroadcastReceiver = null
    }

    // ========== МЕТОДЫ ДЛЯ DATASTREAMHANDLER ==========

    suspend fun sendData(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = bluetoothSocket
            if (socket == null || !socket.isConnected) {
                AppLogger.logError("Нет активного соединения для отправки", TAG)
                return@withContext false
            }

            val outputStream = socket.outputStream
            val bytes = data.toByteArray(Charsets.UTF_8)

            outputStream.write(bytes)
            outputStream.flush()

            AppLogger.logInfo("Отправлено ${bytes.size} байт: ${data.take(50)}...", TAG)
            true
        } catch (e: IOException) {
            AppLogger.logError("IOException при отправке данных: ${e.message}", TAG)
            handleConnectionError()
            false
        } catch (e: Exception) {
            AppLogger.logError("Ошибка отправки данных: ${e.message}", TAG)
            false
        }
    }

    fun startDataListening(): Flow<String> = channelFlow {
        if (isListeningForData.get()) {
            AppLogger.logError("Прослушивание уже активнo", TAG)
            return@channelFlow
        }

        isListeningForData.set(true)

        if (dataScope == null) {
            dataScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        }

        AppLogger.logInfo("Начало прослушивания входящих данных", TAG)

        dataListeningJob = dataScope?.launch {
            try {
                val socket = bluetoothSocket
                if (socket == null || !socket.isConnected) {
                    AppLogger.logError("Нет активного соединения для прослушивания", TAG)
                    return@launch
                }

                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (isListeningForData.get() && socket.isConnected) {
                    try {
                        bytesRead = inputStream.read(buffer)
                        if (bytesRead > 0) {
                            val data = String(buffer, 0, bytesRead, Charsets.UTF_8)
                            send(data)
                            AppLogger.logInfo("Получено $bytesRead байт", TAG)
                        }
                    } catch (e: IOException) {
                        AppLogger.logError("IOException при чтении данных: ${e.message}", TAG)
                        break
                    }
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка прослушивания: ${e.message}", TAG)
            } finally {
                isListeningForData.set(false)
            }
        }

        awaitClose {
            isListeningForData.set(false)
            AppLogger.logInfo("Поток прослушивания закрыт", TAG)
        }
    }

    fun stopDataListening() {
        dataListeningJob?.cancel("Остановка прослушивания данных")
        dataListeningJob = null
        isListeningForData.set(false)
        AppLogger.logInfo("Прослушивание данных остановлено", TAG)
    }

    private fun handleConnectionError() {
        AppLogger.logInfo("Обработка ошибки соединения", TAG)
        cleanupSocket()
        currentState = BluetoothServiceState.DISCONNECTED
    }

    // ========== ПРОВЕРКИ РАЗРЕШЕНИЙ ==========

    private fun hasBluetoothConnectPermission(): Boolean {
        val context = this.context ?: return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (e: Exception) {
            AppLogger.logError("Ошибка проверки разрешения BLUETOOTH_CONNECT: ${e.message}", TAG)
            false
        }
    }

    private fun hasDiscoveryPermissions(): Boolean {
        val context = this.context ?: return false

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val hasScan = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_SCAN
                ) == PackageManager.PERMISSION_GRANTED

                val hasConnect = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED

                hasScan && hasConnect
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        } catch (e: Exception) {
            AppLogger.logError("Ошибка проверки разрешений: ${e.message}", TAG)
            false
        }
    }
}

/** Текущее состояние сервиса Bluetooth */
private enum class BluetoothServiceState {
    IDLE, CONNECTING, CONNECTED, DISCONNECTED, ERROR
}
