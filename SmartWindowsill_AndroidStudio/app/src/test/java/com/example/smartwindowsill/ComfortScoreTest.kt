package com.example.smartwindowsill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ComfortScoreTest {

    private val plant = Plant(
        name = "Test", imageUri = null,
        minTemp = 15f, optTemp = 22f, maxTemp = 30f,
        minHumidity = 40f, optHumidity = 60f, maxHumidity = 80f,
        minLight = 1000f, optLight = 5000f, maxLight = 9000f
    )

    private fun day(temp: Float, hum: Float, light: Float, spread: Float = 0f) = DailyCondition(
        day = 0L, sampleCount = 10,
        tempMean = temp, tempMin = temp - spread, tempMax = temp + spread,
        humidityMean = hum, humidityMin = hum - spread, humidityMax = hum + spread,
        lightMean = light, lightMin = light - spread, lightMax = light + spread
    )

    private val all = setOf(Metric.TEMPERATURE, Metric.HUMIDITY, Metric.LIGHT)

    @Test fun idealValuesGiveFullScore() {
        assertEquals(100f, computeComfort(day(22f, 60f, 5000f), plant, all)!!.index, 0.01f)
    }

    @Test fun rangeEdgeIsHalfScore() {
        val r = IdealRange(15f, 22f, 30f)
        assertEquals(0.5f, parameterScore(15f, r), 0.001f)
        assertEquals(0.5f, parameterScore(30f, r), 0.001f)
    }

    @Test fun farOutsideIsZeroAndNeverNegative() {
        assertEquals(0f, parameterScore(-50f, IdealRange(15f, 22f, 30f)), 0f)
    }

    @Test fun disabledMetricIsExcludedAndScoreRenormalized() {
        // Температура далеко от идеала, влажность и свет идеальны
        val d = day(temp = 45f, hum = 60f, light = 5000f)
        val withTemp = computeComfort(d, plant, all)!!.index
        val withoutTemp = computeComfort(d, plant, setOf(Metric.HUMIDITY, Metric.LIGHT))!!.index
        assertTrue(withTemp < 70f)
        assertEquals(100f, withoutTemp, 0.01f)
    }

    @Test fun worstOffenderIsListedFirst() {
        val point = computeComfort(day(temp = 22f, hum = 45f, light = 5000f), plant, all)!!
        assertEquals(Metric.HUMIDITY, point.parts.first().metric)
        // Потери по параметрам в сумме равны недостающим пунктам до 100
        assertEquals(100f - point.index, point.parts.sumOf { it.lossPoints.toDouble() }.toFloat(), 0.01f)
    }

    @Test fun noActiveUsableMetricsGivesNull() {
        assertNull(computeComfort(day(22f, 60f, 5000f), plant, emptySet()))
        // Почва выбрана, но у растения нет порогов и нет данных
        assertNull(computeComfort(day(22f, 60f, 5000f), plant, setOf(Metric.SOIL)))
    }

    @Test fun soilIsUsedWhenAvailable() {
        val soilPlant = plant.copy(minSoil = 20f, optSoil = 50f, maxSoil = 70f)
        val d = day(22f, 60f, 5000f).copy(soilMean = 50f, soilMin = 50f, soilMax = 50f)
        val p = computeComfort(d, soilPlant, Metric.entries.toSet())
        assertNotNull(p)
        assertEquals(4, p!!.parts.size)
    }

    @Test fun dailySwingWidensLowHighBand() {
        val p = computeComfort(day(22f, 60f, 5000f, spread = 6f), plant, setOf(Metric.TEMPERATURE))!!
        assertTrue(p.low < p.index)
        assertEquals(100f, p.high, 0.01f) // идеал 22 лежит внутри дневного диапазона 16..28
    }
}
