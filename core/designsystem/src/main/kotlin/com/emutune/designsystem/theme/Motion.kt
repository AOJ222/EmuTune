package com.emutune.designsystem.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Motion tokens. Motion is refined and quick — the app feels fast, not flashy.
 * A single easing curve and a small set of durations keep transitions coherent.
 */
object Motion {
    val Easing: Easing = FastOutSlowInEasing

    const val Quick = 120
    const val Standard = 220
    const val Emphasis = 340
}
