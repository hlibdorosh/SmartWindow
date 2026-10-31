package com.example.smartwindowsill

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sensor_history")
data class SensorHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: Long,    // Время замера (мс с эпохи)
    val temperature: Float,
    val humidity: Float,
    val light: Float,       // lx
    val soilMoisture: Float? = null // %, null пока нет датчика почвы
)
