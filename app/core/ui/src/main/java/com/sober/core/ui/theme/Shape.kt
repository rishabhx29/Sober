package com.sober.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val SoberShapes = Shapes(
    small = RoundedCornerShape(4.dp),       // Chips, tags
    medium = RoundedCornerShape(14.dp),     // Cards
    large = RoundedCornerShape(16.dp),      // Bottom sheets
    extraLarge = RoundedCornerShape(28.dp)  // Pill buttons
)
