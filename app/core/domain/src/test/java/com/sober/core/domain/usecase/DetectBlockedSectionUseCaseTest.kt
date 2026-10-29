package com.sober.core.domain.usecase

import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.DetectionType
import com.sober.core.domain.model.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DetectBlockedSectionUseCaseTest {

    private val useCase = DetectBlockedSectionUseCase()

    private fun rule(
        id: String = "com.instagram.android:reels",
        packageName: String = "com.instagram.android",
        type: DetectionType = DetectionType.SELECTED_TAB,
        value: String = "com.instagram.android:id/reels_tab",
        activity: String? = null
    ) = DetectionRule(
        id = id,
        packageName = packageName,
        appName = "Instagram",
        sectionId = id.substringAfter(':'),
        sectionName = "Reels",
        detectionType = type,
        detectionValue = value,
        overlayTitle = "Reels is blocked",
        overlaySubtitle = "You set this rule.",
        activityValue = activity
    )

    private fun snapshot(
        packageName: String = "com.instagram.android",
        activityName: String? = "com.instagram.android.activity.MainTabActivity",
        ids: Set<String> = emptySet(),
        selected: Set<String> = setOf("com.instagram.android:id/reels_tab"),
        texts: Set<String> = emptySet()
    ) = WindowSnapshot(
        packageName = packageName,
        activityName = activityName,
        resourceIds = ids,
        selectedResourceIds = selected,
        texts = texts
    )

    @Test
    fun `an enabled selected-tab rule matches while that tab is open`() {
        val target = rule()

        val match = useCase(snapshot(), listOf(target), setOf(target.id))

        assertEquals(target, match)
    }

    @Test
    fun `the same rule does not match another app`() {
        val target = rule()

        val match = useCase(
            snapshot(packageName = "com.other.app"),
            listOf(target),
            setOf(target.id)
        )

        assertNull(match)
    }

    @Test
    fun `a disabled rule never matches`() {
        val target = rule()

        assertNull(useCase(snapshot(), listOf(target), enabledRuleIds = emptySet()))
    }

    @Test
    fun `the activity prerequisite gates the rule`() {
        val target = rule(activity = "com.instagram.android.activity.MainTabActivity")

        assertNull(
            useCase(
                snapshot(activityName = "com.instagram.android.activity.OtherActivity"),
                listOf(target),
                setOf(target.id)
            )
        )
    }

    @Test
    fun `a missing activity cannot satisfy an activity prerequisite`() {
        val target = rule(activity = "MainTabActivity")

        assertNull(useCase(snapshot(activityName = null), listOf(target), setOf(target.id)))
    }

    @Test
    fun `tab presence alone is not enough`() {
        val target = rule()
        // The Reels icon is visible on the feed, but the feed tab is selected.
        val feedOpen = snapshot(
            ids = setOf("com.instagram.android:id/reels_tab"),
            selected = setOf("com.instagram.android:id/home_tab")
        )

        assertNull(useCase(feedOpen, listOf(target), setOf(target.id)))
    }

    @Test
    fun `text rules ignore case`() {
        val target = rule(type = DetectionType.TEXT, value = "Reels")

        assertEquals(
            target,
            useCase(
                snapshot(selected = emptySet(), texts = setOf("REELS")),
                listOf(target),
                setOf(target.id)
            )
        )
    }

    @Test
    fun `resource id rules match presence anywhere in the tree`() {
        val target = rule(type = DetectionType.RESOURCE_ID, value = "com.instagram.android:id/clips_viewer")

        assertEquals(
            target,
            useCase(
                snapshot(selected = emptySet(), ids = setOf("com.instagram.android:id/clips_viewer")),
                listOf(target),
                setOf(target.id)
            )
        )
    }

    @Test
    fun `activity rules match a class name fragment`() {
        val target = rule(type = DetectionType.ACTIVITY, value = "MainTabActivity")

        assertEquals(
            target,
            useCase(snapshot(selected = emptySet()), listOf(target), setOf(target.id))
        )
    }

    @Test
    fun `the first matching rule wins`() {
        val reels = rule()
        val explore = rule(
            id = "com.instagram.android:explore",
            value = "com.instagram.android:id/search_tab"
        )
        val bothTabsSelected = snapshot(
            selected = setOf(
                "com.instagram.android:id/reels_tab",
                "com.instagram.android:id/search_tab"
            )
        )

        assertEquals(
            reels,
            useCase(bothTabsSelected, listOf(reels, explore), setOf(reels.id, explore.id))
        )
    }
}
