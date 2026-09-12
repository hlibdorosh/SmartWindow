package com.example.smartwindowsill

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_history")
data class SensorHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,    // Время замера (например, 16.07.2026 14:00)
    val temperature: Float,
    val humidity: Float,
    val light: Float
)