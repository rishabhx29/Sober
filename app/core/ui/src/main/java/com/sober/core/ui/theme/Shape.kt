package com.sober.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Softer than the first pass: 14dp cards read as boxes next to the 28dp pill
 * buttons. 18dp gives the cards the same editorial curve as the buttons, while
 * chips stay tight. Nested corners follow outer = inner + padding.
 */
val SoberShapes = Shapes(
    small = RoundedCornerShape(10.dp),      // Chips, tags
    medium = RoundedCornerShape(18.dp),     // Cards
    large = RoundedCornerShape(22.dp),      // Bottom sheets
    extraLarge = RoundedCornerShape(30.dp)  // Pill buttons
)
