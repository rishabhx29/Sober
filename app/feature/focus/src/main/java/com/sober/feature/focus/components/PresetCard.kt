package com.sober.feature.focus.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.SoberTheme

@Composable
fun PresetCard(
    label: String,
    description: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, SoberTheme.colors.primary),
        color = SoberTheme.colors.surface,
        modifier = Modifier
            .size(width = 120.dp, height = 80.dp)
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = label,
                style = SoberTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = SoberTheme.colors.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = description,
                fontSize = 11.sp,
                color = SoberTheme.colors.textSecondary
            )
        }
    }
}
