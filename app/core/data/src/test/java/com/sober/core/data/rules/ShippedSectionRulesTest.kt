package com.sober.core.data.rules

import com.sober.core.domain.model.DetectionRule
import com.sober.core.domain.model.WindowSnapshot
import com.sober.core.domain.usecase.DetectBlockedSectionUseCase
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Runs the catalog that actually ships through the matcher.
 *
 * The parser tests prove the file is well-formed; these prove the rules behave
 * as rules. Because detection is data, a typo in an id never fails a build and
 * never crashes the app -- it just silently stops blocking -- so the shipped
 * document needs its own coverage.
 *
 * What this cannot prove is that the ids are the ones the current app builds
 * use. That needs the apps installed on a device.
 */
class ShippedSectionRulesTest {

    private val catalog = DetectionRuleCatalogParser.parse(
        Buffer().writeUtf8(File("src/main/assets/detection_rules.json").readText())
    )

    private val useCase = DetectBlockedSectionUseCase()

    private fun snapshotFor(rule: DetectionRule) = WindowSnapshot(
        packageName = rule.packageName,
        activityName = rule.activityValue ?: "${rule.packageName}.MainActivity",
        resourceIds = setOf(rule.detectionValue),
        selectedResourceIds = setOf(rule.detectionValue),
        texts = emptySet()
    )

    @Test
    fun `every shipped rule matches the section it describes`() {
        catalog.rules.forEach { rule ->
            val match = useCase(snapshotFor(rule), catalog.rules, setOf(rule.id))

            assertEquals("rule ${rule.id} did not match its own snapshot", rule, match)
        }
    }

    @Test
    fun `no shipped rule matches another app`() {
        catalog.rules.forEach { rule ->
            catalog.rules
                .map { it.packageName }
                .filter { it != rule.packageName }
                .forEach { otherPackage ->
                    val foreign = snapshotFor(rule).copy(packageName = otherPackage)

                    assertNull(
                        "${rule.id} matched inside $otherPackage",
                        useCase(foreign, catalog.rules, setOf(rule.id))
                    )
                }
        }
    }

    @Test
    fun `a tab that is visible but not selected does not match`() {
        catalog.rules.forEach { rule ->
            val feedOpen = snapshotFor(rule).copy(
                selectedResourceIds = setOf("${rule.packageName}:id/some_other_tab")
            )

            assertNull(
                "${rule.id} matched while a different tab was selected",
                useCase(feedOpen, catalog.rules, setOf(rule.id))
            )
        }
    }

    @Test
    fun `the four post-launch apps each ship a blocking rule`() {
        val postLaunch = setOf(
            "com.zhiliaoapp.musically",
            "com.twitter.android",
            "com.reddit.frontpage",
            "com.snapchat.android"
        )

        assertEquals(
            postLaunch,
            catalog.rules.map { it.packageName }.filter { it in postLaunch }.toSet()
        )
        catalog.rules.filter { it.packageName in postLaunch }.forEach { rule ->
            assertEquals(
                "${rule.packageName}:${rule.sectionId}",
                rule.id
            )
            assertTrue(rule.overlayTitle.isNotBlank())
            assertTrue(rule.overlaySubtitle.isNotBlank())
        }
    }

    @Test
    fun `rule ids are unique`() {
        val ids = catalog.rules.map { it.id }

        assertEquals(ids.size, ids.toSet().size)
    }
}
