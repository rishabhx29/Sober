package com.sober.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.sober.core.ui.theme.SoberTheme

/**
 * A tinted container for a single icon: achievement badges, streak marks and
 * session medallions. Replaces the emoji that used to carry this role, so the
 * glyph follows the theme in both light and dark mode and scales with the
 * icon's vector rather than a font's colour emoji table.
 *
 * [contentDescription] belongs on the badge only when the icon alone conveys
 * meaning; when a visible title sits beside it, pass null and let the text
 * speak for the group.
 */
@Composable
fun SoberIconBadge(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 22.dp,
    containerColor: Color = SoberTheme.colors.primaryMuted,
    contentColor: Color = SoberTheme.colors.primary,
    shape: Shape = CircleShape,
    border: BorderStroke? = null
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(containerColor)
            .then(
                if (border != null) Modifier.border(border, shape) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = contentColor,
            modifier = Modifier.size(iconSize)
        )
    }
}
