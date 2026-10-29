package com.sober.system.overlay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.theme.SoberTheme

/**
 * Covers the blocked section while leaving the rest of the app usable.
 *
 * There is deliberately no close affordance: the only exits are "go back" to
 * a safe tab or turning the rule off in Sober. The scrim is translucent so the
 * blocked screen is visibly there, and the platform blur (where the device
 * supports cross-window blur) keeps it unreadable without hiding it entirely.
 *
 * The accessibility guard reuses this screen with [footnote] naming what
 * turning the setting off would cost, so the copy lives at the call site
 * rather than being assembled here.
 */
@Composable
fun SectionBlockOverlayScreen(
    title: String,
    subtitle: String,
    footnote: String,
    onBack: () -> Unit
) {
    val colors = SoberTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.94f))
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .drawBehind {
                        // Drawn rather than a Material icon: this module does not
                        // depend on the icon artifact, and a shackle plus a body
                        // is the whole glyph.
                        val stroke = 3.dp.toPx()
                        val bodyWidth = size.width * 0.80f
                        val bodyTop = size.height * 0.44f
                        drawRoundRect(
                            color = colors.primary,
                            topLeft = Offset((size.width - bodyWidth) / 2f, bodyTop),
                            size = Size(bodyWidth, size.height * 0.44f),
                            cornerRadius = CornerRadius(8.dp.toPx())
                        )
                        drawArc(
                            color = colors.primary,
                            startAngle = 180f,
                            sweepAngle = 180f,
                            useCenter = false,
                            topLeft = Offset(size.width * 0.24f, size.height * 0.16f),
                            size = Size(size.width * 0.52f, size.height * 0.56f),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 14.sp,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            SoberButton(text = "Go back", onClick = onBack)

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = footnote,
                fontSize = 11.sp,
                color = colors.textMuted,
                textAlign = TextAlign.Center
            )
        }
    }
}
