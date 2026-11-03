package com.example.smartwindowsill

import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

object DataSeeder {

    suspend fun seedIfEmpty(database: AppDatabase) {
        val dailyDao = database.dailyConditionDao()
        val sensorDao = database.sensorHistoryDao()
        val plantDao = database.plantDao()
        val db = database.openHelper.writableDatabase

        // 1. Создаем исторические агрегаты за последние 45 дней для графиков комфорта
        val todayStart = dayStartMillis(System.currentTimeMillis())
        val cal = Calendar.getInstance()

        for (d in 45 downTo 0) {
            cal.timeInMillis = todayStart
            cal.add(Calendar.DAY_OF_YEAR, -d)
            val dayTs = cal.timeInMillis

            val progress = d.toDouble() / 45.0

            // Реалистичные колебания условий: сезонная синусоида + микроколебания
            // Дни 18-20 моделируют легкое похолодание (проветривание/пасмурная погода) для наглядности графика
            val isDipDay = d in 18..20
            val tempMean = if (isDipDay) {
                17.8f + (d % 2) * 0.4f
            } else {
                (22.2 + 1.6 * sin(progress * 4.0 * Math.PI) + sin(d * 1.7) * 0.7).toFloat()
            }
            val tempMin = tempMean - (1.6f + (d % 3) * 0.4f)
            val tempMax = tempMean + (2.0f + (d % 4) * 0.5f)

            val humMean = if (isDipDay) {
                38.0f + (d % 3) * 1.5f
            } else {
                (58.0 + 5.5 * cos(progress * 3.0 * Math.PI) + cos(d * 2.1) * 1.8).toFloat().coerceIn(45f, 75f)
            }
            val humMin = humMean - (5.0f + (d % 4) * 0.7f)
            val humMax = humMean + (6.0f + (d % 3) * 0.9f)

            val lightMean = if (isDipDay) {
                950f + (d % 3) * 100f
            } else {
                (1900.0 + 650.0 * sin(progress * 5.0 * Math.PI + 1.0) + (d % 5) * 90.0).toFloat().coerceIn(900f, 3200f)
            }
            val lightMin = 0f
            val lightMax = (lightMean * 2.3f).coerceIn(1200f, 7500f)

            dailyDao.upsert(
                DailyCondition(
                    day = dayTs,
                    sampleCount = 1440,
                    tempMean = tempMean,
                    tempMin = tempMin,
                    tempMax = tempMax,
                    humidityMean = humMean,
                    humidityMin = humMin,
                    humidityMax = humMax,
                    lightMean = lightMean,
                    lightMin = lightMin,
                    lightMax = lightMax
                )
            )
        }

        // 2. Создаем 60 минут точек для живого графика датчиков на главном экране (если пусто)
        var sensorCount = 0
        try {
            db.query("SELECT COUNT(*) FROM sensor_history").use { cursor ->
                if (cursor.moveToFirst()) sensorCount = cursor.getInt(0)
            }
        } catch (_: Exception) {}

        if (sensorCount < 10) {
            val now = System.currentTimeMillis()
            for (min in 59 downTo 0) {
                val ts = now - min * 60_000L
                val t = 22.3f + (sin(min / 9.0) * 0.5).toFloat()
                val h = 57.5f + (cos(min / 11.0) * 0.9).toFloat()
                val l = 1850f + (sin(min / 7.0) * 160f).toFloat()
                sensorDao.insertHistory(
                    SensorHistory(
                        timestamp = ts,
                        temperature = t,
                        humidity = h,
                        light = l
                    )
                )
            }
        }

        // 3. Создаем базовое растение, если база еще пуста
        var plantCount = 0
        try {
            db.query("SELECT COUNT(*) FROM plants").use { cursor ->
                if (cursor.moveToFirst()) plantCount = cursor.getInt(0)
            }
        } catch (_: Exception) {}

        if (plantCount == 0) {
            plantDao.insertPlant(
                Plant(
                    nickname = "Монстера",
                    scientificName = "Monstera deliciosa",
                    imageUri = null,
                    minTemp = 18f,
                    optTemp = 22f,
                    maxTemp = 28f,
                    minHumidity = 50f,
                    optHumidity = 65f,
                    maxHumidity = 80f,
                    minLight = 1000f,
                    optLight = 2500f,
                    maxLight = 6000f,
                    wateringIntervalDays = 7,
                    lastWatered = System.currentTimeMillis() - 2 * 24 * 3600 * 1000L
                )
            )
        }
    }
}
