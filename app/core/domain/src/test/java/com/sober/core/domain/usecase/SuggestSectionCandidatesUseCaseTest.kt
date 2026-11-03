package com.sober.core.domain.usecase

import com.sober.core.domain.model.DetectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestSectionCandidatesUseCaseTest {

    private val suggest = SuggestSectionCandidatesUseCase()

    @Test
    fun `selected tabs come first and keep their labels`() {
        val candidates = suggest(
            activityName = "com.example.app.MainActivity",
            selected = listOf(
                SelectedSectionNode("com.example:id/reels_tab", "Reels"),
                SelectedSectionNode("com.example:id/home_tab", "Home")
            )
        )

        assertEquals(3, candidates.size)
        assertEquals("com.example:id/reels_tab", candidates[0].value)
        assertEquals("Reels", candidates[0].label)
        assertEquals(DetectionType.SELECTED_TAB, candidates[0].type)
        assertEquals("Home", candidates[1].label)
    }

    @Test
    fun `a missing label is derived from the id rather than left blank`() {
        val candidates = suggest(
            activityName = null,
            selected = listOf(SelectedSectionNode("com.example:id/shorts_tab", null))
        )

        assertEquals("Shorts tab", candidates.single().label)
    }

    @Test
    fun `one tab selected in two nodes still produces one candidate`() {
        val candidates = suggest(
            activityName = null,
            selected = listOf(
                SelectedSectionNode("com.example:id/reels_tab", "Reels"),
                SelectedSectionNode("com.example:id/reels_tab", "Reels again")
            )
        )

        assertEquals(1, candidates.size)
        assertEquals("Reels", candidates.single().label)
    }

    @Test
    fun `the activity is offered last and marked broad`() {
        val candidates = suggest(
            activityName = "com.example.app.MainActivity",
            selected = listOf(SelectedSectionNode("com.example:id/reels_tab", "Reels"))
        )

        val activity = candidates.last()
        assertEquals(DetectionType.ACTIVITY, activity.type)
        assertEquals("com.example.app.MainActivity", activity.value)
        assertEquals("MainActivity", activity.label)
        assertTrue(activity.isBroad)
    }

    @Test
    fun `no selected tab and no activity means no candidates`() {
        assertTrue(suggest(activityName = null, selected = emptyList()).isEmpty())
        assertTrue(suggest(activityName = "  ", selected = emptyList()).isEmpty())
    }

    @Test
    fun `blank ids are ignored`() {
        val candidates = suggest(
            activityName = null,
            selected = listOf(SelectedSectionNode("   ", "Nothing"))
        )

        assertTrue(candidates.isEmpty())
    }
}
