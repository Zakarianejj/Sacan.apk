package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface SignalDao {

    @Query("SELECT * FROM signal_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<SignalSessionEntity>>

    @Query("SELECT * FROM signal_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: Int): SignalSessionEntity?

    @Query("SELECT * FROM scanned_devices WHERE sessionId = :sessionId ORDER BY rssi DESC")
    suspend fun getDevicesForSession(sessionId: Int): List<ScannedDeviceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SignalSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevices(devices: List<ScannedDeviceEntity>)

    @Query("DELETE FROM signal_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Int)

    @Query("DELETE FROM scanned_devices WHERE sessionId = :sessionId")
    suspend fun deleteDevicesBySessionId(sessionId: Int)

    @Transaction
    suspend fun deleteSessionWithDevices(sessionId: Int) {
        deleteDevicesBySessionId(sessionId)
        deleteSessionById(sessionId)
    }

    @Query("SELECT AVG(rssi) FROM scanned_devices")
    suspend fun getGlobalAverageRssi(): Float?
}
