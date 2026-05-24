package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScannedDeviceEntity
import com.example.data.db.SignalSessionEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SignalViewModel

@Composable
fun SavedSessionsPanel(
    viewModel: SignalViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.savedSessions.collectAsState()
    val selectedSession = viewModel.selectedSession

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("saved_sessions_panel")
    ) {
        if (selectedSession == null) {
            // Display history list
            Text(
                text = "سجل فحوصات الإشارة المحفوظة",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = CyberGray.copy(alpha = 0.4f),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "لا توجد لقطات محفوظة حالياً.",
                            color = CyberGray,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "احفظ اللقطات من لوحة التحكم لمراجعتها وتحليلها من جديد.",
                            color = CyberGrayDark,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp, start = 20.dp, end = 20.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        SessionItemCard(
                            session = session,
                            onClick = { viewModel.loadSessionDetails(session) },
                            onDelete = { viewModel.deleteSession(session.id) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        } else {
            // Display detailed device records for the loaded session
            SessionDetailsView(
                session = selectedSession,
                devices = viewModel.selectedSessionDevices,
                onBack = { viewModel.selectedSession = null },
                onDelete = {
                    viewModel.deleteSession(selectedSession.id)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionItemCard(
    session: SignalSessionEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val statusColor = when {
        session.avgRssi >= -50 -> SignalExcellent
        session.avgRssi >= -70 -> SignalGood
        else -> SignalPoor
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101424)),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.formattedDate,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "الأجهزة: ${session.deviceCount} أجهزة",
                        color = CyberCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "معدل RSSI: ${session.avgRssi} dBm",
                        color = statusColor,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!session.aiAssessment.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(12.dp))
                        Text(text = "يحتوي على تشخيص ذكي", color = CyberPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Record",
                    tint = SignalPoor.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
fun SessionDetailsView(
    session: SignalSessionEntity,
    devices: List<ScannedDeviceEntity>,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Navbar header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = CyberCyan)
            }

            Text(
                text = "تفاصيل اللقطة المخزنة",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            IconButton(onClick = {
                onDelete()
                onBack()
            }) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = SignalPoor)
            }
        }

        // Summary details card
        Card(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14192D)),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.3f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "تقرير الفحص", color = CyberCyan, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(text = session.formattedDate, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(text = "إجمالي الترددات", color = CyberGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${session.deviceCount} أجهزة", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(text = "معدل قوة الإرسال", color = CyberGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Text(text = "${session.avgRssi} dBm", color = SignalExcellent, fontSize = 14.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // AI assessment panel if present
        if (!session.aiAssessment.isNullOrBlank()) {
            Text(
                text = "التشخيص الذكي المحفوظ:",
                color = CyberPurple,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0C0F1A)),
                border = BorderStroke(1.dp, CyberPurple.copy(alpha = 0.4f))
            ) {
                Text(
                    text = session.aiAssessment,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }

        Text(
            text = "قائمة الأجهزة المسجلة (${devices.size}):",
            color = CyberGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices, key = { it.id }) { device ->
                SavedDeviceItem(device = device)
            }
            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun SavedDeviceItem(device: ScannedDeviceEntity) {
    val signalColor = when {
        device.rssi >= -50 -> SignalExcellent
        device.rssi >= -65 -> SignalGood
        device.rssi >= -80 -> SignalFair
        else -> SignalPoor
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, GlassBorder),
        color = Color(0xFF0F121C),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(6.dp),
                color = signalColor.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    val icon = if (device.signalType == "WIFI") Icons.Default.Wifi else Icons.Default.Bluetooth
                    Icon(imageVector = icon, contentDescription = null, tint = signalColor, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = device.name.ifEmpty { "تردد غير مسمى" }, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Text(text = "${device.macAddress} • ${device.manufacturer}", color = CyberGray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${device.rssi} dBm", color = signalColor, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                Text(text = device.frequency, color = CyberGray, fontSize = 9.sp)
            }
        }
    }
}
