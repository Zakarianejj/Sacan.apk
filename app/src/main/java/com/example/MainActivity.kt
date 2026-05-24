package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.ui.components.AIPanel
import com.example.ui.components.SavedSessionsPanel
import com.example.ui.components.SignalListPanel
import com.example.ui.components.StatusDashboard
import com.example.ui.theme.*
import com.example.ui.viewmodel.SignalViewModel

class MainActivity : ComponentActivity() {
    
    private fun checkDevicePermissions(): Boolean {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        
        val hasBtScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        return hasFine && hasCoarse && hasBtScan
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val viewModel = ViewModelProvider(this)[SignalViewModel::class.java]
        viewModel.hasPermissions = checkDevicePermissions()

        setContent {
            MyApplicationTheme {
                MainLayout(viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        try {
            val viewModel = ViewModelProvider(this)[SignalViewModel::class.java]
            viewModel.hasPermissions = checkDevicePermissions()
        } catch (e: Exception) {
            // Safe fallback
        }
    }
}

@Composable
fun MainLayout(viewModel: SignalViewModel) {
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permMap ->
        val fineLocationGranted = permMap[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permMap[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        
        viewModel.hasPermissions = fineLocationGranted || coarseLocationGranted
    }

    val requestPermissionsAction = {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
        launcher.launch(permissions)
    }

    // Automatically prompt permissions on first startup so the system registers active scanning instantly
    LaunchedEffect(Unit) {
        requestPermissionsAction()
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("app_main_scaffold"),
        containerColor = CyberBg,
        bottomBar = {
            BottomCyberNav(
                currentTab = viewModel.currentTab,
                onTabSelected = { viewModel.currentTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()) // Account for bottom bar padding manually
                .statusBarsPadding() // Keep app content below notification camera notch
        ) {
            when (viewModel.currentTab) {
                0 -> StatusDashboard(
                    viewModel = viewModel, 
                    onRequestPermissions = requestPermissionsAction
                )
                1 -> SignalListPanel(viewModel = viewModel)
                2 -> AIPanel(viewModel = viewModel)
                3 -> SavedSessionsPanel(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun BottomCyberNav(
    currentTab: Int,
    onTabSelected: (Int) -> Unit
) {
    // Elegant translucent bottom row
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding(), // CRITICAL notch/gesture safe area override
        color = Color(0xDC0A0E18), // High contrast translucent slate
        border = BorderStroke(1.dp, Brush.horizontalGradient(listOf(GlassBorder, Color.Transparent))),
        tonalElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavTabItem(
                label = "الرادار",
                icon = Icons.Default.Home,
                selected = currentTab == 0,
                onClick = { onTabSelected(0) },
                modifier = Modifier.testTag("tab_radar")
            )

            NavTabItem(
                label = "الترددات",
                icon = Icons.Default.List,
                selected = currentTab == 1,
                onClick = { onTabSelected(1) },
                modifier = Modifier.testTag("tab_hunter")
            )

            NavTabItem(
                label = "استشارتي AI",
                icon = Icons.Default.Build,
                selected = currentTab == 2,
                onClick = { onTabSelected(2) },
                modifier = Modifier.testTag("tab_ai")
            )

            NavTabItem(
                label = "السجلات",
                icon = Icons.Default.History,
                selected = currentTab == 3,
                onClick = { onTabSelected(3) },
                modifier = Modifier.testTag("tab_records")
            )
        }
    }
}

@Composable
fun NavTabItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (selected) CyberCyan else CyberGray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = if (selected) CyberCyan else CyberGray,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
