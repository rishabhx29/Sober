package com.sober.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sober.core.ui.theme.SoberTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Two-stage end-session control: a five-second press to commit, then a
 * sixty-second window to change your mind.
 *
 * The hold is what makes the button honest — a single accidental tap cannot
 * end a session the user deliberately started — and the regret timer exists
 * because ending a session is the one irreversible action in the app.
 */
@Composable
fun SoberHoldToCancelButton(
    modifier: Modifier = Modifier,
    onCancelConfirmed: () -> Unit
) {
    var isHolding by remember { mutableStateOf(false) }
    var holdJob by remember { mutableStateOf<Job?>(null) }

    var isRegretTimerActive by remember { mutableStateOf(false) }
    var regretSecondsRemaining by remember { mutableIntStateOf(REGRET_SECONDS) }

    val scope = rememberCoroutineScope()

    val progress by animateFloatAsState(
        targetValue = if (isHolding) 1f else 0f,
        animationSpec = tween(if (isHolding) HOLD_MILLIS.toInt() else 300),
        label = "hold_progress"
    )

    if (isRegretTimerActive) {
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Cancellation requested",
                color = SoberTheme.colors.danger,
                style = SoberTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Session will end in ${regretSecondsRemaining}s",
                color = SoberTheme.colors.textSecondary,
                style = SoberTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    isRegretTimerActive = false
                    regretSecondsRemaining = REGRET_SECONDS
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SoberTheme.colors.primary,
                    contentColor = SoberTheme.colors.onPrimary
                )
            ) {
                Text("Return to focus", fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(SoberTheme.colors.danger.copy(alpha = 0.2f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            isHolding = true
                            holdJob = scope.launch {
                                delay(HOLD_MILLIS)
                                isHolding = false
                                isRegretTimerActive = true
                                regretSecondsRemaining = REGRET_SECONDS

                                while (regretSecondsRemaining > 0 && isRegretTimerActive) {
                                    delay(1_000)
                                    regretSecondsRemaining--
                                }

                                if (isRegretTimerActive) {
                                    onCancelConfirmed()
                                }
                            }
                            tryAwaitRelease()
                            isHolding = false
                            if (!isRegretTimerActive) {
                                holdJob?.cancel()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .height(52.dp)
                    .background(SoberTheme.colors.danger)
                    .align(Alignment.CenterStart)
            )

            Text(
                text = if (isHolding) "Hold to cancel..." else "End Session (Hold 5s)",
                color = if (progress > 0.5f) SoberTheme.colors.surface else SoberTheme.colors.danger,
                style = SoberTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private const val HOLD_MILLIS = 5_000L
private const val REGRET_SECONDS = 60
