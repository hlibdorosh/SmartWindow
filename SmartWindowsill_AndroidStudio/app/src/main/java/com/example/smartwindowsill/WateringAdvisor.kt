package com.example.smartwindowsill

import java.text.DateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class WateringStatus(
    val intervalDays: Int,            // Частота полива (дней, с поправкой на комфортные условия)
    val lastWateredText: String,      // Когда поливали ("3 дн. назад (29 окт.)", "ещё не поливали")
    val nextWateringText: String,     // Сколько до след. полива ("через ~2 дн.", "⚠️ Пора полить!")
    val isUrgent: Boolean,
    val waterPercent: Int,            // Процент оставшейся влаги (100%..0%)
    val waterFraction: Float          // Доля оставшейся влаги (1.0f..0.0f)
)

object WateringAdvisor {

    fun computeStatus(
        plant: Plant,
        history: List<DailyCondition>,
        now: Long = System.currentTimeMillis()
    ): WateringStatus {
        val baseDays = plant.wateringIntervalDays ?: 7

        // Считаем средние условия за последние дни по датчикам (по умолчанию — оптимум растения)
        val avgTemp = history.map { it.tempMean }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: plant.optTemp
        val avgHum = history.map { it.humidityMean }.takeIf { it.isNotEmpty() }?.average()?.toFloat() ?: plant.optHumidity

        // Поправка на основе отклонения от оптимума комфорта конкретного растения:
        // Если влажность ниже оптимума комфорта: воздух суше -> грунт сохнет быстрее.
        val fHum = (1.0f + (avgHum - plant.optHumidity) * 0.015f).coerceIn(0.65f, 1.4f)
        // Если температура выше оптимума: теплее -> испарение выше.
        val fTemp = (1.0f - (avgTemp - plant.optTemp) * 0.02f).coerceIn(0.7f, 1.3f)

        val multiplier = (fHum * fTemp).coerceIn(0.5f, 2.0f)
        val adjustedDays = (baseDays * multiplier).roundToInt().coerceAtLeast(1)

        val totalIntervalMs = adjustedDays * 24L * 60L * 60L * 1000L
        val (waterFraction, waterPercent) = if (plant.lastWatered != null) {
            val elapsedMs = (now - plant.lastWatered).coerceAtLeast(0L)
            val fraction = (1.0f - (elapsedMs.toDouble() / totalIntervalMs.toDouble()).toFloat()).coerceIn(0f, 1f)
            val percent = (fraction * 100f).roundToInt()
            fraction to percent
        } else {
            0f to 0
        }

        val daysSince = plant.lastWatered?.let {
            val diffMs = (now - it).coerceAtLeast(0L)
            TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
        }

        val daysUntil = daysSince?.let { adjustedDays - it }
        val isUrgent = daysSince == null || (daysUntil != null && daysUntil <= 0)

        val lastWateredFormatted = if (plant.lastWatered != null) {
            val dateStr = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(plant.lastWatered))
            when (daysSince) {
                0 -> "сегодня ($dateStr)"
                1 -> "вчера ($dateStr)"
                else -> "$daysSince дн. назад ($dateStr)"
            }
        } else {
            "ещё не поливали"
        }

        val nextWateringFormatted = when {
            daysSince == null -> "пора полить"
            daysUntil != null && daysUntil < 0 -> "пора полить, просрочено на ${-daysUntil} дн."
            daysUntil == 0 -> "пора полить сегодня"
            daysUntil == 1 -> "завтра"
            else -> "через ~$daysUntil дн."
        }

        return WateringStatus(
            intervalDays = adjustedDays,
            lastWateredText = lastWateredFormatted,
            nextWateringText = nextWateringFormatted,
            isUrgent = isUrgent,
            waterPercent = waterPercent,
            waterFraction = waterFraction
        )
    }
}
