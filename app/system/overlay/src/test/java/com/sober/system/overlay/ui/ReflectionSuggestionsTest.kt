package com.sober.system.overlay.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReflectionSuggestionsTest {

    @Test
    fun `every hour gets three distinct alternatives`() {
        (0..23).forEach { hour ->
            val suggestions = reflectionSuggestions(hour)

            assertEquals("hour $hour", 3, suggestions.size)
            assertEquals("hour $hour", suggestions.size, suggestions.toSet().size)
            assertTrue("hour $hour", suggestions.all { it.isNotBlank() })
        }
    }

    @Test
    fun `the window changes at the stated hours`() {
        val night = reflectionSuggestions(0)
        val morning = reflectionSuggestions(5)
        val afternoon = reflectionSuggestions(12)
        val evening = reflectionSuggestions(18)

        // Inside a window the list is the same; the boundary hour opens the next.
        assertEquals(night, reflectionSuggestions(4))
        assertEquals(morning, reflectionSuggestions(11))
        assertEquals(afternoon, reflectionSuggestions(17))
        assertEquals(evening, reflectionSuggestions(22))
        assertEquals(night, reflectionSuggestions(23))

        assertTrue(morning != night)
        assertTrue(afternoon != morning)
        assertTrue(evening != afternoon)
    }
}
