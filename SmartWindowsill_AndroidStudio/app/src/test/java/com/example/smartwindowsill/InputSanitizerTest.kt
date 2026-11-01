package com.example.smartwindowsill

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InputSanitizerTest {

    @Test
    fun stripsNewlinesAndTabs() {
        val input = "Фикус\n\tБенджамина\r\n"
        val result = InputSanitizer.sanitize(input)
        assertEquals("Фикус Бенджамина", result)
    }

    @Test
    fun stripsAngleBracketsAndQuotes() {
        val input = "<script> \"alert('xss')\" </script> Монстера"
        val result = InputSanitizer.sanitize(input)
        assertEquals("script alert('xss') /script Монстера", result)
    }

    @Test
    fun preservesCultivarSingleQuotes() {
        val input = "Фикус 'Даниэль'"
        val result = InputSanitizer.sanitize(input)
        assertEquals("Фикус 'Даниэль'", result)
    }

    @Test
    fun limitsLengthTo60Characters() {
        val longInput = "А".repeat(100)
        val result = InputSanitizer.sanitize(longInput)
        assertEquals(60, result.length)
        assertEquals("А".repeat(60), result)
    }

    @Test
    fun filterInputPreventsForbiddenCharsAndLength() {
        val input = "Спатифиллум<>\n\t\"тест\"\\"
        val filtered = InputSanitizer.filterInput(input, maxLength = 60)
        assertEquals("Спатифиллумтест", filtered)

        val longInput = "Б".repeat(70)
        val capped = InputSanitizer.filterInput(longInput, maxLength = 60)
        assertEquals(60, capped.length)
    }

    @Test
    fun preservesValidPlantNames() {
        val normalName = "Замиокулькас замиелистный 2"
        assertEquals(normalName, InputSanitizer.sanitize(normalName))
    }
}
