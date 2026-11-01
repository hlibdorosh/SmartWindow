package com.example.smartwindowsill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WateringAdvisorTest {

    private val plant = Plant(
        name = "Monstera",
        imageUri = null,
        minTemp = 18f, optTemp = 22f, maxTemp = 28f,
        minHumidity = 50f, optHumidity = 65f, maxHumidity = 80f,
        minLight = 1500f, optLight = 4000f, maxLight = 8000f,
        wateringIntervalDays = 7
    )

    private fun day(temp: Float, hum: Float) = DailyCondition(
        day = 0L, sampleCount = 10,
        tempMean = temp, tempMin = temp, tempMax = temp,
        humidityMean = hum, humidityMin = hum, humidityMax = hum,
        lightMean = 3000f, lightMin = 1000f, lightMax = 5000f
    )

    @Test
    fun standardConditionsKeepBaseInterval() {
        val status = WateringAdvisor.computeStatus(plant, listOf(day(22f, 65f)))
        assertEquals(7, status.intervalDays)
    }

    @Test
    fun dryAndWarmRoomShortensInterval() {
        // Сухой зимний воздух 25% (ниже оптимума 65%) и отопление 26°C (выше оптимума 22°C)
        val status = WateringAdvisor.computeStatus(plant, listOf(day(26f, 25f)))
        assertTrue(status.intervalDays < 7)
    }

    @Test
    fun humidAndCoolRoomLengthensInterval() {
        // Влажный подоконник 80% (выше оптимума 65%) и прохлада 18°C (ниже оптимума 22°C)
        val status = WateringAdvisor.computeStatus(plant, listOf(day(18f, 80f)))
        assertTrue(status.intervalDays > 7)
    }

    @Test
    fun urgencyDetectedWhenIntervalExceeded() {
        val now = 1_000_000_000_000L
        val eightDaysAgo = now - 8L * 24 * 60 * 60 * 1000
        val wateredPlant = plant.copy(lastWatered = eightDaysAgo)
        val status = WateringAdvisor.computeStatus(wateredPlant, listOf(day(22f, 65f)), now = now)
        assertTrue(status.isUrgent)
        assertTrue(status.nextWateringText.contains("Пора полить"))
    }

    @Test
    fun freshWateringIsNotUrgent() {
        val now = 1_000_000_000_000L
        val oneDayAgo = now - 1L * 24 * 60 * 60 * 1000
        val wateredPlant = plant.copy(lastWatered = oneDayAgo)
        val status = WateringAdvisor.computeStatus(wateredPlant, listOf(day(22f, 65f)), now = now)
        assertFalse(status.isUrgent)
        assertTrue(status.lastWateredText.contains("вчера"))
    }
}
