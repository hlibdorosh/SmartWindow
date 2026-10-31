package com.example.smartwindowsill

import kotlin.math.abs
import kotlin.math.max

enum class Metric(val label: String, val unit: String) {
    TEMPERATURE("Температура", "°C"),
    HUMIDITY("Влажность", "%"),
    LIGHT("Свет", "lx")
}

data class IdealRange(val min: Float, val opt: Float, val max: Float)

fun Plant.idealRange(metric: Metric): IdealRange? = when (metric) {
    Metric.TEMPERATURE -> IdealRange(minTemp, optTemp, maxTemp)
    Metric.HUMIDITY -> IdealRange(minHumidity, optHumidity, maxHumidity)
    Metric.LIGHT -> IdealRange(minLight, optLight, maxLight)
}

// (mean, min, max) параметра за день или null, если датчика/данных нет
data class DayStat(val mean: Float, val min: Float, val max: Float)

fun DailyCondition.stat(metric: Metric): DayStat? = when (metric) {
    Metric.TEMPERATURE -> DayStat(tempMean, tempMin, tempMax)
    Metric.HUMIDITY -> DayStat(humidityMean, humidityMin, humidityMax)
    Metric.LIGHT -> DayStat(lightMean, lightMin, lightMax)
}

/**
 * Комфорт одного параметра, 0..1, по нормированному отклонению от идеала.
 * Отклонение делится на «полуширину» диапазона с той стороны, куда ушло значение:
 *   идеал -> 1.0, граница допустимого диапазона (min/max) -> 0.5, два диапазона от идеала и дальше -> 0.
 */
fun parameterScore(value: Float, range: IdealRange): Float {
    val side = if (value < range.opt) range.opt - range.min else range.max - range.opt
    // Вырожденный диапазон (min == opt) — берём условную ширину, чтобы не делить на ноль
    val halfWidth = if (side > 1e-3f) side else max(abs(range.opt) * 0.05f, 1f)
    val deviation = abs(value - range.opt) / halfWidth
    return (1f - deviation / 2f).coerceIn(0f, 1f)
}

// Вклад одного параметра в индекс за день
data class MetricResult(
    val metric: Metric,
    val value: Float,           // среднее за день
    val dayMin: Float,
    val dayMax: Float,
    val ideal: IdealRange,
    val score: Float,           // 0..1
    val lossPoints: Float       // сколько пунктов индекса (0..100) параметр «съедает»
)

data class ComfortPoint(
    val day: Long,
    val index: Float,           // 0..100, по средним за день
    val low: Float,             // 0..100, худший случай внутри дня (по min/max)
    val high: Float,            // 0..100, лучший случай внутри дня
    val parts: List<MetricResult> // отсортированы: сильнее всех тянущие вниз — первые
)

/**
 * Индекс комфорта за день только по активным параметрам: исключённые не учитываются,
 * а итог нормируется по оставшимся. Параметры без данных (нет датчика/порогов) пропускаются.
 * Возвращает null, если не осталось ни одного пригодного параметра.
 */
fun computeComfort(day: DailyCondition, plant: Plant, active: Set<Metric>): ComfortPoint? {
    val parts = ArrayList<MetricResult>()
    var worstSum = 0f
    var bestSum = 0f

    for (metric in Metric.entries) {
        if (metric !in active) continue
        val ideal = plant.idealRange(metric) ?: continue
        val stat = day.stat(metric) ?: continue

        val score = parameterScore(stat.mean, ideal)
        val scoreAtMin = parameterScore(stat.min, ideal)
        val scoreAtMax = parameterScore(stat.max, ideal)
        worstSum += minOf(scoreAtMin, scoreAtMax)
        // Если идеал внутри дневного диапазона, значение проходило через идеал
        bestSum += if (ideal.opt in stat.min..stat.max) 1f else maxOf(scoreAtMin, scoreAtMax)

        parts += MetricResult(metric, stat.mean, stat.min, stat.max, ideal, score, lossPoints = 0f)
    }
    if (parts.isEmpty()) return null

    val n = parts.size
    val index = parts.sumOf { it.score.toDouble() }.toFloat() / n * 100f
    val withLoss = parts
        .map { it.copy(lossPoints = (1f - it.score) / n * 100f) }
        .sortedByDescending { it.lossPoints }

    return ComfortPoint(
        day = day.day,
        index = index,
        low = (worstSum / n * 100f).coerceIn(0f, 100f),
        high = (bestSum / n * 100f).coerceIn(0f, 100f),
        parts = withLoss
    )
}
