package com.alexbar3073.bluetoothcar.data.bluetooth

import android.content.Context
import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.ConnectionFeasibilityChecker
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.ConnectionStateManager
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.DataStreamHandler
import com.alexbar3073.bluetoothcar.data.bluetooth.listeners.DeviceAvailabilityMonitor
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothConnectionManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var manager: BluetoothConnectionManager
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        
        // Мокаем конструкторы помощников
        mockkConstructor(ConnectionFeasibilityChecker::class)
        mockkConstructor(DeviceAvailabilityMonitor::class)
        mockkConstructor(ConnectionStateManager::class)
        mockkConstructor(DataStreamHandler::class)

        // Инициализируем менеджер
        manager = BluetoothConnectionManager(context)
    }

    @Test
    fun `should restart connection process when device changes`() = runTest {
        val device1 = BluetoothDeviceData("Device1", "00:11:22:33:44:55")
        val device2 = BluetoothDeviceData("Device2", "AA:BB:CC:DD:EE:FF")
        
        manager.updateSettings(AppSettings(selectedDevice = device1))
        verify { anyConstructed<ConnectionFeasibilityChecker>().start(device1) }

        manager.updateSettings(AppSettings(selectedDevice = device2))
        
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<ConnectionFeasibilityChecker>().start(device2) }
    }

    @Test
    fun `should not restart connection if device is same`() = runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val settings = AppSettings(selectedDevice = device)
        
        manager.updateSettings(settings)
        verify(exactly = 1) { anyConstructed<ConnectionFeasibilityChecker>().start(device) }
        
        manager.updateSettings(settings.copy(fuelTankCapacity = 60f))
        
        verify(exactly = 1) { anyConstructed<ConnectionFeasibilityChecker>().start(any()) }
    }

    @Test
    fun `retryConnection should stop all and start CFC`() = runTest {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        manager.updateSettings(AppSettings(selectedDevice = device))
        
        manager.startConnectionProcess()
        
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<DeviceAvailabilityMonitor>().stop() }
        verify { anyConstructed<ConnectionStateManager>().stop() }
        verify { anyConstructed<DataStreamHandler>().stop() }
        verify(atLeast = 2) { anyConstructed<ConnectionFeasibilityChecker>().start(device) }
    }
}
