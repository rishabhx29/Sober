package com.sober.feature.apps

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sober.core.ui.theme.SoberTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The row carries two controls on one line: the row itself opens the detail
 * sheet, and the switch flips the limit without opening it. Tapping the wrong
 * layer is an easy regression, so both paths are checked here. Tests run on the
 * JVM through Robolectric; there is no device in this environment.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppRowTest {

    @get:Rule
    val compose = createComposeRule()

    private fun app(
        name: String = "Instagram",
        usageMinutes: Int = 30,
        limitMinutes: Int? = 60,
        isLimited: Boolean = true
    ) = InstalledApp(
        name = name,
        packageName = "com.example.${name.lowercase()}",
        icon = null,
        usageMinutes = usageMinutes,
        limitMinutes = limitMinutes,
        isLimited = isLimited
    )

    @Test
    fun `active limit renders a checked switch`() {
        compose.setContent {
            SoberTheme {
                AppRow(app = app(isLimited = true), onClick = {}, onLimitToggle = {})
            }
        }

        compose.onNodeWithContentDescription("Limit for Instagram")
            .assertIsDisplayed()
            .assertIsOn()
        compose.onNodeWithText("60m").assertIsDisplayed()
    }

    @Test
    fun `switched off limit renders an unchecked switch`() {
        compose.setContent {
            SoberTheme {
                AppRow(app = app(isLimited = false), onClick = {}, onLimitToggle = {})
            }
        }

        compose.onNodeWithContentDescription("Limit for Instagram")
            .assertIsOff()
    }

    @Test
    fun `flipping the switch reports the new state and does not open the sheet`() {
        var toggle: Boolean? = null
        var rowClicks = 0

        compose.setContent {
            SoberTheme {
                AppRow(
                    app = app(isLimited = true),
                    onClick = { rowClicks++ },
                    onLimitToggle = { toggle = it }
                )
            }
        }

        compose.onNodeWithContentDescription("Limit for Instagram").performClick()

        assertEquals(false, toggle)
        assertEquals(0, rowClicks)
    }

    @Test
    fun `tapping the row opens the sheet without touching the limit`() {
        var rowClicks = 0

        compose.setContent {
            SoberTheme {
                AppRow(app = app(), onClick = { rowClicks++ }, onLimitToggle = {})
            }
        }

        compose.onNodeWithText("Instagram").performClick()

        assertEquals(1, rowClicks)
    }

    @Test
    fun `an app without a limit has no switch`() {
        compose.setContent {
            SoberTheme {
                AppRow(
                    app = app(limitMinutes = null, isLimited = false),
                    onClick = {},
                    onLimitToggle = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Limit for Instagram").assertDoesNotExist()
    }

    @Test
    fun `a limit that has not been reached shows no blocked marker`() {
        compose.setContent {
            SoberTheme {
                AppRow(
                    app = app(usageMinutes = 30, limitMinutes = 60),
                    onClick = {},
                    onLimitToggle = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Blocked").assertDoesNotExist()
    }

    @Test
    fun `a reached limit shows the blocked marker`() {
        compose.setContent {
            SoberTheme {
                AppRow(
                    app = app(usageMinutes = 60, limitMinutes = 60),
                    onClick = {},
                    onLimitToggle = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Blocked").assertIsDisplayed()
    }

    @Test
    fun `a switched off limit never shows the blocked marker`() {
        compose.setContent {
            SoberTheme {
                AppRow(
                    app = app(usageMinutes = 90, limitMinutes = 60, isLimited = false),
                    onClick = {},
                    onLimitToggle = {}
                )
            }
        }

        compose.onNodeWithContentDescription("Blocked").assertDoesNotExist()
    }

    @Test
    fun `an app with no limit still shows its usage`() {
        compose.setContent {
            SoberTheme {
                AppRow(
                    app = app(usageMinutes = 12, limitMinutes = null, isLimited = false),
                    onClick = {},
                    onLimitToggle = {}
                )
            }
        }

        compose.onNodeWithText("12m today").assertIsDisplayed()
    }
}
