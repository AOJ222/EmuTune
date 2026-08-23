package com.emutune.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing
import com.emutune.ui.debug.OptimizationDemoSection
import com.emutune.ui.debug.SafGrantSection

/**
 * App preferences only. Emulator configuration belongs to the game/route context, not a
 * global settings dump, so this screen deliberately stays minimal.
 */
@Composable
fun SettingsScreen(onGrantDolphinAccess: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.L),
        verticalArrangement = Arrangement.spacedBy(Spacing.L),
    ) {
        Text(
            text = "App preferences only — emulator configuration lives in the game and route context.",
            style = MaterialTheme.typography.bodyMedium,
            color = EmuTuneColors.TextSecondary,
        )

        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
                Text(
                    text = "EmuTune",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmuTuneColors.TextPrimary,
                )
                Text(
                    text = "Evidence-driven emulator optimisation. Milestone 1.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            }
        }

        SafGrantSection(onGrant = onGrantDolphinAccess)
        OptimizationDemoSection()
    }
}
