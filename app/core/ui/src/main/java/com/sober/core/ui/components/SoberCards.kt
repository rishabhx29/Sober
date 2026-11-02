package com.sober.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.SoberShapes
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.Spacing
import com.sober.core.ui.theme.TabularNumberStyle

@Composable
fun SoberCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = SoberTheme.colors

    Card(
        modifier = modifier,
        shape = SoberShapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // No shadow
    ) {
        Column(
            modifier = Modifier.padding(Spacing.medium),
            content = content
        )
    }
}

/**
 * A compact metric with an icon, a label and one number. Home's two secondary
 * tiles used bare uppercase labels with no visual anchor; the badge gives each
 * tile a glanceable identity without adding a second accent colour.
 */
@Composable
fun SoberStatTile(
    icon: ImageVector,
    label: String,
    value: String,
    caption: String,
    modifier: Modifier = Modifier
) {
    val colors = SoberTheme.colors

    Card(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "$label, $value $caption"
        },
        shape = SoberShapes.medium,
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        border = BorderStroke(1.dp, colors.cardBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SoberIconBadge(
                    imageVector = icon,
                    contentDescription = null,
                    size = 26.dp,
                    iconSize = 15.dp,
                    shape = RoundedCornerShape(8.dp)
                )
                SoberSectionLabel(text = label, isHeading = false)
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                fontSize = 26.sp,
                color = colors.primary,
                fontWeight = FontWeight.Bold,
                style = TabularNumberStyle
            )
            Text(
                text = caption,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}
