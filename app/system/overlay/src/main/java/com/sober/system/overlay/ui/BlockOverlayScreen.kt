package com.sober.system.overlay.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.domain.usecase.BlockDecision
import com.sober.core.domain.usecase.BlockReason
import com.sober.core.domain.usecase.ManageEmergencyOverrideUseCase
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.random.Random

/**
 * What the override panel needs to know. Everything is already filtered for the
 * blocked package by the controller, so this carries no package ids.
 */
data class OverrideUi(
    val hasPin: Boolean,
    /** When the PIN gate was passed for this app, or null if not requested. */
    val requestedAt: Long? = null,
    /** Epoch millis the wait ends. */
    val readyAt: Long? = null
)

/**
 * The full-screen block screen rendered inside a `TYPE_APPLICATION_OVERLAY`
 * window.
 *
 * The main body deliberately has no back affordance and no close control: the
 * only way out is the system back gesture (which here dismisses nothing, since
 * the window is not focusable) or the app's own limit resetting. That is the
 * product intent — "you've had enough for today."
 *
 * The emergency override (PRD 5.3) does have an exit, but a slow one: press and
 * hold for ten seconds, then PIN, then a five-minute wait. Everything is entered
 * on the in-window keypad because the overlay window is not focusable and
 * cannot host an IME.
 *
 * Per the PRD the ring counts down to whatever actually releases the block:
 * midnight for daily limits, session end for a focus session. A daily-limit
 * block also carries one motivational line that changes with the date.
 */
@Composable
fun BlockOverlayScreen(
    decision: BlockDecision,
    appLabel: String,
    appIcon: Drawable? = null,
    override: OverrideUi = OverrideUi(hasPin = false),
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    onRequestOverride: () -> Unit = {},
    onCancelRequest: () -> Unit = {},
    onWaitElapsed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = SoberTheme.colors
    val isSessionBlock = decision.reason == BlockReason.FOCUS_SESSION

    // A ticking clock drives both countdown styles. Session blocks tick at
    // second resolution because their window is minutes, not hours.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(decision.reason, decision.sessionEndsAt) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    var overrideOpen by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        BlockBody(
            decision = decision,
            appLabel = appLabel,
            appIcon = appIcon,
            now = now,
            isSessionBlock = isSessionBlock
        )

        if (override.requestedAt != null) {
            OverrideWaitPanel(
                readyAt = override.readyAt,
                now = now,
                onCancel = onCancelRequest,
                onReady = onWaitElapsed
            )
        } else if (overrideOpen) {
            OverrideFlowPanel(
                hasPin = override.hasPin,
                onVerifyPin = onVerifyPin,
                onRequestOverride = {
                    overrideOpen = false
                    onRequestOverride()
                },
                onDismiss = { overrideOpen = false }
            )
        } else {
            OverrideAffordance(
                hasPin = override.hasPin,
                onOpen = { overrideOpen = true },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
        }
    }
}

/** The block message itself. Unchanged by the override flow. */
@Composable
private fun BlockBody(
    decision: BlockDecision,
    appLabel: String,
    appIcon: Drawable?,
    now: Long,
    isSessionBlock: Boolean
) {
    val colors = SoberTheme.colors

    val countdownLabel = when {
        isSessionBlock ->
            decision.sessionLabel?.takeIf { it.isNotBlank() } ?: "Focus session"
        decision.reason == BlockReason.SCHEDULE ->
            decision.scheduleName?.takeIf { it.isNotBlank() } ?: "Scheduled break"
        else -> "Back at midnight"
    }
    val countdownText = when {
        isSessionBlock ->
            // No end time means an open-ended session (PRD 2.6); "--:--" would
            // read as a broken timer where the truth is "until you decide".
            if (decision.sessionEndsAt == null) "Open-ended"
            else sessionRemainingText(decision.sessionEndsAt, now)

        decision.reason == BlockReason.SCHEDULE ->
            minutesUntilText(decision.scheduleEndsAt, now)

        else -> timeUntilMidnight(now)
    }
    val ringProgress = if (isSessionBlock) {
        sessionRemainingFraction(decision.sessionStartedAt, decision.sessionEndsAt, now)
    } else {
        null
    }

    // Wordmark, top-left, per spec.
    Text(
        text = "Sober",
        color = colors.textMuted,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(start = 20.dp, top = 24.dp)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AppIconRing(appLabel = appLabel, appIcon = appIcon)

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = headlineFor(decision, appLabel),
            color = colors.textPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = reasonFor(decision),
            color = colors.textSecondary,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(36.dp))

        // No ring for a rule with no clock behind it: this block ends when the
        // user changes their mind, not at a time, and "Back at midnight" would
        // be false — it outlives midnight.
        val hasCountdown = decision.reason != BlockReason.SECTION_RULE_UNAVAILABLE &&
            decision.reason != BlockReason.CUSTOM_APP_RULE
        if (hasCountdown) {
            CountdownRing(
                label = countdownLabel,
                timeText = countdownText,
                progress = ringProgress
            )

            if (!isSessionBlock) {
                Spacer(modifier = Modifier.height(32.dp))
                DailyQuote(quoteOfTheDay(now))
            }
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun OverrideAffordance(
    hasPin: Boolean,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SoberTheme.colors
    Text(
        text = if (hasPin) "Need an override?" else "Set a PIN in Sober to enable overrides.",
        color = colors.textMuted,
        fontSize = 11.sp,
        modifier = if (hasPin) {
            modifier
                .clip(RoundedCornerShape(28.dp))
                .clickable { onOpen() }
                .padding(horizontal = 20.dp, vertical = 12.dp)
        } else {
            modifier
        }
    )
}

/**
 * Step one of the override: hold, then prove you know the PIN, then answer a
 * sum. The sum is not security — it is a speed bump aimed at a distracted user,
 * while the PIN is what actually authorises the action.
 */
@Composable
private fun OverrideFlowPanel(
    hasPin: Boolean,
    onVerifyPin: (String, (Boolean) -> Unit) -> Unit,
    onRequestOverride: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = SoberTheme.colors
    var step by remember { mutableStateOf(OverrideStep.HOLD) }
    var pin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var mathInput by remember { mutableStateOf("") }
    var mathError by remember { mutableStateOf(false) }

    // Fixed for this panel instance: changing the sum while the user is typing
    // would make a correct answer look wrong.
    val problem = remember { Random.nextInt(2, 10) to Random.nextInt(2, 10) }

    OverridePanelScaffold(title = "Emergency override", onDismiss = onDismiss) {
        when (step) {
            OverrideStep.HOLD -> {
                PanelHint(
                    title = "This is deliberate",
                    body = "Press and hold for 10 seconds. Then confirm your PIN and " +
                        "answer a sum. The block lifts for 15 minutes only."
                )
                Spacer(modifier = Modifier.height(24.dp))
                HoldToContinueButton(
                    label = "Hold 10s to continue",
                    holdMillis = 10_000L,
                    onHeld = { step = OverrideStep.PIN }
                )
            }

            OverrideStep.PIN -> {
                PanelHint(
                    title = "Enter your PIN",
                    body = if (hasPin) {
                        "Required before an override can be granted."
                    } else {
                        "No PIN is set. Add one in Sober's settings first."
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
                SecretDots(length = pin.length)
                PinError(pinError)
                Spacer(modifier = Modifier.height(16.dp))
                NumberPad(
                    confirmLabel = "Continue",
                    confirmEnabled = hasPin && pin.length >= 4,
                    onDigit = {
                        if (pin.length < 6) {
                            pin += it
                            pinError = null
                        }
                    },
                    onBackspace = { pin = pin.dropLast(1) },
                    onConfirm = {
                        onVerifyPin(pin) { ok ->
                            if (ok) {
                                step = OverrideStep.MATH
                            } else {
                                pin = ""
                                pinError = "Incorrect PIN."
                            }
                        }
                    }
                )
            }

            OverrideStep.MATH -> {
                PanelHint(
                    title = "One more thing",
                    body = "Answer the sum to request the override."
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "${problem.first} + ${problem.second} = ?",
                    color = colors.textPrimary,
                    fontSize = 26.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = mathInput.ifEmpty { "–" },
                    color = if (mathError) colors.danger else colors.textPrimary,
                    fontSize = 30.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                if (mathError) {
                    Text(
                        text = "That's not right. Try again.",
                        color = colors.danger,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                NumberPad(
                    confirmLabel = "Request",
                    confirmEnabled = mathInput.isNotEmpty(),
                    onDigit = {
                        if (mathInput.length < 2) {
                            mathInput += it
                            mathError = false
                        }
                    },
                    onBackspace = { mathInput = mathInput.dropLast(1) },
                    onConfirm = {
                        if (mathInput.toIntOrNull() == problem.first + problem.second) {
                            onRequestOverride()
                        } else {
                            mathError = true
                            mathInput = ""
                        }
                    }
                )
            }
        }
    }
}

private enum class OverrideStep { HOLD, PIN, MATH }

/**
 * The wait after a successful PIN. When the countdown reaches zero the block
 * lifts on its own — that is what the wait is: friction, not another decision.
 */
@Composable
private fun OverrideWaitPanel(
    readyAt: Long?,
    now: Long,
    onCancel: () -> Unit,
    onReady: () -> Unit
) {
    val colors = SoberTheme.colors

    LaunchedEffect(readyAt) {
        val target = readyAt ?: return@LaunchedEffect
        val remaining = target - System.currentTimeMillis()
        if (remaining > 0) delay(remaining)
        onReady()
    }

    OverridePanelScaffold(title = "Override requested", onDismiss = onCancel) {
        PanelHint(
            title = "The block lifts in",
            body = "You passed the PIN gate. This wait is the point — it is long " +
                "enough to notice you are acting on impulse."
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = waitRemainingText(readyAt, now),
            color = colors.textPrimary,
            fontSize = 40.sp,
            fontFamily = JetBrainsMono,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Lifts for ${ManageEmergencyOverrideUseCase.GRANT_MINUTES} minutes, then " +
                "this screen comes back.",
            color = colors.textSecondary,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Cancel request",
            color = colors.danger,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .clickable { onCancel() }
                .padding(vertical = 12.dp)
        )
    }
}

/** Shared frame for the override panels: opaque, scrollable, dismissible. */
@Composable
private fun OverridePanelScaffold(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SoberTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = colors.textPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // A text glyph rather than a Material icon: this module does
                // not depend on the icon artifact, and one glyph is not worth
                // a new dependency in an overlay that ships with the engine.
                Text(
                    text = "Close",
                    color = colors.textMuted,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onDismiss() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            content()
        }
    }
}

@Composable
private fun PanelHint(title: String, body: String) {
    val colors = SoberTheme.colors
    Text(
        text = title,
        color = colors.textPrimary,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = body,
        color = colors.textSecondary,
        fontSize = 14.sp
    )
}

@Composable
private fun SecretDots(length: Int) {
    val colors = SoberTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(6) { index ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp)
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (index < length) colors.primary else colors.divider)
            )
        }
    }
}

@Composable
private fun PinError(message: String?) {
    if (message == null) return
    Text(
        text = message,
        color = SoberTheme.colors.danger,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 8.dp)
    )
}

/**
 * In-window keypad. A real text field cannot be used: this window is not
 * focusable, so the IME would never appear.
 */
@Composable
private fun NumberPad(
    confirmLabel: String,
    confirmEnabled: Boolean,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onConfirm: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        KEYPAD_ROWS.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { digit ->
                    PadKey(
                        text = digit,
                        onClick = { onDigit(digit) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PadKey(
                text = "⌫",
                onClick = onBackspace,
                modifier = Modifier.weight(1f)
            )
            PadKey(
                text = "0",
                onClick = { onDigit("0") },
                modifier = Modifier.weight(1f)
            )
            PadKey(
                text = "OK",
                onClick = onConfirm,
                enabled = confirmEnabled,
                primary = true,
                contentDescription = confirmLabel,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9")
)

@Composable
private fun PadKey(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    primary: Boolean = false,
    contentDescription: String? = null
) {
    val colors = SoberTheme.colors
    val container = when {
        primary && enabled -> colors.primary
        primary -> colors.primary.copy(alpha = 0.3f)
        else -> colors.surface
    }
    val content = when {
        primary -> colors.onPrimary
        else -> colors.textPrimary
    }
    Box(
        modifier = modifier
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(container)
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = content,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Ten-second press. Progress fills as a bar, and the action only fires if the
 * finger is still down when the bar completes — releasing early cancels it.
 */
@Composable
private fun HoldToContinueButton(label: String, holdMillis: Long, onHeld: () -> Unit) {
    val colors = SoberTheme.colors
    var progress by remember { mutableFloatStateOf(0f) }
    var holding by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(colors.primary.copy(alpha = 0.15f))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        val startedAt = System.currentTimeMillis()
                        val job = scope.launch {
                            while (System.currentTimeMillis() - startedAt < holdMillis) {
                                progress = ((System.currentTimeMillis() - startedAt).toFloat() /
                                    holdMillis).coerceIn(0f, 1f)
                                delay(16)
                            }
                            progress = 1f
                            holding = false
                            onHeld()
                        }
                        tryAwaitRelease()
                        holding = false
                        job.cancel()
                        progress = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress)
                .fillMaxHeight()
                .background(colors.primary)
                .align(Alignment.CenterStart)
        )
        Text(
            text = if (holding) "Keep holding…" else label,
            color = if (progress > 0.5f) colors.onPrimary else colors.primary,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AppIconRing(appLabel: String, appIcon: Drawable?) {
    val colors = SoberTheme.colors
    Box(
        modifier = Modifier.size(64.dp),
        contentAlignment = Alignment.Center
    ) {
        // 2dp danger ring around the icon, per spec.
        Box(
            modifier = Modifier
                .size(64.dp)
                .drawBehind {
                    drawCircle(
                        color = colors.danger,
                        radius = size.minDimension / 2 - 2.dp.toPx(),
                        style = Stroke(width = 2.dp.toPx())
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (appIcon != null) {
                val bitmap = remember(appIcon) { appIcon.toSquareBitmap(ICON_BITMAP_PX) }
                Image(
                    bitmap = bitmap,
                    contentDescription = null,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                )
            } else {
                // Letters only when the icon cannot be loaded at all.
                Text(
                    text = appLabel.take(1).uppercase(),
                    color = colors.textPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Rasterises a [Drawable] once per overlay so Compose can draw it.
 *
 * The overlay window has no ImageLoader (Coil is an app-side dependency), and
 * vector/adaptive drawables cannot be drawn directly by Compose, so a small
 * bitmap is the cheapest thing that works for every icon type.
 */
private fun Drawable.toSquareBitmap(sizePx: Int): ImageBitmap {
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val previousBounds = bounds
    setBounds(0, 0, sizePx, sizePx)
    draw(canvas)
    bounds = previousBounds
    return bitmap.asImageBitmap()
}

private const val ICON_BITMAP_PX = 160

/**
 * @param progress remaining fraction for a timed block, or null for a block
 * that only releases at midnight — a full static ring in that case, because a
 * "time remaining until midnight" arc would imply a progress the user cannot
 * act on.
 */
@Composable
private fun CountdownRing(label: String, timeText: String, progress: Float?) {
    val colors = SoberTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(132.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .drawBehind {
                        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                        val inset = stroke.width / 2
                        val arcSize = Size(size.width - stroke.width, size.height - stroke.width)
                        drawArc(
                            color = colors.divider,
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = stroke
                        )
                        drawArc(
                            color = colors.primary,
                            startAngle = -90f,
                            sweepAngle = 360f * (progress ?: 1f),
                            useCenter = false,
                            topLeft = Offset(inset, inset),
                            size = arcSize,
                            style = stroke
                        )
                    }
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    color = colors.textSecondary,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = timeText,
                    color = colors.textPrimary,
                    fontSize = 16.sp,
                    fontFamily = JetBrainsMono,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun DailyQuote(quote: Quote) {
    val colors = SoberTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "“${quote.text}”",
            color = colors.textMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = quote.author,
            color = colors.textMuted,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}

internal data class Quote(val text: String, val author: String)

private val QUOTES = listOf(
    Quote("It is not that we have a short time to live, but that we waste a lot of it.", "Seneca"),
    Quote("Tell me what you pay attention to and I will tell you who you are.", "José Ortega y Gasset"),
    Quote("The successful warrior is the average man, with laser-like focus.", "Bruce Lee"),
    Quote("Concentrate all your thoughts upon the work at hand.", "Alexander Graham Bell"),
    Quote("You can do anything, but not everything.", "David Allen"),
    Quote("The shorter way to do many things is to do only one thing at a time.", "Mozart"),
    Quote("Do the hard jobs first. The easy jobs will take care of themselves.", "Dale Carnegie"),
    Quote("He who has a why to live can bear almost any how.", "Friedrich Nietzsche"),
    Quote("Be not afraid of going slowly, be afraid only of standing still.", "Chinese proverb"),
    Quote("Simplicity is the ultimate sophistication.", "Leonardo da Vinci"),
    Quote("First we form habits, then they form us.", "Rob Gilbert"),
    Quote("The ability to simplify means to eliminate the unnecessary.", "Hans Hofmann")
)

/**
 * Picks the quote for the current date rather than at random, so the line is
 * stable while a block is on screen and changes once a day as the PRD asks.
 */
internal fun quoteOfTheDay(nowMillis: Long): Quote {
    val dayOfYear = Calendar.getInstance().apply {
        timeInMillis = nowMillis
    }.get(Calendar.DAY_OF_YEAR)
    return QUOTES[dayOfYear % QUOTES.size]
}

/**
 * Remaining wait as MM:SS, floored at zero. Shared with the ring formatter's
 * style rather than a new one so the two never disagree about padding.
 */
internal fun waitRemainingText(readyAt: Long?, nowMillis: Long): String {
    if (readyAt == null) return "--:--"
    val totalSeconds = ((readyAt - nowMillis) / 1_000L).coerceAtLeast(0)
    return String.format("%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}

private fun headlineFor(decision: BlockDecision, appLabel: String): String {
    return when (decision.reason) {
        BlockReason.FOCUS_SESSION -> "You're in a focus session."
        BlockReason.SCHEDULE ->
            "${decision.scheduleName?.takeIf { it.isNotBlank() } ?: "A scheduled break"} is on."
        BlockReason.SECTION_RULE_UNAVAILABLE -> "Section blocking is paused."
        BlockReason.CUSTOM_APP_RULE -> "$appLabel is closed."
        else -> "You've had enough $appLabel for today."
    }
}

private fun reasonFor(decision: BlockDecision): String = when (decision.reason) {
    BlockReason.APP_DAILY_LIMIT ->
        "${decision.usedMinutes} min used of your ${decision.limitMinutes} min daily limit."
    BlockReason.OVERALL_DAILY_LIMIT ->
        "${decision.usedMinutes} min used of your ${decision.limitMinutes} min daily screen time."
    BlockReason.FOCUS_SESSION ->
        if (decision.sessionEndsAt == null) {
            "This app is blocked until you end the session."
        } else {
            "This app is blocked until your session ends."
        }
    BlockReason.SCHEDULE ->
        decision.scheduleEndsAt?.let { endsAt ->
            "This app is blocked until ${timeOfDayText(endsAt)}."
        } ?: "This app is blocked during its scheduled window."
    // Says both facts plainly: which rule is being honoured, and what would
    // open the rest of the app again. There is deliberately no countdown — the
    // block ends when access is granted, not when a timer runs out.
    BlockReason.SECTION_RULE_UNAVAILABLE ->
        "You blocked a section of this app, but Sober's Accessibility access is " +
            "off. Until it is back on, the whole app stays blocked."
    BlockReason.CUSTOM_APP_RULE ->
        "You added it to your blocked apps in Sober. Remove it there to open it again."
}

/** "6:30 PM" — the wall-clock time a schedule window lifts. */
private fun timeOfDayText(epochMillis: Long): String {
    val at = Calendar.getInstance().apply { timeInMillis = epochMillis }
    val hour24 = at.get(Calendar.HOUR_OF_DAY)
    val hour12 = when (val h = hour24 % 12) {
        0 -> 12
        else -> h
    }
    val suffix = if (hour24 < 12) "AM" else "PM"
    return String.format("%d:%02d %s", hour12, at.get(Calendar.MINUTE), suffix)
}

/** HH:MM left in a schedule window, floored at zero. */
private fun minutesUntilText(endsAt: Long?, nowMillis: Long): String {
    if (endsAt == null) return "--:--"
    val totalMinutes = ((endsAt - nowMillis) / 60_000L).coerceAtLeast(0)
    return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60)
}

/** Formats the countdown to the next midnight as HH:MM. */
private fun timeUntilMidnight(nowMillis: Long): String {
    val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val midnight = (now.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val totalMinutes = ((midnight.timeInMillis - now.timeInMillis) / 60_000L).coerceAtLeast(0)
    return String.format("%02d:%02d", totalMinutes / 60, totalMinutes % 60)
}

/** MM:SS under an hour, H:MM:SS above it — a session timer is read to the second. */
internal fun sessionRemainingText(endsAt: Long?, nowMillis: Long): String {
    if (endsAt == null) return "--:--"
    val totalSeconds = ((endsAt - nowMillis) / 1_000L).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Remaining slice of the session window, clamped to 0..1. Returns null when the
 * window is unknown so the ring falls back to the static full circle.
 */
internal fun sessionRemainingFraction(startedAt: Long?, endsAt: Long?, nowMillis: Long): Float? {
    if (startedAt == null || endsAt == null || endsAt <= startedAt) return null
    val total = (endsAt - startedAt).toFloat()
    val remaining = (endsAt - nowMillis).toFloat()
    return (remaining / total).coerceIn(0f, 1f)
}
