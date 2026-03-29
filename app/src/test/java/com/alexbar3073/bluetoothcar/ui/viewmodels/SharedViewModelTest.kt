// Файл: app/src/test/java/com/alexbar3073/bluetoothcar/ui/viewmodels/SharedViewModelTest.kt
package com.alexbar3073.bluetoothcar.ui.viewmodels

import app.cash.turbine.test
import com.alexbar3073.bluetoothcar.core.AppController
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.AppSettings
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.data.models.CarData
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var appController: AppController
    private lateinit var viewModel: SharedViewModel

    private val connectionStatusFlow = MutableStateFlow(ConnectionState.UNDEFINED.toStatusInfo())
    private val carDataFlow = MutableStateFlow(CarData())
    private val appSettingsFlow = MutableStateFlow(AppSettings())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        appController = mockk(relaxed = true)

        every { appController.connectionStatusInfo } returns connectionStatusFlow
        every { appController.carData } returns carDataFlow
        every { appController.appSettings } returns appSettingsFlow

        viewModel = SharedViewModel(appController)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewModel should reflect car data from controller ignoring timestamp`() = runTest {
        val testCarData = CarData(speed = 60f, fuel = 25f)
        
        viewModel.carData.test {
            // Пропускаем начальное значение, так как у него всегда рандомный timestamp
            awaitItem() 
            
            carDataFlow.value = testCarData
            val received = awaitItem()
            
            assertEquals(testCarData.speed, received.speed)
            assertEquals(testCarData.fuel, received.fuel)
        }
    }

    @Test
    fun `isConnected should be true when state is active`() = runTest {
        viewModel.isConnected.test {
            assertEquals(false, awaitItem())

            connectionStatusFlow.value = ConnectionState.LISTENING_DATA.toStatusInfo()
            assertEquals(true, awaitItem())
        }
    }

    @Test
    fun `selectBluetoothDevice should update settings in appController`() {
        val device = BluetoothDeviceData("Test Device", "00:11:22:33:44:55")
        val currentSettings = AppSettings(fuelTankCapacity = 50f)
        every { appController.getCurrentSettings() } returns currentSettings

        viewModel.selectBluetoothDevice(device)

        verify { 
            appController.updateSettings(match { 
                it.selectedDevice == device && it.fuelTankCapacity == 50f 
            }) 
        }
    }
}
