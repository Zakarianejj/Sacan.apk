package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScannedDeviceEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalCard(
    device: ScannedDeviceEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // RSSI strength colors
    val signalColor = when {
        device.rssi >= -50 -> SignalExcellent
        device.rssi >= -65 -> SignalGood
        device.rssi >= -80 -> SignalFair
        else -> SignalPoor
    }

    // Match icons
    val icon = when (device.signalType) {
        "WIFI" -> Icons.Default.Wifi
        "BLUETOOTH" -> Icons.Default.Bluetooth
        else -> Icons.Default.Lan
    }

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("signal_card_${device.macAddress}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xAA10141E)
        ),
        border = BorderStroke(
            1.dp, 
            Brush.linearGradient(
                colors = listOf(GlassBorder, signalColor.copy(alpha = 0.3f))
            )
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Signal type visual node
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = signalColor.copy(alpha = 0.15f),
                border = BorderStroke(1.dp, signalColor.copy(alpha = 0.4f))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = device.signalType,
                        tint = signalColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (device.name.isEmpty()) "Unnamed Device" else device.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(3.dp))
                
                // MAC Info & Vendor
                Text(
                    text = "${device.macAddress} • ${device.manufacturer}",
                    color = CyberGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Stats footer tag row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Channel or security block
                    DeviceStatusTag(text = device.frequency, color = CyberCyan)
                    if (device.channel != "N/A" && device.channel.isNotEmpty()) {
                        DeviceStatusTag(text = device.channel, color = CyberPurple)
                    }
                    DeviceStatusTag(
                        text = "${String.format("%.1f", device.distance)}m", 
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // dBm readouts
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${device.rssi} dBm",
                    color = signalColor,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                // Strength classification text in Arabic
                val levelArabic = when {
                    device.rssi >= -50 -> "ممتازة"
                    device.rssi >= -65 -> "جيدة جيدة"
                    device.rssi >= -80 -> "مقبولة"
                    else -> "ضعيفة جداً"
                }
                Text(
                    text = levelArabic,
                    color = signalColor.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun DeviceStatusTag(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.12f),
        border = BorderStroke(0.5.dp, color.copy(alpha = 0.35f)),
        modifier = Modifier.padding(vertical = 1.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
