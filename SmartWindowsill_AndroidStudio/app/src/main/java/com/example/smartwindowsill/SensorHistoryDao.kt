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

    // Агрегаты по сырым замерам за интервал [from, to)
    @Query(
        """
        SELECT COUNT(*) AS sampleCount,
               AVG(temperature) AS tempMean, MIN(temperature) AS tempMin, MAX(temperature) AS tempMax,
               AVG(humidity) AS humidityMean, MIN(humidity) AS humidityMin, MAX(humidity) AS humidityMax,
               AVG(light) AS lightMean, MIN(light) AS lightMin, MAX(light) AS lightMax,
               AVG(soilMoisture) AS soilMean, MIN(soilMoisture) AS soilMin, MAX(soilMoisture) AS soilMax
        FROM sensor_history
        WHERE timestamp >= :from AND timestamp < :to
        """
    )
    suspend fun aggregate(from: Long, to: Long): DayAggregate

    @Query("DELETE FROM sensor_history WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
