package com.alexbar3073.bluetoothcar.data.bluetooth.listeners

import com.alexbar3073.bluetoothcar.MainDispatcherRule
import com.alexbar3073.bluetoothcar.data.bluetooth.AppBluetoothService
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectionStateManagerTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var bluetoothService: AppBluetoothService
    private lateinit var stateManager: ConnectionStateManager
    private lateinit var stateCallback: (ConnectionState, String?) -> Unit
    
    // Используем UnconfinedTestDispatcher для тестов, чтобы корутины запускались сразу
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        bluetoothService = mockk(relaxed = true)
        stateCallback = mockk(relaxed = true)
        // Передаем testDispatcher в конструктор для синхронизации с runTest
        stateManager = ConnectionStateManager(bluetoothService, stateCallback, testDispatcher)
    }

    @Test
    fun `should return CONNECTED on successful connection and start monitoring`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = eq(device),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } answers {
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }

        stateManager.start(device)
        runCurrent()

        verify { stateCallback(ConnectionState.CONNECTING, null) }
        verify { stateCallback(ConnectionState.CONNECTED, null) }
        verify { bluetoothService.startConnectionMonitoring(eq(device), any()) }
    }

    @Test
    fun `should return ERROR after 5 failed attempts`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } returns Pair(false, "Connection refused")

        stateManager.start(device)
        
        // В CSM 5 попыток. Между попытками задержка 2000мс.
        // Нам нужно промотать время, чтобы выполнились все ретраи.
        advanceTimeBy(15000) 
        runCurrent()

        coVerify(exactly = 5) { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        }
        
        // Проверяем финальный статус ошибки после 5 попыток
        verify { stateCallback(ConnectionState.ERROR, match { it.contains("после 5 попыток") }) }
    }

    @Test
    fun `should return DISCONNECTED when active connection is lost`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        val monitorCallbackSlot = slot<(Boolean, BluetoothDeviceData?) -> Unit>()

        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            ) 
        } answers {
            val onConnected = secondArg<(BluetoothDeviceData) -> Unit>()
            onConnected(device)
            Pair(true, null)
        }
        
        every { 
            bluetoothService.startConnectionMonitoring(eq(device), capture(monitorCallbackSlot)) 
        } just runs

        stateManager.start(device)
        runCurrent()

        if (monitorCallbackSlot.isCaptured) {
            monitorCallbackSlot.captured(false, device)
            runCurrent()
            verify { stateCallback(ConnectionState.DISCONNECTED, null) }
        }
    }

    @Test
    fun `should return ERROR if start is called while already attempting connection`() = runTest(testDispatcher) {
        val device = BluetoothDeviceData("Car", "00:11:22:33:44:55")
        
        // Заставляем первую попытку "подвиснуть"
        coEvery { 
            bluetoothService.connectToDevice(
                deviceData = any(),
                onConnected = any(),
                onDisconnected = any(),
                timeoutMs = any()
            )
        } coAnswers { 
            kotlinx.coroutines.delay(5000)
            Pair(true, null)
        }

        stateManager.start(device)
        // Вторая попытка в тот же момент
        stateManager.start(device)

        verify { stateCallback(ConnectionState.ERROR, match { it.contains("уже выполняется") }) }
    }
}
