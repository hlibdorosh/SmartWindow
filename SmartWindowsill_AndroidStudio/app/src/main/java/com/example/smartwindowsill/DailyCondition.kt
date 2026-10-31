package com.example.smartwindowsill

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// Дневной срез условий (одна строка на локальные сутки). Сглаживает суточные колебания:
// для каждого параметра хранятся среднее, минимум и максимум за день.
// Окружение общее для всех растений на подоконнике, поэтому таблица не привязана к plantId —
// индекс комфорта для конкретного растения считается из этих данных и его порогов.
@Entity(tableName = "daily_condition_history")
data class DailyCondition(
    @PrimaryKey val day: Long,          // начало локальных суток, мс с эпохи
    val sampleCount: Int,
    val tempMean: Float, val tempMin: Float, val tempMax: Float,             // °C
    val humidityMean: Float, val humidityMin: Float, val humidityMax: Float, // %
    val lightMean: Float, val lightMin: Float, val lightMax: Float           // lx
)

// Результат агрегации сырых замеров за сутки (null, если замеров не было)
data class DayAggregate(
    val sampleCount: Int,
    val tempMean: Float?, val tempMin: Float?, val tempMax: Float?,
    val humidityMean: Float?, val humidityMin: Float?, val humidityMax: Float?,
    val lightMean: Float?, val lightMin: Float?, val lightMax: Float?
) {
    fun toEntity(day: Long): DailyCondition? {
        if (sampleCount <= 0) return null
        return DailyCondition(
            day = day,
            sampleCount = sampleCount,
            tempMean = tempMean ?: return null, tempMin = tempMin ?: return null, tempMax = tempMax ?: return null,
            humidityMean = humidityMean ?: return null, humidityMin = humidityMin ?: return null,
            humidityMax = humidityMax ?: return null,
            lightMean = lightMean ?: return null, lightMin = lightMin ?: return null, lightMax = lightMax ?: return null
        )
    }
}

@Dao
interface DailyConditionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: DailyCondition)

    @Query("SELECT * FROM daily_condition_history WHERE day >= :since ORDER BY day ASC")
    fun getSince(since: Long): Flow<List<DailyCondition>>
}
