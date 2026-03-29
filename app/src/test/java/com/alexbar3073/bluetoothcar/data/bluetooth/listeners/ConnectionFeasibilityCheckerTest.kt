package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.Closeable

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionFeasibilityCheckerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothService: AppBluetoothService
    private lateinit var checker: ConnectionFeasibilityChecker
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    private val monitorCallbackSlot = slot<(Boolean) -> Unit>()
    
    // Используем UnconfinedTestDispatcher для мгновенного выполнения корутин в тестах
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        // Передаем тестовый диспетчер в конструктор
        checker = ConnectionFeasibilityChecker(bluetoothService, stateCallback, testDispatcher)
    }

    @Test
    fun `should return NO_DEVICE_SELECTED if device is null`() = runTest(testDispatcher) {
        checker.start(null)
        runCurrent()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, match { it.contains("не выбрано") }) }
    }

    @Test
    fun `should return NO_DEVICE_SELECTED if address is blank`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "  ")
        
        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, match { it.contains("не выбрано") }) }
    }

    @Test
    fun `should return ERROR if bluetooth adapter is not available`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns false

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("недоступен") }) }
    }

    @Test
    fun `should return BLUETOOTH_DISABLED if adapter is off`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns false
        every { bluetoothService.monitorBluetoothState(capture(monitorCallbackSlot)) } returns mockk<Closeable>(relaxed = true)

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.BLUETOOTH_DISABLED, any()) }
        verify { bluetoothService.monitorBluetoothState(any()) }
    }

    @Test
    fun `should return DEVICE_SELECTED if all checks pass`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns true
        every { bluetoothService.isDevicePaired(device.address) } returns true

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.DEVICE_SELECTED, null) }
    }

    @Test
    fun `should return NO_DEVICE_SELECTED if device is not paired`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns true
        every { bluetoothService.isDevicePaired(device.address) } returns false

        checker.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.NO_DEVICE_SELECTED, match { it.contains("сопряжено") }) }
    }

    @Test
    fun `monitoring should trigger DEVICE_SELECTED when bluetooth is enabled`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns false
        every { bluetoothService.monitorBluetoothState(capture(monitorCallbackSlot)) } returns mockk<Closeable>(relaxed = true)
        
        checker.start(device)
        runCurrent()

        if (monitorCallbackSlot.isCaptured) {
            monitorCallbackSlot.captured(true)
            runCurrent()
            verify { stateCallback(ConnectionState.DEVICE_SELECTED, null) }
        }
    }
    
    @Test
    fun `monitoring should trigger DISCONNECTED when bluetooth is disabled`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Test", "00:11:22:33:44:55")
        
        every { bluetoothService.bluetoothAdapterIsAvailable() } returns true
        every { bluetoothService.bluetoothAdapterIsEnabled() } returns true
        every { bluetoothService.isDevicePaired(device.address) } returns true
        every { bluetoothService.monitorBluetoothState(capture(monitorCallbackSlot)) } returns mockk<Closeable>(relaxed = true)
        
        checker.start(device)
        runCurrent()

        if (monitorCallbackSlot.isCaptured) {
            monitorCallbackSlot.captured(false)
            runCurrent()
            verify { stateCallback(ConnectionState.DISCONNECTED, match { it.contains("отключен") }) }
        }
    }
}
