package com.emutune.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EmuTuneColorScheme = darkColorScheme(
    primary = EmuTuneColors.Accent,
    onPrimary = EmuTuneColors.Background,
    primaryContainer = EmuTuneColors.AccentDim,
    onPrimaryContainer = EmuTuneColors.TextPrimary,
    secondary = EmuTuneColors.Ice,
    onSecondary = EmuTuneColors.Background,
    background = EmuTuneColors.Background,
    onBackground = EmuTuneColors.TextPrimary,
    surface = EmuTuneColors.Surface,
    onSurface = EmuTuneColors.TextPrimary,
    surfaceVariant = EmuTuneColors.SurfaceElevated,
    onSurfaceVariant = EmuTuneColors.TextSecondary,
    outline = EmuTuneColors.BorderStrong,
    error = EmuTuneColors.Danger,
    onError = EmuTuneColors.Background,
)

/**
 * The single theme entry point. The app is dark-first and does not toggle light mode —
 * the instrument character is intrinsic, not a preference.
 */
@Composable
fun EmuTuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = EmuTuneColorScheme,
        typography = EmuTuneTypography,
        content = content,
    )
}
