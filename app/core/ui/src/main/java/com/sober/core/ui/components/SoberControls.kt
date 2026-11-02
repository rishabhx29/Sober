package com.sober.core.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.SoberTheme

/**
 * Thin usage bar: navy fill on a cream track, 4dp tall by default.
 *
 * `isDanger` is the only place red appears for usage states, per the Manuscript
 * rule that signal red shows up at most once per screen.
 */
@Composable
fun SoberProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    height: Dp = 4.dp,
    contentDescription: String? = null
) {
    val colors = SoberTheme.colors
    val fraction = progress.coerceIn(0f, 1f)
    val shape = RoundedCornerShape(height / 2)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "progress-fill"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(shape)
            .background(colors.divider)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        progressBarRangeInfo =
                            ProgressBarRangeInfo(fraction, 0f..1f, 0)
                    }
                } else {
                    // Decorative bars would otherwise each become a "0 percent"
                    // stop in TalkBack; the row text already carries the number.
                    Modifier.clearAndSetSemantics {}
                }
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedFraction)
                .fillMaxHeight()
                .background(if (isDanger) colors.danger else colors.primary)
        )
    }
}

/**
 * Navy-filled selected segment on a cream group, matching the duration and
 * timeframe controls. Generic over the option type so callers keep their own
 * enums instead of mapping to strings and back.
 */
@Composable
fun <T> SoberSegmentedControl(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String = { it.toString() }
) {
    val colors = SoberTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.background)
            .padding(3.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(if (isSelected) colors.primary else Color.Transparent)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(option) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label(option),
                    color = if (isSelected) colors.onPrimary else colors.primary,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Navy track when on, muted grey when off. No third colour state.
 *
 * Pass a null [onCheckedChange] when the whole row is the switch target: the
 * row carries the toggleable semantics, so the switch itself is removed from the
 * accessibility tree instead of becoming a second, smaller duplicate stop.
 */
@Composable
fun SoberToggleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = SoberTheme.colors

    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier.then(
            if (onCheckedChange == null) {
                Modifier.clearAndSetSemantics {}
            } else {
                Modifier
            }
        ),
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = colors.onPrimary,
            checkedTrackColor = colors.primary,
            checkedBorderColor = colors.primary,
            uncheckedThumbColor = colors.textMuted,
            uncheckedTrackColor = colors.divider,
            uncheckedBorderColor = Color.Transparent
        )
    )
}

/** A stacked label/value/track column used by usage rows across screens. */
@Composable
fun SoberUsageRow(
    name: String,
    detail: String,
    progress: Float,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false
) {
    val colors = SoberTheme.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = name,
                color = colors.primary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.widthIn(max = 200.dp)
            )
            Text(
                text = detail,
                color = if (isDanger) colors.danger else colors.textSecondary,
                fontSize = 13.sp
            )
        }
        Box(modifier = Modifier.height(6.dp))
        SoberProgressBar(progress = progress, isDanger = isDanger)
    }
}
