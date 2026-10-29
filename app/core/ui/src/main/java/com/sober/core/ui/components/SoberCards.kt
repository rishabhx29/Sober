package com.sober.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sober.core.ui.theme.SoberShapes
import com.sober.core.ui.theme.SoberTheme
import com.sober.core.ui.theme.Spacing

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
