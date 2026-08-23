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
import com.emutune.designsystem.component.SectionTitle
import com.emutune.designsystem.component.StatusBadge
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing
import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.session.PlaySessionSource

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val installations by viewModel.installations.collectAsStateWithLifecycle()
    val nowPlaying by viewModel.nowPlaying.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(Spacing.L),
        verticalArrangement = Arrangement.spacedBy(Spacing.L),
    ) {
        SectionTitle("NOW PLAYING")
        OpticSurface(modifier = Modifier.fillMaxWidth()) {
            NowPlayingSummary(nowPlaying)
        }

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
private fun NowPlayingSummary(nowPlaying: com.emutune.data.repo.NowPlaying?) {
    val game = nowPlaying?.game
    if (game == null) {
        Text(
            text = "Nothing playing right now.",
            style = MaterialTheme.typography.titleMedium,
            color = EmuTuneColors.TextPrimary,
        )
        Text(
            text = "Start a game from the Library to see its best verified route.",
            style = MaterialTheme.typography.bodyMedium,
            color = EmuTuneColors.TextSecondary,
        )
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = game.title,
                style = MaterialTheme.typography.titleLarge,
                color = EmuTuneColors.TextPrimary,
            )
            nowPlaying.edition?.let { edition ->
                Text(
                    text = edition.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            }
        }
        StatusBadge(
            label = when (nowPlaying.session.source) {
                PlaySessionSource.LAUNCHED -> "Launched"
                PlaySessionSource.MANUAL -> "Playing"
            },
            color = EmuTuneColors.Accent,
        )
    }
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
