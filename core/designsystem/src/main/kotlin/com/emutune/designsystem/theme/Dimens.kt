package com.emutune.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing, radius and elevation tokens. Screens reference these rather than inlining
 * arbitrary dp values, so the rhythm stays consistent across every surface.
 */
object Spacing {
    val XS = 4.dp
    val S = 8.dp
    val M = 12.dp
    val L = 16.dp
    val XL = 24.dp
    val XXL = 32.dp
}

object Radius {
    val S = 10.dp
    val M = 16.dp
    val L = 24.dp
    val XL = 32.dp
}

object Elevation {
    val Raised = 2.dp
    val Card = 4.dp
    val Overlay = 8.dp
}
