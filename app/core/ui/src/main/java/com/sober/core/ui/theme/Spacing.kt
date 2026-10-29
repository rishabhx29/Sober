package com.sober.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SoberSpacing(
    val base: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val contentGutter: Dp = 20.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 32.dp,
    val sectionSpacing: Dp = 48.dp
)

val Spacing = SoberSpacing()
