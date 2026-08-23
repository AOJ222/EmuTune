package com.emutune.designsystem.theme

import androidx.compose.ui.unit.dp

/**
 * Focus tokens for controller/D-pad navigation. EmuTune is operable without touch, so
 * focus indication is a first-class visual language, not an afterthought: a visible
 * accent ring, a subtle press scale, and coherent in/out timing.
 *
 * Recreated from the focus-token discipline of the Ourtor/Cinevo design systems
 * (ring + press scale + asymmetric in/out easing), expressed as native Compose tokens.
 */
object Focus {
    /** Width of the focus ring drawn around a focused target. */
    val RingWidth = 2.dp

    /** Focus-ring and press-feedback accent. Same hue as the primary accent. */
    val RingColor = EmuTuneColors.Accent

    /** Scale-up applied while an element holds focus (handheld/controller focus). */
    const val FocusedScale = 1.01f

    /** Scale-down applied while an element is actively pressed. */
    const val PressedScale = 0.98f

    /** Milliseconds for the focus-in animation (quick, feels responsive). */
    const val TransitionInMs = 120

    /** Milliseconds for the focus-out animation (slightly slower to settle). */
    const val TransitionOutMs = 200
}
