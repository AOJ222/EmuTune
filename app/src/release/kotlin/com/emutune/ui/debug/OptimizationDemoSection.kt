package com.emutune.ui.debug

import androidx.compose.runtime.Composable

/**
 * Release substitute for the debug optimisation simulation. Renders nothing — the
 * debug-only [OptimizationDemoViewModel] and [com.emutune.data.emulator.FakeEmulatorAdapter]
 * are not on the release dependency graph, so this section must not exist there.
 */
@Composable
fun OptimizationDemoSection() {
    // Intentionally empty: the simulation is debug-only.
}
