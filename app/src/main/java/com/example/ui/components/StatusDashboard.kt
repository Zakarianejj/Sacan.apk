package com.example.ui.components

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScannedDeviceEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.SignalViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun StatusDashboard(
    viewModel: SignalViewModel,
    modifier: Modifier = Modifier,
    onRequestPermissions: () -> Unit = {}
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val activeDevices = viewModel.activeDevices
    val selectedDevice = viewModel.selectedDevice

    // Active statistics
    val totalCount = activeDevices.size
    val wifiCount = activeDevices.count { it.signalType == "WIFI" }
    val btCount = activeDevices.count { it.signalType == "BLUETOOTH" }
    val lanCount = activeDevices.count { it.signalType == "LAN" }

    val averageRssi = if (activeDevices.isNotEmpty()) {
        activeDevices.map { it.rssi }.average().roundToInt()
    } else -100

    val strongestDevice = activeDevices.maxByOrNull { it.rssi }

    // Fake historic values for the real-time line chart
    val rssiHistory = remember { mutableStateListOf<Float>() }
    
    // Cycle line graph points
    LaunchedEffect(activeDevices) {
        if (activeDevices.isNotEmpty()) {
            rssiHistory.add(averageRssi.toFloat())
            if (rssiHistory.size > 15) {
                rssiHistory.removeAt(0)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App header and tagline
        Text(
            text = "رادار الترددات النشط",
            color = Color.White,
            fontWeight = FontWeight.Black,
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = "SIGNAL HUNTER TELEMETRY RADAR v1.0",
            color = CyberCyan,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Dynamic physical hardware/simulation permission alert banner
        if (!viewModel.hasPermissions) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x33FF0055)),
                border = BorderStroke(1.dp, SignalPoor)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📡 يعمل التطبيق حالياً في وضع محاكاة الإشارات فقط.",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "اضغط على الزر أدناه للسماح بالوصول للموقع والراديو وبدء مسح الأجهزة الحقيقية المحيطة بك.",
                        color = CyberGray,
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermissions,
                        colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = "السماح بمسح الرادار الحقيقي",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        } else {
            // Connected/Live success badge
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0x2200FF7F)),
                border = BorderStroke(1.dp, SignalExcellent.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active scanning",
                        tint = SignalExcellent,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "المسح الطيفي الميداني الحقيقي نشط ومتصل بالراديو 📡",
                        color = SignalExcellent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // The Radar Canvas
        RadarScanner(
            modifier = Modifier
                .size(290.dp)
                .padding(vertical = 8.dp),
            devices = activeDevices,
            selectedDevice = selectedDevice,
            onDeviceSelected = { viewModel.selectedDevice = it }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Scanner Quick Stats
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCompactCard(
                title = "قوة الإشارة (معدل)",
                value = "$averageRssi dBm",
                subText = when {
                    averageRssi >= -50 -> "قوية جداً"
                    averageRssi >= -70 -> "مستقرة"
                    else -> "مزدحمة"
                },
                accentColor = when {
                    averageRssi >= -50 -> SignalExcellent
                    averageRssi >= -70 -> SignalGood
                    else -> SignalPoor
                },
                modifier = Modifier.weight(1f)
            )

            StatsCompactCard(
                title = "الأجهزة الكلية",
                value = "$totalCount أجهزة",
                subText = "W:$wifiCount | B:$btCount | L:$lanCount",
                accentColor = CyberCyan,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Strongest antenna stat card
        strongestDevice?.let { dev ->
            StatsFullWidthCard(
                title = "أقوى بث مكتشف",
                name = dev.name.ifEmpty { "إشارة بدون اسم" },
                rssi = dev.rssi,
                frequency = dev.frequency,
                mac = dev.macAddress,
                vendor = dev.manufacturer,
                icon = Icons.Default.NetworkCheck,
                onClick = { viewModel.selectedDevice = dev }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Dynamic Bezier Line Graph Tracker
        Text(
            text = "تذبذب الإشارة الفعلي (معدل RSSI الحالي)",
            color = CyberGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp)
        )
        LiveSignalGraph(
            history = rssiHistory,
            modifier = Modifier
                .fillMaxWidth()
                .height(90.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Scanning state controls row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start / Pause
            Button(
                onClick = {
                    if (viewModel.isScanning) {
                        viewModel.pauseScanning()
                    } else {
                        viewModel.startScanning()
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (viewModel.isScanning) Color(0x33FF0055) else Color(0x3300FF66)
                ),
                border = BorderStroke(1.dp, if (viewModel.isScanning) SignalPoor else SignalExcellent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("scan_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isScanning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (viewModel.isScanning) SignalPoor else SignalExcellent
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (viewModel.isScanning) "إيقاف الرصد" else "بدء الرصد",
                    color = if (viewModel.isScanning) SignalPoor else SignalExcellent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Save Snapshot
            Button(
                onClick = {
                    viewModel.saveCurrentSession()
                    Toast.makeText(context, "تم حفظ الجلسة بنجاح في السجلات المحلية!", Toast.LENGTH_SHORT).show()
                },
                enabled = activeDevices.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0x22BD00FF),
                    disabledContainerColor = Color.DarkGray.copy(alpha = 0.2f)
                ),
                border = BorderStroke(1.dp, if (activeDevices.isNotEmpty()) CyberPurple else Color.Gray),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("save_session_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = null,
                    tint = if (activeDevices.isNotEmpty()) CyberPurple else Color.Gray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "حفظ اللقطة لـ AI",
                    color = if (activeDevices.isNotEmpty()) Color.White else Color.Gray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Export button CSV
        OutlinedButton(
            onClick = {
                val csv = viewModel.generateCSVExport(activeDevices)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Signal Hunter Export")
                    putExtra(Intent.EXTRA_TEXT, csv)
                }
                context.startActivity(Intent.createChooser(shareIntent, "تحميل ومشاركة تقرير الترددات"))
            },
            enabled = activeDevices.isNotEmpty(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = CyberCyan
            ),
            border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.7f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("export_csv_button")
        ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = CyberCyan)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "تصدير التقرير الفني المباشر (CSV)",
                color = CyberCyan,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        Spacer(modifier = Modifier.height(80.dp)) // Nav bar breathing room
    }

    // Modal popup sheet when user selects any specific Radar node
    selectedDevice?.let { dev ->
        AlertDialog(
            onDismissRequest = { viewModel.selectedDevice = null },
            containerColor = CyberSurfaceVariant,
            shape = RoundedCornerShape(22.dp),
            tonalElevation = 8.dp,
            icon = {
                val statusColor = when {
                    dev.rssi >= -50 -> SignalExcellent
                    dev.rssi >= -65 -> SignalGood
                    dev.rssi >= -80 -> SignalFair
                    else -> SignalPoor
                }
                Icon(
                    imageVector = if (dev.signalType == "WIFI") Icons.Default.Wifi else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(40.dp)
                )
            },
            title = {
                Text(
                    text = dev.name.ifEmpty { "تردد مجهول الهوية" },
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailTextRow("معرف العنوان MAC :", dev.macAddress)
                    DetailTextRow("نوع الشبكة :", dev.signalType)
                    DetailTextRow("مستوى الإرسال :", "${dev.rssi} dBm")
                    DetailTextRow("الشركة المصنعة :", dev.manufacturer)
                    DetailTextRow("النطاق والتردد :", dev.frequency)
                    if (dev.channel != "N/A" && dev.channel.isNotEmpty()) {
                        DetailTextRow("قناة البث :", dev.channel)
                    }
                    DetailTextRow("حماية التشفير :", dev.security)
                    DetailTextRow("المسافة التقريبية :", "~ ${String.format("%.1f", dev.distance)} متر")
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.selectedDevice = null },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("إغلاق لوحة التحكم", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
fun DetailTextRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = value, color = CyberCyan, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = CyberGray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StatsCompactCard(
    title: String,
    value: String,
    subText: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(105.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF101422)),
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(GlassBorder, accentColor.copy(alpha = 0.3f))))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = title, color = CyberGray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(
                text = value,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp
            )
            Text(text = subText, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsFullWidthCard(
    title: String,
    name: String,
    rssi: Int,
    frequency: String,
    mac: String,
    vendor: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val signalColor = when {
        rssi >= -50 -> SignalExcellent
        rssi >= -65 -> SignalGood
        rssi >= -80 -> SignalFair
        else -> SignalPoor
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111625)),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(10.dp),
                color = signalColor.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = icon, contentDescription = null, tint = signalColor, modifier = Modifier.size(22.dp))
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, color = CyberCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = "$mac • $vendor", color = CyberGray, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "$rssi dBm", color = signalColor, fontFamily = FontFamily.Monospace, fontSize = 15.sp, fontWeight = FontWeight.Black)
                Text(text = frequency, color = CyberGray, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun LiveSignalGraph(
    history: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .background(Color(0xFF0C0F18), RoundedCornerShape(12.dp))
    ) {
        val width = size.width
        val height = size.height

        if (history.size < 2) return@Canvas

        val maxPoints = 15
        val xInterval = width / (maxPoints - 1)

        val yMin = -100f
        val yMax = -30f
        val yRange = yMax - yMin

        val path = Path()
        val fillPath = Path()

        history.forEachIndexed { index, value ->
            // Normalize graph values (clamped between -30 and -100)
            val normalizedVal = (value.coerceIn(yMin, yMax) - yMin) / yRange
            // Invert coordinate systems since top is 0 in Android Canvas
            val y = height - (normalizedVal * height)
            val x = index * xInterval

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }

            if (index == history.lastIndex) {
                fillPath.lineTo(x, height)
                fillPath.close()
            }
        }

        // Draw neon fill gradient
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(CyberPurple.copy(alpha = 0.3f), Color.Transparent)
            )
        )

        // Draw neon stroke line
        drawPath(
            path = path,
            color = CyberCyan,
            style = Stroke(width = 2.dp.toPx())
        )

        // Highlight head point with glowing bubble
        val lastVal = history.last()
        val lastNorm = (lastVal.coerceIn(yMin, yMax) - yMin) / yRange
        val lastX = (history.size - 1) * xInterval
        val lastY = height - (lastNorm * height)

        drawCircle(
            color = CyberCyan.copy(alpha = 0.4f),
            radius = 7.dp.toPx(),
            center = Offset(lastX, lastY)
        )
        drawCircle(
            color = Color.White,
            radius = 3.dp.toPx(),
            center = Offset(lastX, lastY)
        )
    }
}
