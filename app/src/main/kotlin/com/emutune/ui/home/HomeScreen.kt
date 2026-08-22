package com.emutune.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.Metric
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing
import com.emutune.model.device.DeviceFingerprint

@Composable
fun HomeScreen(
    onOpenGame: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val installations by viewModel.installations.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.L),
        verticalArrangement = Arrangement.spacedBy(Spacing.L),
    ) {
        SectionTitle("YOUR DEVICE")
        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            DeviceSummary(profile)
        }

        SectionTitle("INSTALLED EMULATORS")
        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            if (installations.isEmpty()) {
                Text(
                    text = "No supported emulators detected.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    installations.forEach { installation ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = viewModel.emulatorName(installation.emulatorId),
                                style = MaterialTheme.typography.titleMedium,
                                color = EmuTuneColors.TextPrimary,
                            )
                            Text(
                                text = installation.versionName ?: "unknown version",
                                style = MaterialTheme.typography.labelMedium,
                                color = EmuTuneColors.TextSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = EmuTuneColors.TextSecondary,
    )
}

@Composable
private fun DeviceSummary(profile: DeviceFingerprint?) {
    if (profile == null) {
        Text(
            text = "Profiling device…",
            style = MaterialTheme.typography.bodyMedium,
            color = EmuTuneColors.TextSecondary,
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.XL),
    ) {
        Metric(
            label = "MODEL",
            value = profile.model ?: "Unknown",
            modifier = Modifier.weight(1f),
        )
        Metric(
            label = "ANDROID",
            value = profile.androidApiLevel?.let { "API $it" } ?: "Unknown",
            modifier = Modifier.weight(1f),
        )
        Metric(
            label = "SOC",
            value = profile.socModel?.name ?: "Unknown",
            modifier = Modifier.weight(1f),
        )
    }
    Spacer(modifier = Modifier.height(Spacing.S))
    Text(
        text = "SoC and GPU are only shown when a reliable source identifies them.",
        style = MaterialTheme.typography.labelMedium,
        color = EmuTuneColors.TextTertiary,
    )
}
