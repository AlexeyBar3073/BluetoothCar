package com.alexbar3073.bluetoothcar.ui.screens.devices.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alexbar3073.bluetoothcar.ui.theme.AppColors

@Composable
fun BluetoothStatusCard(
    status: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = AppColors.ErrorAlpha
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .background(
                        AppColors.Error.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .padding(4.dp)
            ) {
                androidx.compose.material3.Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Ошибка",
                    tint = AppColors.Error,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = AppColors.Error.copy(alpha = 0.9f),
                textAlign = TextAlign.Start,
                modifier = Modifier.weight(1f)
            )
        }
    }
}