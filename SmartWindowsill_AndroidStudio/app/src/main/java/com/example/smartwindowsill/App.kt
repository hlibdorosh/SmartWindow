package com.example.smartwindowsill

import android.app.Application
import androidx.room.Room

class App : Application() {
    lateinit var database: AppDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        
        database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "smart_windowsill.db"
        ).build()
    }
}
