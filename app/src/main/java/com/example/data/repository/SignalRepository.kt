package com.example.data.repository

import android.content.Context
import android.net.wifi.WifiManager
import android.net.ConnectivityManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.data.api.Content
import com.example.data.api.GenerateContentRequest
import com.example.data.api.Part
import com.example.data.api.RetrofitClient
import com.example.data.db.AppDatabase
import com.example.data.db.ScannedDeviceEntity
import com.example.data.db.SignalSessionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.random.Random

class SignalRepository(private val context: Context) {

    private val db = AppDatabase.getDatabase(context)
    private val signalDao = db.signalDao()

    val allSessions: Flow<List<SignalSessionEntity>> = signalDao.getAllSessions()

    suspend fun getDevicesForSession(sessionId: Int): List<ScannedDeviceEntity> {
        return signalDao.getDevicesForSession(sessionId)
    }

    suspend fun saveSession(session: SignalSessionEntity, devices: List<ScannedDeviceEntity>): Long {
        return withContext(Dispatchers.IO) {
            val id = signalDao.insertSession(session)
            val updatedDevices = devices.map { it.copy(sessionId = id.toInt()) }
            signalDao.insertDevices(updatedDevices)
            id
        }
    }

    suspend fun deleteSession(sessionId: Int) {
        withContext(Dispatchers.IO) {
            signalDao.deleteSessionWithDevices(sessionId)
        }
    }

    /**
     * Call Gemini API to diagnose current signal parameters and provide expert security/performance analysis.
     */
    suspend fun analyzeSignalsWithAI(
        apiKeyOverride: String?,
        devices: List<ScannedDeviceEntity>
    ): String = withContext(Dispatchers.IO) {
        val key = if (!apiKeyOverride.isNullOrEmpty()) apiKeyOverride else BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            return@withContext "Error: Gemini API Key is missing. Please enter your API Key in the settings or secrets panel."
        }

        val dfText = templatePromptText(devices)
        val request = GenerateContentRequest(
            contents = listOf(
                Content(parts = listOf(Part(text = dfText)))
            ),
            systemInstruction = Content(parts = listOf(Part(text = "You are Signal Hunter AI, an elite telecommunications, radio frequency, and cybersecurity expert. Provide structured, clean, impressive analysis. Speak in Arabic as requested by the user, and use English terms where appropriate.")))
        )

        try {
            val response = RetrofitClient.service.generateContent(key, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "لم نتمكن من الحصول على رد من الذكاء الاصطناعي. الرجاء المحاولة مرة أخرى."
        } catch (e: Exception) {
            Log.e("SignalRepository", "Gemini API error", e)
            "حدث خطأ أثناء إجراء التحليل: ${e.message}\nتأكد من صحة مفتاح الـ API والاتصال بالإنترنت."
        }
    }

    private fun templatePromptText(devices: List<ScannedDeviceEntity>): String {
        val sb = StringBuilder()
        sb.append("Please analyze the following list of active radio signals detected around the user device:\n\n")
        devices.forEachIndexed { index, device ->
            sb.append("${index + 1}. [${device.signalType}] Name: ${device.name}, MAC: ${device.macAddress}, RSSI: ${device.rssi} dBm, Freq/Band: ${device.frequency}, Channel: ${device.channel}, Security: ${device.security}, Mfg: ${device.manufacturer}, Est. Distance: ${String.format(Locale.US, "%.1f", device.distance)}m\n")
        }
        sb.append("\nTasks:\n")
        sb.append("1. Evaluate the overall signal landscape (Wi-Fi channel saturation, Bluetooth abundance, security vulnerabilities).\n")
        sb.append("2. Identify primary network intersections or channel conflicts (e.g. multiple 2.4 GHz APs on identical channels).\n")
        sb.append("3. Provide 3 specific, highly technical suggestions to optimize signal strength, prevent interference, and harden security.\n")
        sb.append("4. Suggest the ideal physical positioning for a strong connection (e.g., distance estimate, obstacle attenuation tips).\n")
        sb.append("Format the response using clean markdown in Arabic language, utilizing professional, modern telemetry terminology.")
        return sb.toString()
    }

    /**
     * Actively scans and produces simulated real-time updates.
     * Integrates hardware parameters where possible.
     */
    fun activeSignalStream(isSimulationOnly: Boolean): Flow<List<ScannedDeviceEntity>> = flow {
        // Prepare list of mock base stations that dynamically update as a fallback and extra flavor
        val mockDevices = generateStaticMockDevices().toMutableList()

        while (true) {
            val currentDeviceList = mutableListOf<ScannedDeviceEntity>()

            if (!isSimulationOnly) {
                // If native scanning can be loaded, we supplement it.
                // 1) Wifi scan details
                try {
                    val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                    if (wifiManager != null) {
                        // Gather connection details first as a high-accuracy reference
                        val info = wifiManager.connectionInfo
                        val connectedBssid = info?.bssid?.uppercase() ?: ""

                        // Real Wi-Fi scan results from surrounding access points
                        val results = wifiManager.scanResults
                        if (!results.isNullOrEmpty()) {
                            results.forEach { scanResult ->
                                val bssid = scanResult.BSSID ?: "00:00:00:00:00:00"
                                val rawSsid = scanResult.SSID ?: ""
                                val ssid = if (rawSsid.isEmpty() || rawSsid == "<unknown ssid>") {
                                    "Hidden Network"
                                } else {
                                    rawSsid.replace("\"", "")
                                }
                                
                                val rssi = scanResult.level
                                val freq = scanResult.frequency
                                val bandDesc = if (freq > 4900) "5 GHz" else "2.4 GHz"
                                val channel = freqToChannel(freq)
                                val securityDesc = scanResult.capabilities ?: "WPA2"
                                
                                val device = ScannedDeviceEntity(
                                    sessionId = 0,
                                    name = ssid,
                                    macAddress = bssid.uppercase(),
                                    signalType = "WIFI",
                                    rssi = rssi,
                                    frequency = bandDesc,
                                    channel = "CH $channel",
                                    security = securityDesc,
                                    manufacturer = getVendorFromMac(bssid),
                                    distance = calculateDistanceMeters(rssi, freq.toFloat())
                                )
                                // Avoid duplicate connected access points
                                if (device.macAddress != connectedBssid) {
                                    currentDeviceList.add(device)
                                }
                            }
                        }

                        // Add the currently active connected Wi-Fi
                        if (info != null && info.networkId != -1) {
                            val bssid = info.bssid ?: "00:00:00:00:00:00"
                            val ssid = info.ssid?.replace("\"", "") ?: "Connected Wi-Fi"
                            val rssi = info.rssi
                            val freq = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) info.frequency else 2412
                            val bandDesc = if (freq > 4000) "5 GHz" else "2.4 GHz"
                            val channel = freqToChannel(freq)
                            
                            val alreadyInList = currentDeviceList.any { it.macAddress.equals(bssid, ignoreCase = true) }
                            if (!alreadyInList) {
                                val hardwareDevice = ScannedDeviceEntity(
                                    sessionId = 0,
                                    name = "$ssid (متصل)",
                                    macAddress = bssid.uppercase(),
                                    signalType = "WIFI",
                                    rssi = rssi,
                                    frequency = bandDesc,
                                    channel = "CH $channel",
                                    security = "WPA2/WPA3 (نشط)",
                                    manufacturer = getVendorFromMac(bssid),
                                    distance = calculateDistanceMeters(rssi, freq.toFloat())
                                )
                                currentDeviceList.add(hardwareDevice)
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w("SignalRepository", "Wifi SecurityException - location permission might be denied")
                } catch (e: Exception) {
                    Log.e("SignalRepository", "Wifi Scan error", e)
                }

                // 2) Bluetooth paired/bonded devices scan
                try {
                    val bluetoothManager = context.applicationContext.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                    val bluetoothAdapter = bluetoothManager?.adapter
                    if (bluetoothAdapter != null && bluetoothAdapter.isEnabled) {
                        val bondedDevices = bluetoothAdapter.bondedDevices
                        if (!bondedDevices.isNullOrEmpty()) {
                            bondedDevices.forEach { dev ->
                                val mac = dev.address ?: "00:00:00:00:00:00"
                                val name = dev.name ?: "Unknown Bluetooth Device"
                                
                                val alreadyInList = currentDeviceList.any { it.macAddress.equals(mac, ignoreCase = true) }
                                if (!alreadyInList) {
                                    val bluetoothDevice = ScannedDeviceEntity(
                                        sessionId = 0,
                                        name = "$name (مقترن)",
                                        macAddress = mac.uppercase(),
                                        signalType = "BLUETOOTH",
                                        rssi = -60, // Estimated pairing power strength
                                        frequency = "Bluetooth 2.4 GHz",
                                        channel = "N/A",
                                        security = "SECURE PAIRING",
                                        manufacturer = getVendorFromMac(mac),
                                        distance = 3.0f
                                    )
                                    currentDeviceList.add(bluetoothDevice)
                                }
                            }
                        }
                    }
                } catch (e: SecurityException) {
                    Log.w("SignalRepository", "Bluetooth SecurityException - bluetooth permission missing")
                } catch (e: Exception) {
                    Log.e("SignalRepository", "Bluetooth Scan error", e)
                }
            }

            // Fluctuations in static mock/simulated signals to maintain full screen density
            mockDevices.forEach { dev ->
                // Guard: if a real device has precisely the same MAC address, skip mock version to prevent duplicate keys
                if (currentDeviceList.any { it.macAddress.equals(dev.macAddress, ignoreCase = true) }) {
                    return@forEach
                }

                val fluctuation = Random.nextInt(-4, 5)
                val newRssi = (dev.rssi + fluctuation).coerceIn(-100, -30)
                
                val freqFloat = if (dev.frequency.contains("5 GHz")) 5180f else 2437f
                val newDistance = calculateDistanceMeters(newRssi, freqFloat)

                val updated = dev.copy(
                    rssi = newRssi,
                    distance = newDistance
                )
                currentDeviceList.add(updated)
            }

            // Sort by signal strength (strongest RSSI first)
            currentDeviceList.sortByDescending { it.rssi }

            emit(currentDeviceList)
            delay(2500) // Update frequency every 2.5 seconds
        }
    }.flowOn(Dispatchers.IO)

    // Helper functions for scanning maths
    private fun freqToChannel(freq: Int): Int {
        return when {
            freq == 2484 -> 14
            freq in 2412..2472 -> (freq - 2407) / 5
            freq in 5170..5825 -> (freq - 5000) / 5
            else -> 0
        }
    }

    private fun intToIp(ipAddress: Int): String {
        return String.format(
            Locale.US, "%d.%d.%d.%d",
            ipAddress and 0xff,
            ipAddress shr 8 and 0xff,
            ipAddress shr 16 and 0xff,
            ipAddress shr 24 and 0xff
        )
    }

    private fun calculateDistanceMeters(rssi: Int, freqMhz: Float): Float {
        // Free-space path loss formula
        // FSPL (dB) = 20 log10(d) + 20 log10(f) - 27.55
        // d = 10 ^ ((FSPL - 20 log10(f) + 27.55) / 20)
        val fspl = -rssi // Inverse logic approximation
        val exp = (fspl - (20 * log10(freqMhz.toDouble())) + 27.55) / 20.0
        val dist = 10.0.pow(exp)
        // Coerce to reasonable bounds
        return dist.coerceIn(0.5, 75.0).toFloat()
    }

    fun getVendorFromMac(mac: String): String {
        val uppercaseMac = mac.uppercase()
        return when {
            uppercaseMac.startsWith("00:1A:11") || uppercaseMac.startsWith("FC:FB:FB") -> "Google LLC"
            uppercaseMac.startsWith("00:25:00") || uppercaseMac.startsWith("84:FC:FE") -> "Apple Inc."
            uppercaseMac.startsWith("7C:C2:55") || uppercaseMac.startsWith("BC:D1:1F") -> "TP-Link Corp"
            uppercaseMac.startsWith("00:1E:64") || uppercaseMac.startsWith("EC:F4:BB") -> "Intel Corporation"
            uppercaseMac.startsWith("A4:C1:38") || uppercaseMac.startsWith("B4:E6:2A") -> "Xiaomi Communications"
            uppercaseMac.startsWith("AC:5F:3E") || uppercaseMac.startsWith("04:F1:2F") -> "Samsung Electronics"
            uppercaseMac.startsWith("24:0A:C4") || uppercaseMac.startsWith("30:AE:A4") -> "Espressif Systems"
            uppercaseMac.startsWith("00:50:56") -> "VMware, Inc."
            else -> "Broadcom Network Node"
        }
    }

    private fun generateStaticMockDevices(): List<ScannedDeviceEntity> {
        return listOf(
            ScannedDeviceEntity(
                sessionId = 0,
                name = "ALPHA_QUANTUM_COOP",
                macAddress = "FC:FB:FB:12:9A:8B",
                signalType = "WIFI",
                rssi = -42,
                frequency = "5 GHz",
                channel = "CH 36",
                security = "WPA3 Personal",
                manufacturer = "Google LLC",
                distance = 1.2f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "CYBERNET_NET_EXT",
                macAddress = "84:FC:FE:D2:1C:32",
                signalType = "WIFI",
                rssi = -55,
                frequency = "5 GHz",
                channel = "CH 48",
                security = "WPA2 Enterprise",
                manufacturer = "Apple Inc.",
                distance = 3.5f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "TP-LINK_HIGH_POWER",
                macAddress = "BC:D1:1F:B4:AA:77",
                signalType = "WIFI",
                rssi = -68,
                frequency = "2.4 GHz",
                channel = "CH 6",
                security = "WPA2 Personal",
                manufacturer = "TP-Link Corp",
                distance = 9.2f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "Esp32_Sensor_SmartMesh",
                macAddress = "24:0A:C4:01:E2:BB",
                signalType = "WIFI",
                rssi = -76,
                frequency = "2.4 GHz",
                channel = "CH 11",
                security = "OPEN",
                manufacturer = "Espressif Systems",
                distance = 15.1f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "NeuralLink Band V2",
                macAddress = "EC:F4:BB:3F:8A:2F",
                signalType = "BLUETOOTH",
                rssi = -58,
                frequency = "BLE 2.4 GHz",
                channel = "N/A",
                security = "SECURE PAIRING",
                manufacturer = "Intel Corporation",
                distance = 4.1f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "Galaxy Watch 7",
                macAddress = "AC:5F:3E:91:CE:50",
                signalType = "BLUETOOTH",
                rssi = -62,
                frequency = "Bluetooth Classic",
                channel = "CH 39",
                security = "SECURE",
                manufacturer = "Samsung Electronics",
                distance = 6.4f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "Unknown BLE Tag",
                macAddress = "00:25:00:1E:5F:AA",
                signalType = "BLUETOOTH",
                rssi = -85,
                frequency = "BLE 2.4 GHz",
                channel = "N/A",
                security = "UNSECURED",
                manufacturer = "Apple Inc.",
                distance = 28.5f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "Gateway Router",
                macAddress = "00:1A:11:34:B2:FF",
                signalType = "LAN",
                rssi = -30, // Virtual Ethernet strength
                frequency = "Ethernet RJ45",
                channel = "CH 1",
                security = "LAN Firewall Active",
                manufacturer = "Google LLC",
                distance = 0.5f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "CyberNAS Storage Node",
                macAddress = "00:50:56:4C:CC:D1",
                signalType = "LAN",
                rssi = -38,
                frequency = "Ethernet 10Gbps",
                channel = "CH 2",
                security = "LAN Closed",
                manufacturer = "VMware, Inc.",
                distance = 2.0f
            ),
            ScannedDeviceEntity(
                sessionId = 0,
                name = "Smart Surveillance Cam",
                macAddress = "A4:C1:38:1B:32:04",
                signalType = "LAN",
                rssi = -65,
                frequency = "Wired Node",
                channel = "CH 4",
                security = "LAN Subnet Blocked",
                manufacturer = "Xiaomi Communications",
                distance = 7.8f
            )
        )
    }
}
