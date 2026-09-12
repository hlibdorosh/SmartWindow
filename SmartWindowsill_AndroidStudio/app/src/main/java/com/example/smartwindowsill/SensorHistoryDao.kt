package com.example.smartwindowsill

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SensorHistoryDao {
    @Query("SELECT * FROM sensor_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SensorHistory>>

    @Insert
    suspend fun insertHistory(history: SensorHistory)
    
    @Query("DELETE FROM sensor_history")
    suspend fun clearHistory()
}
