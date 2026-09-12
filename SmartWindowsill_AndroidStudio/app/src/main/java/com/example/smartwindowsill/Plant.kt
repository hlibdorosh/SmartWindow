package com.example.smartwindowsill

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val imageUri: String?, // Путь к фотографии растения

    // Параметры Температуры (°C)
    val minTemp: Float,
    val optTemp: Float,
    val maxTemp: Float,

    // Параметры Влажности (%)
    val minHumidity: Float,
    val optHumidity: Float,
    val maxHumidity: Float,

    // Параметры Освещенности (%)
    val minLight: Float,
    val optLight: Float,
    val maxLight: Float
)