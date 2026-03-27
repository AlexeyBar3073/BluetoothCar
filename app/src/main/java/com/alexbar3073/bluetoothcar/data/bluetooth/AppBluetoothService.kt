// Файл: data/bluetooth/AppBluetoothService.kt
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

/**
 * Низкоуровневый сервис для работы с Android Bluetooth API.
 * Обертка над BluetoothAdapter и BluetoothSocket.
 */
class AppBluetoothService {
    companion object {
        private const val TAG = "AppBluetoothService"
        private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    }

    // ========== СОСТОЯНИЯ И КОНТЕКСТ ==========

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

    /** Публичное свойство состояния подключения согласно принципу единственного источника истины */
    val isConnected: Boolean
        get() = bluetoothSocket?.isConnected == true

    // ========== МОНИТОРИНГ СОЕДИНЕНИЯ ==========

    /** Callback для уведомления об изменении состояния соединения */
    private var connectionStateCallback: ((Boolean, BluetoothDeviceData?) -> Unit)? = null

    /** BroadcastReceiver для мониторинга событий подключения/отключения */
    private var connectionBroadcastReceiver: BroadcastReceiver? = null

    // ========== МОНИТОРИНГ СОСТОЯНИЯ BLUETOOTH ==========

    /** Потокобезопасная коллекция callback-функций для мониторинга состояния Bluetooth (адаптера) */
    private val bluetoothStateCallbacks = ConcurrentHashMap<String, (Boolean) -> Unit>()

    /** BroadcastReceiver для мониторинга включения/выключения Bluetooth адаптера */
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
        startBluetoothStateMonitoring()
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

    // ========== ПУБЛИЧНЫЙ API ==========

    fun bluetoothAdapterIsAvailable(): Boolean = bluetoothAdapter != null

    fun bluetoothAdapterIsEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

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
        } catch (e: Exception) {
            AppLogger.logError("Ошибка получения устройств: ${e.message}", TAG)
            emptyList()
        }
    }

    /**
     * Проверить, сопряжено ли устройство по адресу.
     */
    fun isDevicePaired(address: String): Boolean {
        if (!hasBluetoothConnectPermission()) return false
        return getAndroidDeviceByAddress(address) != null
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

        registerDiscoveryReceiver()

        val started = try {
            adapter.startDiscovery()
        } catch (e: SecurityException) {
            onDiscoveryError("SecurityException при запуске поиска")
            false
        } catch (e: Exception) {
            onDiscoveryError("Ошибка запуска поиска: ${e.message}")
            false
        }

        if (!started) {
            cleanupDiscoveryCallbacks()
            unregisterDiscoveryReceiver()
        }
    }

    fun stopDiscovery() {
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            AppLogger.logError("SecurityException при остановке поиска", TAG)
        }
        cleanupDiscoveryCallbacks()
        unregisterDiscoveryReceiver()
    }

    private fun registerDiscoveryReceiver() {
        val context = this.context ?: return
        if (discoveryReceiver != null) return

        discoveryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
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
                    BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
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
        context.registerReceiver(discoveryReceiver, filter)
    }

    private fun unregisterDiscoveryReceiver() {
        discoveryReceiver?.let { context?.unregisterReceiver(it) }
        discoveryReceiver = null
    }

    private fun cleanupDiscoveryCallbacks() {
        deviceFoundCallback = null
        discoveryFinishedCallback = null
        discoveryErrorCallback = null
    }

    // ========== МЕТОДЫ ПОДКЛЮЧЕНИЯ ==========

    suspend fun connectToDevice(
        deviceData: BluetoothDeviceData,
        onConnected: (BluetoothDeviceData) -> Unit,
        onDisconnected: (BluetoothDeviceData) -> Unit,
        timeoutMs: Long = 10000L
    ): Pair<Boolean, String?> = withContext(Dispatchers.IO) {
        try {
            if (isConnected && currentConnectionAddress == deviceData.address) {
                return@withContext Pair(true, null)
            }

            val androidDevice = getAndroidDeviceByAddress(deviceData.address)
                ?: return@withContext Pair(false, "Устройство не найдено")

            withTimeout(timeoutMs) {
                @Suppress("DEPRECATION")
                val socket = androidDevice.createRfcommSocketToServiceRecord(SPP_UUID)
                
                try { bluetoothAdapter?.cancelDiscovery() } catch (e: SecurityException) {}
                
                socket.connect()
                bluetoothSocket = socket
                currentConnectionAddress = deviceData.address
                
                onConnected(deviceData)
                Pair(true, null)
            }
        } catch (e: Exception) {
            cleanupSocket()
            onDisconnected(deviceData)
            Pair(false, e.message ?: "Ошибка подключения")
        }
    }

    fun disconnect() {
        cleanupSocket()
    }

    private fun cleanupSocket() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            AppLogger.logError("Ошибка закрытия сокета", TAG)
        }
        bluetoothSocket = null
        currentConnectionAddress = null
        stopDataListening()
    }

    // ========== МОНИТОРИНГ СОСТОЯНИЯ BLUETOOTH АДАПТЕРА ==========

    private fun startBluetoothStateMonitoring() {
        if (bluetoothStateReceiver != null) return
        val context = this.context ?: return

        bluetoothStateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val isEnabled = state == BluetoothAdapter.STATE_ON
                    bluetoothStateCallbacks.values.forEach { it(isEnabled) }
                }
            }
        }
        context.registerReceiver(bluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    fun monitorBluetoothState(callback: (Boolean) -> Unit): Closeable {
        val id = UUID.randomUUID().toString()
        bluetoothStateCallbacks[id] = callback
        return Closeable { bluetoothStateCallbacks.remove(id) }
    }

    // ========== МОНИТОРИНГ СОСТОЯНИЯ СОЕДИНЕНИЯ ==========

    fun startConnectionMonitoring(deviceData: BluetoothDeviceData, callback: (Boolean, BluetoothDeviceData?) -> Unit) {
        if (isMonitoringConnection.getAndSet(true)) return
        this.connectionStateCallback = callback
        registerConnectionBroadcastReceiver(deviceData.address)
    }

    private fun registerConnectionBroadcastReceiver(targetAddress: String) {
        val context = this.context ?: return
        connectionBroadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                @Suppress("DEPRECATION")
                val device: AndroidBluetoothDevice? = intent.getParcelableExtra(AndroidBluetoothDevice.EXTRA_DEVICE)
                if (device?.address != targetAddress) return

                when (intent.action) {
                    AndroidBluetoothDevice.ACTION_ACL_CONNECTED -> {
                        val deviceData = BluetoothDeviceData(targetAddress, device.name ?: "Unknown")
                        connectionStateCallback?.invoke(true, deviceData)
                    }
                    AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                        cleanupSocket() // При разрыве сбрасываем локальное состояние
                        val deviceData = BluetoothDeviceData(targetAddress, device.name ?: "Unknown")
                        connectionStateCallback?.invoke(false, deviceData)
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(AndroidBluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(AndroidBluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(connectionBroadcastReceiver, filter)
    }

    fun stopConnectionMonitoring() {
        if (!isMonitoringConnection.getAndSet(false)) return
        connectionBroadcastReceiver?.let { context?.unregisterReceiver(it) }
        connectionBroadcastReceiver = null
        connectionStateCallback = null
    }

    // ========== ПЕРЕДАЧА ДАННЫХ ==========

    suspend fun sendData(data: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val socket = bluetoothSocket ?: return@withContext false
            if (!socket.isConnected) return@withContext false
            
            socket.outputStream.write(data.toByteArray(Charsets.UTF_8))
            socket.outputStream.flush()
            true
        } catch (e: Exception) {
            cleanupSocket()
            false
        }
    }

    fun startDataListening(): Flow<String> = channelFlow {
        if (isListeningForData.getAndSet(true)) return@channelFlow
        
        dataScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        dataListeningJob = dataScope?.launch {
            try {
                val socket = bluetoothSocket ?: return@launch
                val inputStream = socket.inputStream
                val buffer = ByteArray(1024)
                while (isListeningForData.get() && socket.isConnected) {
                    val read = inputStream.read(buffer)
                    if (read > 0) send(String(buffer, 0, read, Charsets.UTF_8))
                }
            } catch (e: Exception) {
                AppLogger.logError("Ошибка чтения данных", TAG)
            } finally {
                isListeningForData.set(false)
            }
        }

        awaitClose { 
            isListeningForData.set(false) 
            dataListeningJob?.cancel()
        }
    }

    fun stopDataListening() {
        isListeningForData.set(false)
        dataListeningJob?.cancel()
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    private fun getAndroidDeviceByAddress(address: String): AndroidBluetoothDevice? {
        if (!hasBluetoothConnectPermission()) return null
        return bluetoothAdapter?.bondedDevices?.firstOrNull { it.address == address }
    }

    private fun androidDeviceToDomain(device: AndroidBluetoothDevice): BluetoothDeviceData {
        val name = if (hasBluetoothConnectPermission()) device.name else "Unknown"
        return BluetoothDeviceData(device.address, name ?: "Unknown")
    }

    private fun hasBluetoothConnectPermission(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun hasDiscoveryPermissions(): Boolean {
        val ctx = context ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val hasScan = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            val hasConnect = ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            hasScan && hasConnect
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else true
    }
}
