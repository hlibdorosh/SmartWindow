package com.example.smartwindowsill

import android.app.Application
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()

        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "smart_windowsill.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4,
                AppDatabase.MIGRATION_4_5,
                AppDatabase.MIGRATION_5_6,
                AppDatabase.MIGRATION_6_7,
                AppDatabase.MIGRATION_7_8
            )
            .fallbackToDestructiveMigration(dropAllTables = false)
            .build()

        // Очистка возможных старых аномальных значений освещенности (глюк 204700 lx)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = database.openHelper.writableDatabase
                db.execSQL("UPDATE sensor_history SET light = 15000 WHERE light > 20000")
                db.execSQL("UPDATE daily_condition_history SET lightMax = 15000 WHERE lightMax > 20000")
                db.execSQL("UPDATE daily_condition_history SET lightMean = 15000 WHERE lightMean > 20000")
            } catch (_: Exception) {}
        }
    }
}
