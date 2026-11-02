package com.sober.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.JetBrainsMono
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.TabularNumberStyle

/**
 * The top of every tab: an optional mono eyebrow, a large tight headline and an
 * optional supporting line. The size jump between 11sp eyebrow and 32sp title
 * is what gives the screens their editorial rhythm; the old 22sp headers sat
 * too close to body copy to read as page titles.
 */
@Composable
fun SoberScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = SoberTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(modifier = Modifier.weight(1f)) {
            if (eyebrow != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(colors.accent)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = eyebrow.uppercase(),
                        fontFamily = JetBrainsMono,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = title,
                fontSize = 32.sp,
                lineHeight = 36.sp,
                letterSpacing = (-0.8).sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = colors.textSecondary
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Section heading with an option to number it. The rule to the right fills the
 * leftover width, so short and long labels both reach the screen edge and the
 * page reads as a set of aligned editorial blocks instead of floating labels.
 */
@Composable
fun SoberSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    index: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = SoberTheme.colors

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (index != null) {
            Text(
                text = index,
                fontFamily = JetBrainsMono,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = colors.accent
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary,
            modifier = Modifier.semantics { heading() }
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(colors.divider)
        )
        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}

/**
 * One hairline row for lists that used to be cards. Grouped open rows read as a
 * single ledger, which keeps long screens (Settings, Apps) from turning into a
 * stack of nested outlined boxes.
 */
@Composable
fun SoberListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    meta: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val colors = SoberTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.width(14.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = colors.textSecondary
                )
            }
        }
        if (meta != null) {
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = meta,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary,
                style = TabularNumberStyle
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(10.dp))
            trailing()
        }
        if (showChevron) {
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Pill filters with optional counts. Scrolling horizontally rather than
 * splitting the width keeps labels readable and lets a fourth or fifth option
 * exist without every chip shrinking to an ellipsis.
 */
@Composable
fun <T> SoberFilterChips(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    label: (T) -> String,
    count: (T) -> Int? = { null }
) {
    val colors = SoberTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            val countValue = count(option)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isSelected) colors.primary else colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.primary else colors.cardBorder,
                        shape = RoundedCornerShape(percent = 50)
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.Tab,
                        onClick = { onSelect(option) }
                    )
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label(option),
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (isSelected) colors.onPrimary else colors.textSecondary
                )
                if (countValue != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = countValue.toString(),
                        fontFamily = JetBrainsMono,
                        fontSize = 11.sp,
                        color = if (isSelected) {
                            colors.onPrimary.copy(alpha = 0.65f)
                        } else {
                            colors.textMuted
                        }
                    )
                }
            }
        }
    }
}

/** Text-only action for section headers and inline "manage" links. */
@Composable
fun SoberQuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val colors = SoberTheme.colors

    Row(
        modifier = modifier
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.primary
        )
    }
}

/**
 * One number and its caption. Used in place of stat cards where the values are
 * supporting detail: three of these divided by hairlines take less vertical
 * room than two tiles and keep the hero number above the fold.
 */
@Composable
fun SoberInlineMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    isEmphasised: Boolean = false
) {
    val colors = SoberTheme.colors

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isEmphasised) colors.accent else colors.textPrimary,
            style = TabularNumberStyle
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = colors.textMuted
        )
    }
}
