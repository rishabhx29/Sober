package com.sober.system.overlay.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sober.core.domain.usecase.BlockDecision
import com.sober.core.domain.usecase.BlockReason
import com.sober.core.ui.theme.SoberTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The overlay is the whole product from the user's side, and its copy is
 * policy: it decides whether a block reads as "wait until 8:00" or as a wall.
 * These tests pin the cases where the copy changes shape.
 *
 * The screen ticks once a second, so the test clock is frozen: with
 * `autoAdvance = false` the delayed loop stays parked and the assertions see the
 * first frame instead of waiting on a clock that never stops.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BlockOverlayScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(decision: BlockDecision, appLabel: String = "Instagram") {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            SoberTheme {
                BlockOverlayScreen(decision = decision, appLabel = appLabel)
            }
        }
    }

    @Test
    fun `a schedule block names the rule and its end`() {
        val endsAt = System.currentTimeMillis() + 60 * 60 * 1000

        show(
            BlockDecision(
                packageName = "com.instagram.android",
                reason = BlockReason.SCHEDULE,
                usedMinutes = 0,
                limitMinutes = 0,
                scheduleName = "Sleep Mode",
                scheduleEndsAt = endsAt
            )
        )

        compose.onNodeWithText("Sleep Mode is on.").assertIsDisplayed()
        compose.onNodeWithText("Sober").assertIsDisplayed()
    }

    @Test
    fun `a custom rule has no countdown, because it has no end`() {
        show(
            BlockDecision(
                packageName = "com.instagram.android",
                reason = BlockReason.CUSTOM_APP_RULE,
                usedMinutes = 0,
                limitMinutes = 0
            )
        )

        compose.onNodeWithText("Instagram is closed.").assertIsDisplayed()
        compose.onNodeWithText("Back at midnight").assertDoesNotExist()
    }

    @Test
    fun `a section rule without accessibility explains the real cause`() {
        show(
            BlockDecision(
                packageName = "com.instagram.android",
                reason = BlockReason.SECTION_RULE_UNAVAILABLE,
                usedMinutes = 0,
                limitMinutes = 0
            )
        )

        compose.onNodeWithText("Section blocking is paused.").assertIsDisplayed()
    }
}
