package com.emutune.ui.game

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.ConfidenceIndicator
import com.emutune.designsystem.component.Metric
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.component.StatusBadge
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing
import com.emutune.ui.presentation.StatusPresentation

@Composable
fun GameScreen(
    gameId: Long,
    onBack: () -> Unit,
    onMeasureFps: () -> Unit = {},
    viewModel: GameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(gameId) { viewModel.load(gameId) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.S, vertical = Spacing.XS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = EmuTuneColors.TextPrimary,
                )
            }
            Text(
                text = state.game?.title ?: "",
                style = MaterialTheme.typography.titleLarge,
                color = EmuTuneColors.TextPrimary,
            )
        }

        when {
            state.loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = EmuTuneColors.Accent)
                }
            }

            state.game == null -> {
                Text(
                    text = "Game not found.",
                    modifier = Modifier.padding(Spacing.L),
                    color = EmuTuneColors.TextSecondary,
                )
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(Spacing.L),
                    verticalArrangement = Arrangement.spacedBy(Spacing.L),
                ) {
                    CurrentPerformanceCard(
                        state = state,
                        onMeasureFps = { viewModel.startFpsMeasurement(onMeasureFps) },
                    )
                    BestVerifiedCard(
                        state = state,
                        onPlay = viewModel::play,
                        onMarkManual = viewModel::markPlayingManually,
                    )
                    RoutesCard(state)
                }
            }
        }
    }
}

@Composable
private fun BestVerifiedCard(
    state: GameUiState,
    onPlay: () -> Unit,
    onMarkManual: () -> Unit,
) {
    val recommendation = state.recommendation ?: return
    val recommendedRow = state.routes.firstOrNull { it.isRecommended } ?: return
    val evaluation = recommendation.evaluations.firstOrNull {
        it.executionRouteId == recommendation.recommendedRouteId
    }

    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "BEST VERIFIED",
            style = MaterialTheme.typography.labelLarge,
            color = EmuTuneColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(Spacing.S))
        Text(
            text = "${recommendedRow.emulatorName} · ${recommendedRow.platformName}",
            style = MaterialTheme.typography.headlineMedium,
            color = EmuTuneColors.TextPrimary,
        )
        Spacer(modifier = Modifier.height(Spacing.M))

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XL)) {
            Metric(
                label = "AVG FPS",
                value = evaluation?.metrics?.averageFps?.let { String.format("%.1f", it) } ?: "—",
                valueColor = EmuTuneColors.Accent,
            )
            Metric(
                label = "1% LOW",
                value = evaluation?.metrics?.onePercentLowFps?.let { String.format("%.1f", it) } ?: "—",
            )
            Metric(
                label = "VS CURRENT",
                value = recommendation.comparison?.percentChange?.let { "%+d%%".format(it.toInt()) } ?: "—",
                valueColor = EmuTuneColors.Success,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.M))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ConfidenceIndicator(
                label = StatusPresentation.confidenceLabel(recommendation.confidence.level),
                fraction = StatusPresentation.confidenceFraction(recommendation.confidence.level),
                color = StatusPresentation.confidenceColor(recommendation.confidence.level),
            )
            StatusBadge(
                label = StatusPresentation.label(recommendation.status),
                color = StatusPresentation.color(recommendation.status),
                icon = StatusPresentation.icon(recommendation.status),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.L))

        PlayActions(state, onPlay = onPlay, onMarkManual = onMarkManual)
        Spacer(modifier = Modifier.height(Spacing.S))
        Text(
            text = "Automatic optimisation is gated on the emulator's CONFIG_WRITE capability. Dolphin currently exposes guided configuration only.",
            style = MaterialTheme.typography.labelMedium,
            color = EmuTuneColors.TextTertiary,
        )
    }
}

@Composable
private fun CurrentPerformanceCard(
    state: GameUiState,
    onMeasureFps: () -> Unit,
) {
    val observation = state.latestObservation
    val metrics = observation?.metrics

    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "CURRENT PERFORMANCE",
            style = MaterialTheme.typography.labelLarge,
            color = EmuTuneColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(Spacing.S))

        if (observation != null && metrics != null && metrics.hasAnyMeasurement) {
            val routeName = state.routes.firstOrNull {
                it.route.id == observation.executionRouteId
            }?.let { "${it.emulatorName} · ${it.platformName}" }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.XL)) {
                Metric(
                    label = "UPDATES/S",
                    value = metrics.averageFps?.let { String.format("%.1f", it) } ?: "—",
                    valueColor = EmuTuneColors.Accent,
                )
                Metric(
                    label = "1% LOW",
                    value = metrics.onePercentLowFps?.let { String.format("%.1f", it) } ?: "—",
                )
            }
            Spacer(modifier = Modifier.height(Spacing.S))
            Text(
                text = "Measured on this device",
                style = MaterialTheme.typography.labelMedium,
                color = EmuTuneColors.TextSecondary,
            )
            Text(
                text = "${StatusPresentation.evidenceGradeLabel(observation.evidenceGrade)} confidence",
                style = MaterialTheme.typography.labelMedium,
                color = EmuTuneColors.TextTertiary,
            )
            if (routeName != null) {
                Spacer(modifier = Modifier.height(Spacing.XS))
                Text(
                    text = routeName,
                    style = MaterialTheme.typography.labelMedium,
                    color = EmuTuneColors.TextTertiary,
                )
            }
        } else {
            Text(
                text = "Not measured on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = EmuTuneColors.TextSecondary,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.M))
        MeasureAction(state = state, onMeasureFps = onMeasureFps)
    }
}

@Composable
private fun MeasureAction(state: GameUiState, onMeasureFps: () -> Unit) {
    when (val fpsState = state.fpsState) {
        FpsMeasureUiState.Idle -> {
            OutlinedButton(onClick = onMeasureFps, modifier = Modifier.fillMaxWidth()) {
                Text("MEASURE")
            }
        }

        FpsMeasureUiState.Measuring -> {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.height(16.dp),
                    color = EmuTuneColors.Accent,
                )
                Spacer(modifier = Modifier.padding(horizontal = Spacing.S))
                Text(
                    text = "Measuring from screen capture…",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            }
        }

        is FpsMeasureUiState.Success -> {
            Text(
                text = "Measurement saved",
                style = MaterialTheme.typography.labelLarge,
                color = EmuTuneColors.Success,
            )
        }

        is FpsMeasureUiState.Failure -> {
            Text(
                text = "Measurement failed: ${fpsState.reason}",
                style = MaterialTheme.typography.bodyMedium,
                color = EmuTuneColors.Danger,
            )
        }
    }
}

@Composable
private fun PlayActions(
    state: GameUiState,
    onPlay: () -> Unit,
    onMarkManual: () -> Unit,
) {
    val playState = state.playState
    when (playState) {
        PlayUiState.Idle -> {
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth()) {
                Text("PLAY")
            }
        }

        PlayUiState.Launching -> {
            Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                Text("LAUNCHING…")
            }
        }

        PlayUiState.Launched -> {
            StatusBadge(label = "Launched", color = EmuTuneColors.Success)
        }

        PlayUiState.MarkedManual -> {
            StatusBadge(label = "Playing now", color = EmuTuneColors.Accent)
        }

        is PlayUiState.OfferManual -> {
            OpticSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    Text(
                        text = "Automatic launch is not available.",
                        style = MaterialTheme.typography.titleMedium,
                        color = EmuTuneColors.TextPrimary,
                    )
                    Text(
                        text = playState.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = EmuTuneColors.TextSecondary,
                    )
                    Button(onClick = onMarkManual, modifier = Modifier.fillMaxWidth()) {
                        Text("MARK AS PLAYING")
                    }
                }
            }
        }
    }
}

@Composable
private fun RoutesCard(state: GameUiState) {
    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "ROUTES",
            style = MaterialTheme.typography.labelLarge,
            color = EmuTuneColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(Spacing.M))
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
            state.routes.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "${row.emulatorName} · ${row.platformName}",
                            style = MaterialTheme.typography.titleMedium,
                            color = EmuTuneColors.TextPrimary,
                        )
                        if (row.disqualified != null) {
                            Text(
                                text = row.disqualified.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = EmuTuneColors.Danger,
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = row.averageFps?.let { String.format("%.1f FPS", it) } ?: "—",
                            style = MaterialTheme.typography.labelLarge,
                            color = EmuTuneColors.TextSecondary,
                        )
                        if (row.isRecommended) {
                            Spacer(modifier = Modifier.padding(horizontal = Spacing.S))
                            StatusBadge(
                                label = "BEST",
                                color = EmuTuneColors.Success,
                            )
                        }
                    }
                }
            }
        }
    }
}
