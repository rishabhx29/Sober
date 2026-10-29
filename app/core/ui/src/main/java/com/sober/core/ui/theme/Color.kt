package com.sober.core.ui.theme

import androidx.compose.ui.graphics.Color

// Manuscript Palette
val SoberBackground = Color(0xFFF7F4EF)      // Warm cream
val SoberSurface = Color(0xFFFFFFFF)          // Pure white (cards)
val SoberCardBorder = Color(0xFFE8E0D5)       // Warm border
val SoberPrimary = Color(0xFF1A1A2E)          // Ink Navy
val SoberPrimaryMuted = Color(0xFFEDEAE4)     // Cream tint
val SoberDanger = Color(0xFFC0392B)           // Signal Red
val SoberDangerLight = Color(0xFFF9ECEB)      // Red tint bg
val SoberWarning = Color(0xFF8B6F47)          // Warm Brown
val SoberWarningLight = Color(0xFFF5EFE6)     // Warning bg
val SoberTextPrimary = Color(0xFF1A1A1A)      // Body copy
val SoberTextSecondary = Color(0xFF6B6560)    // Supporting text
val SoberTextMuted = Color(0xFF9C9490)        // Placeholders
val SoberDivider = Color(0xFFE8E0D5)          // Dividers

// Dark palette.
//
// Inverted, not dimmed: the navy that carries the light theme becomes the
// background here, and cream becomes the accent. Darkening the cream/white
// tokens instead would leave navy text on navy cards and a primary button that
// disappears into the page.
val SoberDarkBackground = Color(0xFF12121A)   // Ink, near-black
val SoberDarkSurface = Color(0xFF1B1B26)      // Raised card
val SoberDarkCardBorder = Color(0xFF2A2A38)   // Hairline on ink
val SoberDarkPrimary = Color(0xFFE8E0D5)      // Cream accent
val SoberDarkOnPrimary = Color(0xFF1A1A2E)    // Text/icon on the cream accent
val SoberDarkTextPrimary = Color(0xFFF2EDE4)  // Body copy
val SoberDarkTextSecondary = Color(0xFFB3ABA2) // Supporting text
val SoberDarkTextMuted = Color(0xFF8A837C)    // Placeholders
val SoberDarkDivider = Color(0xFF2A2A38)      // Dividers
val SoberDarkDanger = Color(0xFFE0685A)       // Signal Red, lifted for contrast
