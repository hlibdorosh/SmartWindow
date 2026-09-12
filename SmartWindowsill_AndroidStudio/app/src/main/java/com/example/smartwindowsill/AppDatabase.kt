package com.example.smartwindowsill

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Plant::class, SensorHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun sensorHistoryDao(): SensorHistoryDao
}
