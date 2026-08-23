package com.emutune.ui.debug

import androidx.compose.runtime.Composable

/** Release no-op for the debug-only Dolphin SAF grant spike. */
@Composable
fun SafGrantSection(onGrant: () -> Unit) {
    // Intentionally empty: the SAF grant spike is debug-only.
}
