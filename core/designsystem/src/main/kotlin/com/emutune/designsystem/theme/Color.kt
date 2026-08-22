package com.emutune.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Central colour tokens. The palette is a dark, instrument-like base with a single
 * restrained teal accent and a cooler ice-blue for data. Status colours live here so
 * no screen hard-codes a hex value.
 */
object EmuTuneColors {

    // Base surfaces
    val Background = Color(0xFF08090C)
    val Surface = Color(0xFF0F1218)
    val SurfaceElevated = Color(0xFF171B24)
    val SurfaceGlass = Color(0x14FFFFFF) // translucent white for glass panels
    val Border = Color(0x1FFFFFFF)
    val BorderStrong = Color(0x33FFFFFF)

    // Text
    val TextPrimary = Color(0xFFF2F4F8)
    val TextSecondary = Color(0xFF9AA3B2)
    val TextTertiary = Color(0xFF5E6673)

    // Accent
    val Accent = Color(0xFF4CC9B0) // restrained teal
    val AccentDim = Color(0xFF2A8A78)
    val Ice = Color(0xFF6EA8FE) // ice blue for data/charts

    // Semantic
    val Success = Color(0xFF4ADE80)
    val Warning = Color(0xFFFACC15)
    val Danger = Color(0xFFF87171)
    val Neutral = Color(0xFF9AA3B2)
}
