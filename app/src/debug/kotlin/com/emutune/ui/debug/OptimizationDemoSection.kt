package com.emutune.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.component.StatusBadge
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing

/**
 * Debug-only optimisation simulation. Runs the full configuration transaction
 * lifecycle (read → snapshot → validate → apply → verify → commit/rollback) through a
 * [com.emutune.data.emulator.FakeEmulatorAdapter], and shows each step as it happens.
 *
 * This composable exists only in the debug source set; the release build substitutes a
 * no-op, so the simulation can never ship.
 */
@Composable
fun OptimizationDemoSection(viewModel: OptimizationDemoViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Optimisation simulation",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmuTuneColors.TextPrimary,
                )
                StatusBadge(label = "DEBUG", color = EmuTuneColors.Warning)
            }

            Text(
                text = "Exercises the full transaction lifecycle against a fake adapter. No real emulator is touched; no benchmark data is produced.",
                style = MaterialTheme.typography.bodyMedium,
                color = EmuTuneColors.TextSecondary,
            )

            if (state.before.isNotEmpty()) {
                Text(
                    text = "Current configuration: ${state.before}",
                    style = MaterialTheme.typography.labelLarge,
                    color = EmuTuneColors.TextSecondary,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                Button(
                    onClick = { viewModel.run(forceFailure = false) },
                    enabled = !state.running,
                ) {
                    Text("Run commit")
                }
                OutlinedButton(
                    onClick = { viewModel.run(forceFailure = true) },
                    enabled = !state.running,
                ) {
                    Text("Run rollback")
                }
            }

            if (state.steps.isNotEmpty()) {
                Spacer(modifier = Modifier.height(Spacing.S))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.steps.forEach { step ->
                        Text(
                            text = step.label + (step.detail?.let { " — $it" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = EmuTuneColors.TextSecondary,
                        )
                    }
                }
            }

            when (val outcome = state.outcome) {
                is OptimizationDemoOutcome.Committed -> StatusBadge(
                    label = "Committed",
                    color = EmuTuneColors.Success,
                )
                is OptimizationDemoOutcome.RolledBack -> StatusBadge(
                    label = if (outcome.verified) "Rolled back (verified)" else "Rolled back (verification failed)",
                    color = if (outcome.verified) EmuTuneColors.Warning else EmuTuneColors.Danger,
                )
                is OptimizationDemoOutcome.Rejected -> StatusBadge(
                    label = "Rejected",
                    color = EmuTuneColors.Danger,
                )
                null -> Unit
            }
        }
    }
}
