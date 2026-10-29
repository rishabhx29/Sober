package com.sober.core.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Every colour a screen uses. `onPrimary` exists because the accent flips from
 * navy to cream in dark mode: hard-coded white button text would be unreadable
 * on cream, so "text on the accent" has to be a token, not a constant.
 */
data class SoberColors(
    val primary: Color = SoberPrimary,
    val onPrimary: Color = Color.White,
    val background: Color = SoberBackground,
    val surface: Color = SoberSurface,
    val cardBorder: Color = SoberCardBorder,
    val textPrimary: Color = SoberTextPrimary,
    val textSecondary: Color = SoberTextSecondary,
    val textMuted: Color = SoberTextMuted,
    val divider: Color = SoberDivider,
    val danger: Color = SoberDanger
)

val LightSoberColors = SoberColors()

val DarkSoberColors = SoberColors(
    primary = SoberDarkPrimary,
    onPrimary = SoberDarkOnPrimary,
    background = SoberDarkBackground,
    surface = SoberDarkSurface,
    cardBorder = SoberDarkCardBorder,
    textPrimary = SoberDarkTextPrimary,
    textSecondary = SoberDarkTextSecondary,
    textMuted = SoberDarkTextMuted,
    divider = SoberDarkDivider,
    danger = SoberDarkDanger
)

val LocalSoberColors = staticCompositionLocalOf { LightSoberColors }

object SoberTheme {
    val colors: SoberColors
        @Composable
        get() = LocalSoberColors.current

    val typography: androidx.compose.material3.Typography
        @Composable
        get() = MaterialTheme.typography
}

private val LightColorScheme = lightColorScheme(
    primary = SoberPrimary,
    onPrimary = SoberSurface,
    secondary = SoberWarning,
    background = SoberBackground,
    surface = SoberSurface,
    onBackground = SoberTextPrimary,
    onSurface = SoberTextPrimary,
    error = SoberDanger
)

private val DarkColorScheme = darkColorScheme(
    primary = SoberDarkPrimary,
    onPrimary = SoberDarkOnPrimary,
    secondary = SoberWarning,
    background = SoberDarkBackground,
    surface = SoberDarkSurface,
    onBackground = SoberDarkTextPrimary,
    onSurface = SoberDarkTextPrimary,
    error = SoberDarkDanger,
    onError = SoberDarkOnPrimary
)

@Composable
fun SoberTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkSoberColors else LightSoberColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    // Null inside a system overlay window: that ComposeView is hosted by the
    // application context, not an Activity, and has no window to tint.
    val window = view.context.findActivity()?.window
    if (window != null && !view.isInEditMode) {
        SideEffect {
            window.statusBarColor = colors.background.toArgb()
            // Dark bars need light icons; light bars need dark icons.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalSoberColors provides colors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SoberTypography,
            shapes = SoberShapes,
            content = content
        )
    }
}

/**
 * Walks the wrapper chain because theme-aware callers pass a
 * `ContextThemeWrapper`, so the view's own context is rarely the Activity.
 */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
