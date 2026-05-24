package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "signal_sessions")
data class SignalSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val deviceCount: Int,
    val avgRssi: Int,
    val aiAssessment: String? = null
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
}

@Entity(tableName = "scanned_devices")
data class ScannedDeviceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: Int, // Refers to SignalSessionEntity.id
    val name: String,
    val macAddress: String,
    val signalType: String, // "WIFI", "BLUETOOTH", "LAN"
    val rssi: Int, // Signal strength in dBm
    val frequency: String, // e.g. "2.4 GHz", "5 GHz", "BLE 2.4GHz"
    val channel: String, // e.g. "CH 6", "N/A"
    val security: String, // "WPA2 Enterprise", "WPA3", "WPA2 Personal", "OPEN", "SECURE"
    val manufacturer: String,
    val distance: Float // Estimated distance in meters
)
