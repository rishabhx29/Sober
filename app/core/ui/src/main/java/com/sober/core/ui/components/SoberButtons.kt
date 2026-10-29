package com.sober.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sober.core.ui.theme.SoberShapes
import com.sober.core.ui.theme.SoberTheme

@Composable
fun SoberButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = SoberTheme.colors

    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = SoberShapes.extraLarge,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            disabledContainerColor = colors.primary.copy(alpha = 0.4f),
            disabledContentColor = colors.onPrimary.copy(alpha = 0.6f)
        )
    ) {
        Text(
            text = text,
            color = colors.onPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun SoberOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = SoberTheme.colors

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = SoberShapes.extraLarge,
        border = BorderStroke(1.dp, colors.primary),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = colors.primary,
            containerColor = Color.Transparent
        )
    ) {
        Text(
            text = text,
            color = colors.primary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
