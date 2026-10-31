package com.example.smartwindowsill

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "plants")
data class Plant(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    @ColumnInfo(defaultValue = "") val nickname: String = "",
    val scientificName: String? = null,
    val imageUri: String?, // Путь к фотографии растения (файл в хранилище приложения)

    // Параметры Температуры (°C)
    val minTemp: Float,
    val optTemp: Float,
    val maxTemp: Float,

    // Параметры Влажности (%)
    val minHumidity: Float,
    val optHumidity: Float,
    val maxHumidity: Float,

    // Параметры Освещенности (lx, как на экране датчика)
    val minLight: Float,
    val optLight: Float,
    val maxLight: Float,

    // Параметры влажности почвы (%), null — нет данных (датчика почвы пока нет)
    val minSoil: Float? = null,
    val optSoil: Float? = null,
    val maxSoil: Float? = null,

    // Когда растение добавлено (мс с эпохи)
    @ColumnInfo(defaultValue = "0") val addedAt: Long = System.currentTimeMillis(),
    // Когда последний раз поливали (мс с эпохи), null — ещё не поливали
    val lastWatered: Long? = null
)
