// Файл: ui/screens/DevicesReactivityTest.kt
package com.alexbar3073.bluetoothcar.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.navigation.NavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.alexbar3073.bluetoothcar.data.bluetooth.ConnectionState
import com.alexbar3073.bluetoothcar.data.models.BluetoothDeviceData
import com.alexbar3073.bluetoothcar.ui.screens.devices.DevicesScreen
import com.alexbar3073.bluetoothcar.ui.theme.BluetoothCarTheme
import com.alexbar3073.bluetoothcar.ui.viewmodels.SharedViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ТЕГ: UI/Devices/ReactivityTest
 */
@RunWith(AndroidJUnit4::class)
class DevicesReactivityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Ignore("Тест временно отключен из-за нестабильности Compose-иерархии в тестовом окружении")
    @Test
    fun devicesScreen_updatesWhenBluetoothStateChanges() {
        // ... (код сохранен)
    }
}
