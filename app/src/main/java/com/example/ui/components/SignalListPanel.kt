package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScannedDeviceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SignalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalListPanel(
    viewModel: SignalViewModel,
    modifier: Modifier = Modifier
) {
    val filteredList = viewModel.filteredActiveDevices()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("signal_list_panel")
    ) {
        // Headline
        Text(
            text = "قائمة الإشارات اللاسلكية الكاشفة",
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )

        // Glass Search Card
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("ابحث بالاسم، الماك المخصص، أو المصنع...", color = CyberGray, fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = CyberCyan) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = CyberCyan,
                unfocusedBorderColor = GlassBorder,
                focusedContainerColor = Color(0xFF101422),
                unfocusedContainerColor = Color(0xFF0F121C)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("device_search_input")
        )

        // Filtration filter buttons row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterTabButton(
                title = "الكل",
                selected = viewModel.filterType == "ALL",
                onClick = { viewModel.filterType = "ALL" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                title = "Wi-Fi",
                selected = viewModel.filterType == "WIFI",
                onClick = { viewModel.filterType = "WIFI" },
                modifier = Modifier.weight(1f)
            )
            FilterTabButton(
                title = "BT/BLE",
                selected = viewModel.filterType == "BLUETOOTH",
                onClick = { viewModel.filterType = "BLUETOOTH" },
                modifier = Modifier.weight(1.1f)
            )
            FilterTabButton(
                title = "Subnet LAN",
                selected = viewModel.filterType == "LAN",
                onClick = { viewModel.filterType = "LAN" },
                modifier = Modifier.weight(1.3f)
            )
        }

        // Active scanning indicators
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "عدد الإشارات المطابقة: ${filteredList.size}",
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            if (viewModel.isScanning) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        color = CyberCyan,
                        strokeWidth = 1.5.dp,
                        modifier = Modifier.size(10.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "جاري الفحص النشط...",
                        color = SignalExcellent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Text(
                    text = "منقطع حالياً",
                    color = SignalPoor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // List rendering
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = CyberGray.copy(alpha = 0.5f),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "لم نجد أي إشارة تطابق بحثك حالياً.",
                        color = CyberGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f)
            ) {
                items(filteredList, key = { it.macAddress }) { device ->
                    SignalCard(
                        device = device,
                        onClick = { viewModel.selectedDevice = device }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp)) // Floating nav bottom space
                }
            }
        }
    }
}

@Composable
fun FilterTabButton(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (selected) CyberCyan.copy(alpha = 0.15f) else Color.Transparent,
        border = BorderStroke(
            1.dp,
            if (selected) CyberCyan else GlassBorder
        ),
        modifier = modifier.height(36.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = title,
                color = if (selected) CyberCyan else CyberGray,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
