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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BluetoothConnectionManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context
    private lateinit var bluetoothService: AppBluetoothService
    private lateinit var manager: BluetoothConnectionManager
    
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        bluetoothService = mockk(relaxed = true)
        
        // Мокаем конструкторы помощников
        mockkConstructor(ConnectionFeasibilityChecker::class)
        mockkConstructor(DeviceAvailabilityMonitor::class)
        mockkConstructor(ConnectionStateManager::class)
        mockkConstructor(DataStreamHandler::class)

        // Настраиваем заглушки для сконструированных объектов
        every { anyConstructed<ConnectionFeasibilityChecker>().start(any()) } just runs
        every { anyConstructed<ConnectionFeasibilityChecker>().stop() } just runs
        
        every { anyConstructed<DeviceAvailabilityMonitor>().start(any()) } just runs
        every { anyConstructed<DeviceAvailabilityMonitor>().stop() } just runs
        
        every { anyConstructed<ConnectionStateManager>().start(any()) } just runs
        every { anyConstructed<ConnectionStateManager>().stop() } just runs
        
        every { anyConstructed<DataStreamHandler>().stop() } just runs

        // Инициализируем менеджер
        manager = BluetoothConnectionManager(context, bluetoothService)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `should start connection process when device is first selected`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val settings = AppSettings(selectedDevice = device)
        
        manager.updateSettings(settings)
        runCurrent()
        
        verify { anyConstructed<ConnectionFeasibilityChecker>().start(device) }
    }

    @Test
    fun `should restart connection process when device changes`() = runTest(testDispatcher) {
        val device1 = BluetoothDeviceData("Device1", "00:11:22:33:44:55")
        val device2 = BluetoothDeviceData("Device2", "AA:BB:CC:DD:EE:FF")
        
        manager.updateSettings(AppSettings(selectedDevice = device1))
        runCurrent()
        manager.updateSettings(AppSettings(selectedDevice = device2))
        runCurrent()
        
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<ConnectionFeasibilityChecker>().start(device2) }
    }

    @Test
    fun `retryConnection should stop all and start CFC`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        manager.updateSettings(AppSettings(selectedDevice = device))
        runCurrent()
        
        manager.startConnectionProcess()
        runCurrent()
        
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<DeviceAvailabilityMonitor>().stop() }
        verify { anyConstructed<ConnectionStateManager>().stop() }
        verify { anyConstructed<DataStreamHandler>().stop() }
        verify(atLeast = 2) { anyConstructed<ConnectionFeasibilityChecker>().start(device) }
    }
    
    @Test
    fun `should stop all processes on cleanup`() = runTest(testDispatcher) {
        manager.cleanup()
        runCurrent()
        
        verify { anyConstructed<ConnectionFeasibilityChecker>().stop() }
        verify { anyConstructed<DeviceAvailabilityMonitor>().stop() }
        verify { anyConstructed<ConnectionStateManager>().stop() }
        verify { anyConstructed<DataStreamHandler>().stop() }
    }
}
