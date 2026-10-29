package com.sober.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.SoberTheme

/**
 * Section label: 11sp uppercase, muted, no card. Sits above a group of cards.
 */
@Composable
fun SoberSectionLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text.uppercase(),
        color = SoberTheme.colors.textMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 1.sp,
        modifier = modifier
    )
}

/** Thin 1px divider in the warm border tone, matching the card border. */
@Composable
fun SoberDivider(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(SoberTheme.colors.divider)
    )
}

/**
 * A secondary action that should read as a link, not a button.
 *
 * The underline carries the affordance so it does not depend on colour alone,
 * and [minimumInteractiveComponentSize] restores the 48dp touch target that
 * 14sp text would otherwise lose.
 */
@Composable
fun SoberTextLink(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        color = SoberTheme.colors.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        textDecoration = TextDecoration.Underline,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp)
    )
}

/**
 * A status row: label on the left, and a coloured dot plus text on the right.
 * Used for permission state where the value is granted/missing rather than a
 * free-form string.
 */
@Composable
fun SoberStatusRow(
    label: String,
    statusText: String,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = SoberTheme.colors.textPrimary,
            fontSize = 15.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isPositive) SoberTheme.colors.primary else SoberTheme.colors.danger
                    )
            )
            Text(
                text = statusText,
                color = if (isPositive) {
                    SoberTheme.colors.textSecondary
                } else {
                    SoberTheme.colors.danger
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}