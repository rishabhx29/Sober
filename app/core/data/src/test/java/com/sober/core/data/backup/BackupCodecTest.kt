package com.sober.core.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val codec = BackupCodec()

    @Test
    fun `a document survives a round trip`() {
        val document = BackupDocument(
            createdAt = 1_760_000_000_000L,
            appLimits = listOf(
                BackupAppLimit(
                    packageName = "com.instagram.android",
                    appName = "Instagram",
                    dailyLimitMinutes = 30,
                    blockedSections = listOf("reels"),
                    dailyOpenLimit = 3
                )
            ),
            scheduleRules = listOf(
                BackupScheduleRule(
                    ruleName = "Work",
                    blockedPackages = listOf("com.instagram.android"),
                    daysOfWeek = listOf(1, 2, 3, 4, 5),
                    startTime = "09:00",
                    endTime = "17:00"
                )
            ),
            customRules = listOf(
                BackupCustomRule(type = "BLOCKED_WEBSITE", value = "youtube.com", enabled = true)
            ),
            sectionRules = listOf("instagram_reels"),
            customSectionRules = listOf(
                BackupSectionRule(
                    packageName = "com.reddit.frontpage",
                    appName = "Reddit",
                    sectionName = "Popular",
                    detectionType = "SELECTED_TAB",
                    detectionValue = "com.reddit.frontpage:id/nav_popular",
                    overlayTitle = "Popular is blocked",
                    overlaySubtitle = "The rest of Reddit still works.",
                    activityValue = "com.reddit.frontpage.MainActivity",
                    enabled = false
                ),
                BackupSectionRule(
                    packageName = "com.reddit.frontpage",
                    appName = "Reddit",
                    sectionName = "Search",
                    detectionType = "ACTIVITY",
                    detectionValue = "com.reddit.frontpage.SearchActivity"
                )
            ),
            pauseRules = listOf(BackupPauseRule("com.zhiliaoapp.musically", 10)),
            categoryLimits = listOf(BackupCategoryLimit("SOCIAL", 45)),
            allowOnly = BackupAllowOnly(enabled = true, allowedPackages = listOf("com.android.dialer")),
            bedtime = BackupBedtime(
                enabled = true,
                startMinuteOfDay = 1320,
                endMinuteOfDay = 420,
                allowedPackages = listOf("com.android.dialer")
            ),
            dailyCap = BackupDailyCap(minutes = 120, exemptPackages = listOf("com.google.android.apps.maps"))
        )

        val decoded = codec.decode(codec.encode(document))

        assertEquals(document, decoded)
    }

    @Test
    fun `missing sections decode to empty defaults`() {
        val decoded = codec.decode("""{"format":"sober-backup","version":1}""")

        assertTrue(decoded.appLimits.isEmpty())
        assertTrue(decoded.scheduleRules.isEmpty())
        assertTrue(decoded.customRules.isEmpty())
        assertTrue(decoded.customSectionRules.isEmpty())
        assertTrue(decoded.categoryLimits.isEmpty())
        assertEquals(null, decoded.allowOnly)
        assertEquals(null, decoded.bedtime)
        assertEquals(null, decoded.dailyCap)
    }

    @Test
    fun `a version 1 file still decodes without recorded sections`() {
        val decoded = codec.decode(
            """{"format":"sober-backup","version":1,"sectionRules":["instagram_reels"]}"""
        )

        assertEquals(2, BackupDocument.CURRENT_VERSION)
        assertEquals(listOf("instagram_reels"), decoded.sectionRules)
        assertTrue(decoded.customSectionRules.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a foreign file is rejected`() {
        codec.decode("""{"format":"something-else","version":1}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a file from a newer version is rejected`() {
        codec.decode("""{"format":"sober-backup","version":3}""")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `a file that is not JSON is rejected`() {
        codec.decode("not a backup")
    }
}
