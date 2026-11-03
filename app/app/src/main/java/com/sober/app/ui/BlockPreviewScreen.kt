package com.sober.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.domain.usecase.BlockDecision
import com.sober.core.domain.usecase.BlockReason
import com.sober.core.ui.components.SoberQuietButton
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.system.overlay.ui.BlockOverlayScreen
import com.sober.system.overlay.ui.OverrideUi

/**
 * Renders the real block screen inside the app for the health check.
 *
 * This is the same composable the overlay service attaches over a blocked app,
 * so it proves the layout and the copy, not the window permission — the health
 * check reports that separately. Actions are inert: the banner above the
 * preview and the always-visible Close mean the screen cannot pretend to be a
 * real block.
 */
@Composable
fun BlockPreviewScreen(onClose: () -> Unit) {
    val colors = SoberTheme.colors
    val decision = remember {
        BlockDecision(
            packageName = "com.instagram.android",
            reason = BlockReason.APP_DAILY_LIMIT,
            usedMinutes = 90,
            limitMinutes = 60
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "PREVIEW",
                    fontFamily = JetBrainsMono,
                    fontSize = 10.sp,
                    letterSpacing = 1.4.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.accent
                )
                Text(
                    text = "Nothing is blocked",
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
            SoberQuietButton(text = "Close", onClick = onClose)
        }

        Box(modifier = Modifier.weight(1f)) {
            BlockOverlayScreen(
                decision = decision,
                appLabel = "Instagram",
                override = OverrideUi(hasPin = false),
                // Inert on purpose: a preview must not start an override wait
                // or look like it granted anything.
                onRequestOverride = {},
                onCancelRequest = {},
                onWaitElapsed = {}
            )
        }
    }
}
