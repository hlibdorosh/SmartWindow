package com.example.smartwindowsill

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Plant::class, SensorHistory::class, DailyCondition::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun sensorHistoryDao(): SensorHistoryDao
    abstract fun dailyConditionDao(): DailyConditionDao

    companion object {
        // v1 -> v2: plants got a nickname column
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN nickname TEXT NOT NULL DEFAULT ''")
            }
        }

        // v2 -> v3: plants got addedAt and lastWatered columns
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN addedAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE plants ADD COLUMN lastWatered INTEGER")
                // Существующие растения считаем добавленными в момент обновления
                db.execSQL("UPDATE plants SET addedAt = CAST(strftime('%s','now') AS INTEGER) * 1000")
            }
        }

        // v3 -> v4: soil columns, soil moisture in raw telemetry, daily aggregate table
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN minSoil REAL")
                db.execSQL("ALTER TABLE plants ADD COLUMN optSoil REAL")
                db.execSQL("ALTER TABLE plants ADD COLUMN maxSoil REAL")
                db.execSQL("ALTER TABLE sensor_history ADD COLUMN soilMoisture REAL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS daily_condition_history (
                        day INTEGER NOT NULL,
                        sampleCount INTEGER NOT NULL,
                        tempMean REAL NOT NULL, tempMin REAL NOT NULL, tempMax REAL NOT NULL,
                        humidityMean REAL NOT NULL, humidityMin REAL NOT NULL, humidityMax REAL NOT NULL,
                        lightMean REAL NOT NULL, lightMin REAL NOT NULL, lightMax REAL NOT NULL,
                        soilMean REAL, soilMin REAL, soilMax REAL,
                        PRIMARY KEY(day)
                    )
                    """.trimIndent()
                )
            }
        }

        // v4 -> v5: plants got a scientificName column
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN scientificName TEXT")
            }
        }
    }
}
