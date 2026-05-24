package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.ScannedDeviceEntity
import com.example.ui.theme.*
import kotlin.math.*

@Composable
fun RadarScanner(
    modifier: Modifier = Modifier,
    devices: List<ScannedDeviceEntity>,
    selectedDevice: ScannedDeviceEntity?,
    onDeviceSelected: (ScannedDeviceEntity) -> Unit
) {
    // Infinite rotation for the radar sweep sweep-line
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepRotation"
    )

    // Pulsing circles animation
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, delayMillis = 1500, easing = EaseOutQuad),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse2"
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0F172A), CyberBg),
                    radius = 500f
                )
            )
            .testTag("radar_canvas_container"),
        contentAlignment = Alignment.Center
    ) {
        // We capture positions at composition level to map click detection
        var canvasSize by remember { mutableStateOf(Offset.Zero) }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .pointerInput(devices) {
                    detectTapGestures { tapOffset ->
                        val centerX = canvasSize.x / 2
                        val centerY = canvasSize.y / 2
                        val maxRadius = min(centerX, centerY) * 0.9f

                        var closestDevice: ScannedDeviceEntity? = null
                        var minDistance = Float.MAX_VALUE

                        devices.forEach { dev ->
                            val angleHash = (dev.macAddress.hashCode() and 0x7FFFFFFF) % 360
                            val angleRad = Math.toRadians(angleHash.toDouble())

                            // Distance mapping normalized
                            val distanceFactor = 0.15f + (dev.distance.coerceIn(1.0f, 40.0f) / 40.0f) * 0.75f
                            val devRadius = distanceFactor * maxRadius

                            val devX = (centerX + devRadius * cos(angleRad)).toFloat()
                            val devY = (centerY + devRadius * sin(angleRad)).toFloat()

                            val dist = sqrt((tapOffset.x - devX).pow(2) + (tapOffset.y - devY).pow(2))
                            if (dist < 40f && dist < minDistance) { // Tap sensitivity threshold (40 pixels)
                                closestDevice = dev
                                minDistance = dist
                            }
                        }

                        closestDevice?.let { onDeviceSelected(it) }
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            canvasSize = Offset(width, height)

            val cx = width / 2
            val cy = height / 2
            val radius = min(cx, cy) * 0.9f

            // 1. Draw glowing faint background circles (Pulsing effects)
            drawCircle(
                color = CyberCyan.copy(alpha = 0.12f * (1f - pulseScale1)),
                radius = radius * pulseScale1,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = CyberPurple.copy(alpha = 0.1f * (1f - pulseScale2)),
                radius = radius * pulseScale2,
                center = Offset(cx, cy),
                style = Stroke(width = 2.dp.toPx())
            )

            // 2. Draw static tactical grids
            val dailsCount = 4
            for (i in 1..dailsCount) {
                drawCircle(
                    color = CyberCyan.copy(alpha = 0.25f),
                    radius = radius * (i.toFloat() / dailsCount),
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            // 3. Draw crosshairs
            drawLine(
                color = CyberCyan.copy(alpha = 0.25f),
                start = Offset(cx - radius, cy),
                end = Offset(cx + radius, cy),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = CyberCyan.copy(alpha = 0.25f),
                start = Offset(cx, cy - radius),
                end = Offset(cx, cy + radius),
                strokeWidth = 1.dp.toPx()
            )

            // 4. Draw Rotating Radar Sweep Gradient line
            val sweepEndX = (cx + radius * cos(Math.toRadians(rotationAngle.toDouble()))).toFloat()
            val sweepEndY = (cy + radius * sin(Math.toRadians(rotationAngle.toDouble()))).toFloat()

            // Sweep visual trail
            drawArc(
                brush = Brush.sweepGradient(
                    center = Offset(cx, cy),
                    colors = listOf(
                        CyberCyan.copy(alpha = 0.4f),
                        CyberCyan.copy(alpha = 0.05f),
                        Color.Transparent,
                        Color.Transparent
                    ),
                ),
                startAngle = rotationAngle - 45f,
                sweepAngle = 45f,
                useCenter = true
            )

            // Dynamic sweep leading line
            drawLine(
                color = CyberCyan,
                start = Offset(cx, cy),
                end = Offset(sweepEndX, sweepEndY),
                strokeWidth = 1.5.dp.toPx()
            )

            // 5. Draw Devices (Blips)
            devices.forEach { dev ->
                val angleHash = (dev.macAddress.hashCode() and 0x7FFFFFFF) % 360
                val angleRad = Math.toRadians(angleHash.toDouble())

                // Calculate positions
                val distanceFactor = 0.15f + (dev.distance.coerceIn(1.0f, 40.0f) / 40.0f) * 0.75f
                val devRadius = distanceFactor * radius

                val devX = (cx + devRadius * cos(angleRad)).toFloat()
                val devY = (cy + devRadius * sin(angleRad)).toFloat()

                // Blip color depending on status
                val blipColor = when {
                    dev.signalType == "WIFI" -> CyberCyan
                    dev.signalType == "BLUETOOTH" -> CyberPurple
                    else -> CyberAccent
                }

                // Blink intensity based on RSSI strength
                val rssiProgress = ((dev.rssi + 100).coerceIn(0, 70) / 70f)
                val blipSize = (8.dp.toPx() + (rssiProgress * 6.dp.toPx()))

                val isSelected = selectedDevice?.macAddress == dev.macAddress

                // Draw outer glowing ring for selected devices
                if (isSelected) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.5f),
                        radius = blipSize + 8.dp.toPx(),
                        center = Offset(devX, devY),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    drawCircle(
                        color = blipColor.copy(alpha = 0.3f),
                        radius = blipSize + 16.dp.toPx(),
                        center = Offset(devX, devY)
                    )
                }

                // Outer signal halo
                drawCircle(
                    color = blipColor.copy(alpha = 0.35f),
                    radius = blipSize + 4.dp.toPx(),
                    center = Offset(devX, devY),
                    style = Stroke(width = 1.dp.toPx())
                )

                // Core blip
                drawCircle(
                    color = blipColor,
                    radius = blipSize,
                    center = Offset(devX, devY)
                )

                // White small center
                drawCircle(
                    color = Color.White,
                    radius = blipSize / 3,
                    center = Offset(devX, devY)
                )
            }
        }

        // Overlay status indicators for North/East/South/West directions
        Text(
            text = "000° N",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
        )

        Text(
            text = "180° S",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )

        Text(
            text = "090° E",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp)
        )

        Text(
            text = "270° W",
            color = CyberCyan,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
    }
}
