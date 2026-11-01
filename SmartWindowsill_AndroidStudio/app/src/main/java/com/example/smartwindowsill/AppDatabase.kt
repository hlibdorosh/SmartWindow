package com.example.smartwindowsill

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Plant::class, SensorHistory::class, DailyCondition::class],
    version = 8,
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

        // v5 -> v6: remove soil columns from plants, sensor_history, and daily_condition_history
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1) plants
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plants_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `nickname` TEXT NOT NULL DEFAULT '',
                        `scientificName` TEXT,
                        `imageUri` TEXT,
                        `minTemp` REAL NOT NULL,
                        `optTemp` REAL NOT NULL,
                        `maxTemp` REAL NOT NULL,
                        `minHumidity` REAL NOT NULL,
                        `optHumidity` REAL NOT NULL,
                        `maxHumidity` REAL NOT NULL,
                        `minLight` REAL NOT NULL,
                        `optLight` REAL NOT NULL,
                        `maxLight` REAL NOT NULL,
                        `addedAt` INTEGER NOT NULL DEFAULT 0,
                        `lastWatered` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `plants_new` (`id`, `name`, `nickname`, `scientificName`, `imageUri`,
                        `minTemp`, `optTemp`, `maxTemp`, `minHumidity`, `optHumidity`, `maxHumidity`,
                        `minLight`, `optLight`, `maxLight`, `addedAt`, `lastWatered`)
                    SELECT `id`, `name`, `nickname`, `scientificName`, `imageUri`,
                        `minTemp`, `optTemp`, `maxTemp`, `minHumidity`, `optHumidity`, `maxHumidity`,
                        `minLight`, `optLight`, `maxLight`, `addedAt`, `lastWatered`
                    FROM `plants`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `plants`")
                db.execSQL("ALTER TABLE `plants_new` RENAME TO `plants`")

                // 2) sensor_history
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `sensor_history_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `temperature` REAL NOT NULL,
                        `humidity` REAL NOT NULL,
                        `light` REAL NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `sensor_history_new` (`id`, `temperature`, `humidity`, `light`, `timestamp`)
                    SELECT `id`, `temperature`, `humidity`, `light`, `timestamp`
                    FROM `sensor_history`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `sensor_history`")
                db.execSQL("ALTER TABLE `sensor_history_new` RENAME TO `sensor_history`")

                // 3) daily_condition_history
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_condition_history_new` (
                        `day` INTEGER NOT NULL,
                        `sampleCount` INTEGER NOT NULL,
                        `tempMean` REAL NOT NULL,
                        `tempMin` REAL NOT NULL,
                        `tempMax` REAL NOT NULL,
                        `humidityMean` REAL NOT NULL,
                        `humidityMin` REAL NOT NULL,
                        `humidityMax` REAL NOT NULL,
                        `lightMean` REAL NOT NULL,
                        `lightMin` REAL NOT NULL,
                        `lightMax` REAL NOT NULL,
                        PRIMARY KEY(`day`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `daily_condition_history_new` (`day`, `sampleCount`,
                        `tempMean`, `tempMin`, `tempMax`,
                        `humidityMean`, `humidityMin`, `humidityMax`,
                        `lightMean`, `lightMin`, `lightMax`)
                    SELECT `day`, `sampleCount`,
                        `tempMean`, `tempMin`, `tempMax`,
                        `humidityMean`, `humidityMin`, `humidityMax`,
                        `lightMean`, `lightMin`, `lightMax`
                    FROM `daily_condition_history`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `daily_condition_history`")
                db.execSQL("ALTER TABLE `daily_condition_history_new` RENAME TO `daily_condition_history`")
            }
        }

        // v6 -> v7: plants got a wateringIntervalDays column
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN wateringIntervalDays INTEGER")
            }
        }

        // v7 -> v8: remove name column from plants, migrate empty nickname to name
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `plants_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `nickname` TEXT NOT NULL,
                        `scientificName` TEXT,
                        `imageUri` TEXT,
                        `minTemp` REAL NOT NULL,
                        `optTemp` REAL NOT NULL,
                        `maxTemp` REAL NOT NULL,
                        `minHumidity` REAL NOT NULL,
                        `optHumidity` REAL NOT NULL,
                        `maxHumidity` REAL NOT NULL,
                        `minLight` REAL NOT NULL,
                        `optLight` REAL NOT NULL,
                        `maxLight` REAL NOT NULL,
                        `addedAt` INTEGER NOT NULL DEFAULT 0,
                        `lastWatered` INTEGER,
                        `wateringIntervalDays` INTEGER
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `plants_new` (`id`, `nickname`, `scientificName`, `imageUri`,
                        `minTemp`, `optTemp`, `maxTemp`, `minHumidity`, `optHumidity`, `maxHumidity`,
                        `minLight`, `optLight`, `maxLight`, `addedAt`, `lastWatered`, `wateringIntervalDays`)
                    SELECT `id`,
                        CASE WHEN `nickname` IS NOT NULL AND TRIM(`nickname`) != '' THEN `nickname` ELSE `name` END,
                        `scientificName`, `imageUri`,
                        `minTemp`, `optTemp`, `maxTemp`, `minHumidity`, `optHumidity`, `maxHumidity`,
                        `minLight`, `optLight`, `maxLight`, `addedAt`, `lastWatered`, `wateringIntervalDays`
                    FROM `plants`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `plants`")
                db.execSQL("ALTER TABLE `plants_new` RENAME TO `plants`")
            }
        }
    }
}
