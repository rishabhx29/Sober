package com.sober.system.overlay.ui

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.components.SoberButton
import com.sober.core.ui.components.SoberOutlinedButton
import com.sober.core.ui.components.SoberSectionLabel
import com.sober.core.ui.components.rememberBreathingScale
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle
import kotlinx.coroutines.delay

/**
 * The "take a breath" screen shown before a flagged app opens.
 *
 * It deliberately offers no dismiss by gesture: the window is not focusable, so
 * back does nothing, and the two buttons are the only exits. The countdown is
 * wall-clock based, so a dropped frame or a poked process cannot shorten it.
 */
@Composable
fun PauseOverlayScreen(
    appLabel: String,
    delaySeconds: Int,
    passMinutes: Int,
    onOpen: () -> Unit,
    onClose: () -> Unit
) {
    val colors = SoberTheme.colors
    val totalMillis = delaySeconds * 1_000L
    var remainingMillis by remember { mutableLongStateOf(totalMillis) }

    LaunchedEffect(delaySeconds) {
        val deadline = SystemClock.elapsedRealtime() + totalMillis
        while (true) {
            val left = deadline - SystemClock.elapsedRealtime()
            remainingMillis = left.coerceAtLeast(0L)
            if (left <= 0L) break
            delay(100)
        }
    }

    val ready = remainingMillis <= 0L
    val secondsLeft = ((remainingMillis + 999L) / 1_000L).toInt()
    val breathe = rememberBreathingScale(minScale = 1f, maxScale = 1.05f, periodMillis = 4_000)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            SoberSectionLabel(text = "Pause before opening")
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = appLabel,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            if (!ready) {
                Box(
                    modifier = Modifier
                        .size(168.dp)
                        .graphicsLayer {
                            scaleX = breathe
                            scaleY = breathe
                        }
                        .semantics(mergeDescendants = true) {
                            contentDescription =
                                "Pause. $secondsLeft seconds before you can choose to open."
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(168.dp)) {
                        val stroke = 10.dp.toPx()
                        val radius = (size.minDimension - stroke) / 2
                        val topLeft = Offset(
                            (size.width - 2 * radius) / 2,
                            (size.height - 2 * radius) / 2
                        )
                        drawArc(
                            color = colors.divider,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = colors.primary,
                            startAngle = -90f,
                            sweepAngle = 360f * (remainingMillis.toFloat() / totalMillis),
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(radius * 2, radius * 2),
                            style = Stroke(width = stroke, cap = StrokeCap.Round)
                        )
                    }
                    Text(
                        text = "$secondsLeft",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = JetBrainsMono,
                        style = TabularNumberStyle,
                        color = colors.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Breathe. The app is not going anywhere.",
                    fontSize = 15.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "Still want to open $appLabel?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        SoberButton(
                            text = "Open for $passMinutes min",
                            onClick = onOpen
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SoberOutlinedButton(
                            text = "Not now — go back",
                            onClick = onClose
                        )
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Opening again before the pass ends skips this pause.",
                    fontSize = 12.sp,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
