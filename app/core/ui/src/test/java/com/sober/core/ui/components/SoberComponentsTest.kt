package com.sober.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
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
 * The design-system components are the only UI every screen shares, so a
 * regression here is a regression everywhere. Compose tests run on the JVM via
 * Robolectric, which means they can execute in this environment instead of
 * waiting for a device.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SoberComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `primary button shows its label and reports a click`() {
        var clicks = 0

        compose.setContent {
            SoberTheme {
                SoberButton(text = "Start Session", onClick = { clicks++ })
            }
        }

        compose.onNodeWithText("Start Session")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `disabled primary button does not fire`() {
        var clicks = 0

        compose.setContent {
            SoberTheme {
                SoberButton(text = "Start Session", onClick = { clicks++ }, enabled = false)
            }
        }

        compose.onNodeWithText("Start Session").assertIsNotEnabled()

        assertEquals(0, clicks)
    }

    @Test
    fun `outlined button reports a click`() {
        var clicks = 0

        compose.setContent {
            SoberTheme {
                SoberOutlinedButton(text = "Not now", onClick = { clicks++ })
            }
        }

        compose.onNodeWithText("Not now")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `text link reports a click`() {
        var clicks = 0

        compose.setContent {
            SoberTheme {
                SoberTextLink(text = "Privacy Policy", onClick = { clicks++ })
            }
        }

        compose.onNodeWithText("Privacy Policy")
            .assertIsDisplayed()
            .performClick()

        assertEquals(1, clicks)
    }

    @Test
    fun `status row shows both its label and its state`() {
        compose.setContent {
            SoberTheme {
                SoberStatusRow(
                    label = "Accessibility",
                    statusText = "Granted",
                    isPositive = true
                )
            }
        }

        compose.onNodeWithText("Accessibility").assertIsDisplayed()
        compose.onNodeWithText("Granted").assertIsDisplayed()
    }

    @Test
    fun `section label renders uppercase`() {
        compose.setContent {
            SoberTheme {
                SoberSectionLabel(text = "Notifications")
            }
        }

        compose.onNodeWithText("NOTIFICATIONS").assertIsDisplayed()
    }

    @Test
    fun `card renders its content`() {
        compose.setContent {
            SoberTheme {
                SoberCard {
                    androidx.compose.material3.Text("Sober Premium")
                }
            }
        }

        compose.onNodeWithText("Sober Premium").assertIsDisplayed()
    }
}
