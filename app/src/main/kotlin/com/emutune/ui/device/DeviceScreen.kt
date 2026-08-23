package com.emutune.ui.device

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.component.SectionTitle
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing
import com.emutune.model.config.IniDocument
import com.emutune.model.device.DeviceFingerprint

@Composable
fun DeviceScreen(viewModel: DeviceViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val device = profile
    val hasDolphinGrant by viewModel.hasDolphinGrant.collectAsStateWithLifecycle()
    val dolphinConfig by viewModel.dolphinConfig.collectAsStateWithLifecycle()

    if (device == null) {
        Column(modifier = Modifier.fillMaxSize().padding(Spacing.L)) {
            Text(
                text = "Profiling device…",
                style = MaterialTheme.typography.bodyMedium,
                color = EmuTuneColors.TextSecondary,
            )
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.L),
        verticalArrangement = Arrangement.spacedBy(Spacing.L),
    ) {
        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                SectionTitle("IDENTITY")
                DetailRow("Manufacturer", device.manufacturer)
                DetailRow("Brand", device.brand)
                DetailRow("Model", device.model)
                DetailRow("Codename", device.deviceCodename)
            }
        }

        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                SectionTitle("PLATFORM")
                DetailRow("Android", device.androidApiLevel?.let { "API $it" })
                DetailRow("Build", device.androidBuild)
                DetailRow("ABIs", device.supportedAbis.takeIf { it.isNotEmpty() }?.joinToString())
                DetailRow("RAM", device.ramBytes?.let { "${it / (1024 * 1024)} MB" })
                DetailRow(
                    "Display",
                    if (device.displayWidthPx != null && device.displayHeightPx != null) {
                        "${device.displayWidthPx}×${device.displayHeightPx}"
                    } else {
                        null
                    },
                )
                DetailRow(
                    "Refresh rate",
                    device.refreshRatesHz.takeIf { it.isNotEmpty() }?.joinToString(" Hz, ") { "${it.toInt()}" } + " Hz",
                )
            }
        }

        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                SectionTitle("GRAPHICS")
                DetailRow("SoC", device.socModel?.name ?: "Unknown")
                DetailRow("GPU", device.gpuRenderer ?: "Unknown")
                Text(
                    text = "SoC and GPU are only shown when a reliable source identifies them. Android does not expose a trustworthy SoC-name API, so these remain unknown rather than guessed.",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmuTuneColors.TextTertiary,
                )
            }
        }

        DolphinConfigCard(hasGrant = hasDolphinGrant, config = dolphinConfig)
    }
}

@Composable
private fun DolphinConfigCard(hasGrant: Boolean, config: IniDocument?) {
    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            SectionTitle("DOLPHIN CONFIGURATION")
            when {
                !hasGrant -> {
                    Text(
                        text = "No access granted. Grant it from Settings to see Dolphin's current settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmuTuneColors.TextSecondary,
                    )
                }

                config == null || config.sections.isEmpty() -> {
                    Text(
                        text = "Access granted, but Dolphin.ini could not be read.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmuTuneColors.TextSecondary,
                    )
                }

                else -> {
                    config.sections.forEach { section ->
                        SectionTitle(section.name)
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.XS)) {
                            section.entries.forEach { entry ->
                                DetailRow(entry.key, entry.value)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = EmuTuneColors.TextSecondary,
        )
        Text(
            text = value ?: "Unknown",
            style = MaterialTheme.typography.bodyMedium,
            color = EmuTuneColors.TextPrimary,
        )
    }
}
