package com.example.smartwindowsill

import androidx.room.withTransaction
import java.util.Calendar

// Начало локальных суток для момента ts
fun dayStartMillis(ts: Long): Long = Calendar.getInstance().apply {
    timeInMillis = ts
    set(Calendar.HOUR_OF_DAY, 0)
    set(Calendar.MINUTE, 0)
    set(Calendar.SECOND, 0)
    set(Calendar.MILLISECOND, 0)
}.timeInMillis

fun nextDayStartMillis(dayStart: Long): Long = Calendar.getInstance().apply {
    timeInMillis = dayStart
    add(Calendar.DAY_OF_YEAR, 1)
}.timeInMillis

// Запись замеров: сырой замер + пересчёт дневного агрегата + очистка старых сырых данных.
// Сырые замеры хранятся RAW_RETENTION_DAYS дней, дневные агрегаты — бессрочно.
class ConditionRepository(private val db: AppDatabase) {

    suspend fun record(
        temperature: Float,
        humidity: Float,
        light: Float,
        soilMoisture: Float? = null,
        now: Long = System.currentTimeMillis()
    ) {
        // DHT возвращает NaN при сбое чтения — такие замеры не сохраняем
        if (!temperature.isFinite() || !humidity.isFinite() || !light.isFinite()) return
        val soil = soilMoisture?.takeIf { it.isFinite() }

        val sensorDao = db.sensorHistoryDao()
        val dailyDao = db.dailyConditionDao()

        db.withTransaction {
            sensorDao.insertHistory(
                SensorHistory(
                    timestamp = now,
                    temperature = temperature,
                    humidity = humidity,
                    light = light,
                    soilMoisture = soil
                )
            )
            val start = dayStartMillis(now)
            sensorDao.aggregate(start, nextDayStartMillis(start)).toEntity(start)?.let { dailyDao.upsert(it) }
            sensorDao.deleteOlderThan(now - RAW_RETENTION_DAYS * DAY_MS)
        }
    }

    companion object {
        const val RAW_RETENTION_DAYS = 14L
        private const val DAY_MS = 24L * 60 * 60 * 1000
    }
}
