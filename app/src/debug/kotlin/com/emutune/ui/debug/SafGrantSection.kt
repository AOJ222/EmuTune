package com.emutune.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.component.StatusBadge
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing

/**
 * Debug-only SAF grant trigger for the Dolphin config-access spike (see
 * `docs/08-research/spikes/dolphin-saf-spike.md`). Launches the system
 * `ACTION_OPEN_DOCUMENT_TREE` picker so a real device can confirm whether
 * Dolphin's config root is grantable to a third-party app.
 *
 * Exists only in the debug source set; the release build substitutes a no-op.
 */
@Composable
fun SafGrantSection(onGrant: () -> Unit) {
    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            Text(
                text = "Dolphin config access (spike)",
                style = MaterialTheme.typography.titleMedium,
                color = EmuTuneColors.TextPrimary,
            )
            StatusBadge(label = "DEBUG", color = EmuTuneColors.Warning)
            Text(
                text = "Opens the system folder picker. Grant Dolphin's folder to test SAF read/write of its config.",
                style = MaterialTheme.typography.bodyMedium,
                color = EmuTuneColors.TextSecondary,
            )
            Button(onClick = onGrant, modifier = Modifier.fillMaxWidth()) {
                Text("GRANT DOLPHIN CONFIG ACCESS")
            }
        }
    }
}
