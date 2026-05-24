package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.ScannedDeviceEntity
import com.example.data.db.SignalSessionEntity
import com.example.data.repository.SignalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed interface AIState {
    object Idle : AIState
    object Loading : AIState
    data class Success(val advice: String) : AIState
    data class Error(val message: String) : AIState
}

class SignalViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SignalRepository(application)

    // Scanning States
    var isScanning by mutableStateOf(true)
        private set

    var hasPermissions by mutableStateOf(false)

    var activeDevices by mutableStateOf<List<ScannedDeviceEntity>>(emptyList())
        private set

    var selectedDevice by mutableStateOf<ScannedDeviceEntity?>(null)

    // Filtration
    var filterType by mutableStateOf("ALL") // "ALL", "WIFI", "BLUETOOTH", "LAN"
    var searchQuery by mutableStateOf("")

    // Tab switcher
    var currentTab by mutableStateOf(0) // 0: Dashboard, 1: Signal Hunter, 2: AI Diagnostic, 3: Scan Records

    // Custom API Key Override (Preferences stored)
    private val prefs = application.getSharedPreferences("signal_hunter_prefs", Context.MODE_PRIVATE)
    var apiKeyOverride by mutableStateOf(prefs.getString("gemini_key", "") ?: "")
        private set

    // Saved Sessions (Room state)
    val savedSessions: StateFlow<List<SignalSessionEntity>> = repository.allSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var selectedSession by mutableStateOf<SignalSessionEntity?>(null)
    var selectedSessionDevices by mutableStateOf<List<ScannedDeviceEntity>>(emptyList())
        private set

    // AI Diagnostics State
    var aiState by mutableStateOf<AIState>(AIState.Idle)
        private set

    private var scanJob: Job? = null

    init {
        startScanning()
    }

    fun startScanning() {
        isScanning = true
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            repository.activeSignalStream(isSimulationOnly = false).collectLatest { devices ->
                if (isScanning) {
                    activeDevices = devices
                }
            }
        }
    }

    fun pauseScanning() {
        isScanning = false
        scanJob?.cancel()
    }

    fun setCustomApiKey(key: String) {
        apiKeyOverride = key
        prefs.edit().putString("gemini_key", key).apply()
    }

    fun requestAIDiagnostics() {
        if (activeDevices.isEmpty()) {
            aiState = AIState.Error("لا توجد إشارات نشطة لتحليلها حالياً.")
            return
        }

        aiState = AIState.Loading
        viewModelScope.launch {
            try {
                val advice = repository.analyzeSignalsWithAI(apiKeyOverride, activeDevices)
                aiState = AIState.Success(advice)
            } catch (e: Exception) {
                aiState = AIState.Error(e.message ?: "حدث خطأ غير معروف.")
            }
        }
    }

    fun resetAIState() {
        aiState = AIState.Idle
    }

    fun setCustomAIState(state: AIState) {
        aiState = state
    }

    fun saveCurrentSession() {
        if (activeDevices.isEmpty()) return

        viewModelScope.launch {
            val avgRssi = activeDevices.map { it.rssi }.average().roundToInt()
            val totalDevices = activeDevices.size

            val cachedAiAdvice = when (val currentUiState = aiState) {
                is AIState.Success -> currentUiState.advice
                else -> null
            }

            val session = SignalSessionEntity(
                deviceCount = totalDevices,
                avgRssi = avgRssi,
                aiAssessment = cachedAiAdvice
            )
            repository.saveSession(session, activeDevices)
        }
    }

    fun deleteSession(sessionId: Int) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (selectedSession?.id == sessionId) {
                selectedSession = null
                selectedSessionDevices = emptyList()
            }
        }
    }

    fun loadSessionDetails(session: SignalSessionEntity) {
        selectedSession = session
        viewModelScope.launch {
            selectedSessionDevices = repository.getDevicesForSession(session.id)
        }
    }

    fun generateCSVExport(devices: List<ScannedDeviceEntity>): String {
        val sb = StringBuilder()
        sb.append("Device Name,MAC Address,Type,RSSI (dBm),Frequency,Channel,Security,Manufacturer,Distance (m)\n")
        devices.forEach { d ->
            sb.append("\"${d.name}\",\"${d.macAddress}\",\"${d.signalType}\",${d.rssi},\"${d.frequency}\",\"${d.channel}\",\"${d.security}\",\"${d.manufacturer}\",${d.distance}\n")
        }
        return sb.toString()
    }

    fun filteredActiveDevices(): List<ScannedDeviceEntity> {
        return activeDevices.filter { device ->
            val matchesType = filterType == "ALL" || device.signalType == filterType
            val matchesSearch = searchQuery.isEmpty() ||
                    device.name.contains(searchQuery, ignoreCase = true) ||
                    device.macAddress.contains(searchQuery, ignoreCase = true) ||
                    device.manufacturer.contains(searchQuery, ignoreCase = true)
            matchesType && matchesSearch
        }
    }
}
