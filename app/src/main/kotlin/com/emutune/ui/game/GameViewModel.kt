package com.emutune.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.benchmark.FpsCaptureResult
import com.emutune.data.benchmark.MeasurementRecorder
import com.emutune.data.emulator.AdapterResolver
import com.emutune.data.emulator.EmulatorRegistry
import com.emutune.data.repo.ActivityRepository
import com.emutune.data.repo.GameRepository
import com.emutune.data.repo.LaunchOutcome
import com.emutune.data.repo.ObservationRepository
import com.emutune.data.repo.RecommendationRepository
import com.emutune.data.repo.SessionRepository
import com.emutune.model.activity.ActivityEventType
import com.emutune.model.benchmark.BenchmarkResult
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.Observation
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.GameId
import com.emutune.model.recommendation.DisqualificationReason
import com.emutune.model.recommendation.OptimizationGoal
import com.emutune.model.recommendation.Recommendation
import com.emutune.model.route.ExecutionRoute
import com.emutune.ui.presentation.StatusPresentation
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

data class RouteRowUi(
    val route: ExecutionRoute,
    val emulatorName: String,
    val platformName: String,
    val averageFps: Double?,
    val isRecommended: Boolean,
    val disqualified: DisqualificationReason?,
)

/** State of the Play action: launch-through first, manual "mark as playing" as fallback. */
sealed interface PlayUiState {
    data object Idle : PlayUiState
    data object Launching : PlayUiState
    data object Launched : PlayUiState
    data class OfferManual(val reason: String) : PlayUiState
    data object MarkedManual : PlayUiState
}

/** State of the on-device FPS measurement via screen capture. */
sealed interface FpsMeasureUiState {
    data object Idle : FpsMeasureUiState
    data object Measuring : FpsMeasureUiState
    data class Success(val averageFps: Double) : FpsMeasureUiState
    data class Failure(val reason: String) : FpsMeasureUiState
}

data class GameUiState(
    val game: Game? = null,
    val editions: List<GameEdition> = emptyList(),
    val routes: List<RouteRowUi> = emptyList(),
    val recommendation: Recommendation? = null,
    val loading: Boolean = true,
    val playState: PlayUiState = PlayUiState.Idle,
    val fpsState: FpsMeasureUiState = FpsMeasureUiState.Idle,
    /** The most recent persisted observation for the primary edition, if any. */
    val latestObservation: Observation? = null,
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val recommendationRepository: RecommendationRepository,
    private val sessionRepository: SessionRepository,
    private val observationRepository: ObservationRepository,
    private val measurementRecorder: MeasurementRecorder,
    private val activityRepository: ActivityRepository,
    private val adapterResolver: AdapterResolver,
    private val registry: EmulatorRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(GameUiState())
    val state: StateFlow<GameUiState> = _state

    fun load(gameId: Long) {
        if (_state.value.game?.id?.value == gameId) return
        viewModelScope.launch {
            val game = gameRepository.observeGames().first().firstOrNull { it.id.value == gameId }
            val editions = gameRepository.observeEditions(GameId(gameId)).first()
            val routes = gameRepository.observeAllRoutes().first()
                .filter { route -> editions.any { it.id == route.gameEditionId } }
            val primaryEdition = editions.firstOrNull()

            val recommendation = primaryEdition?.let {
                recommendationRepository.recommendFor(it.id, OptimizationGoal.BALANCED)
            }
            val latestObservation = primaryEdition?.let {
                observationRepository.observeForEdition(it.id).first().firstOrNull()
            }

            _state.value = buildState(game, editions, routes, recommendation, latestObservation, loading = false)
        }
    }

    /** Launch-through (A). On failure, moves to [PlayUiState.OfferManual] for the fallback. */
    fun play() {
        val game = _state.value.game ?: return
        val route = _state.value.routes.firstOrNull { it.isRecommended }?.route ?: return
        val edition = _state.value.editions.firstOrNull { it.id == route.gameEditionId } ?: return
        if (_state.value.playState == PlayUiState.Launching) return

        _state.value = _state.value.copy(playState = PlayUiState.Launching)
        viewModelScope.launch {
            val adapter = adapterResolver.adapterFor(route.emulatorId)
            val outcome = sessionRepository.beginSession(game, edition, route, adapter)
            _state.value = _state.value.copy(
                playState = when (outcome) {
                    is LaunchOutcome.Launched -> PlayUiState.Launched
                    is LaunchOutcome.Unavailable -> PlayUiState.OfferManual(outcome.reason)
                },
            )
        }
    }

    /** Manual fallback (B) — the user declares the game is playing. */
    fun markPlayingManually() {
        val route = _state.value.routes.firstOrNull { it.isRecommended }?.route ?: return
        val edition = _state.value.editions.firstOrNull { it.id == route.gameEditionId } ?: return
        viewModelScope.launch {
            sessionRepository.markManual(edition, route)
            _state.value = _state.value.copy(playState = PlayUiState.MarkedManual)
        }
    }

    /**
     * Measures the current game's frame rate from screen capture, then persists the
     * result as an Observation and recomputes the recommendation. [requestConsent] is
     * the Activity-level callback that launches the MediaProjection consent dialog.
     */
    fun startFpsMeasurement(requestConsent: () -> Unit) {
        if (_state.value.fpsState == FpsMeasureUiState.Measuring) return
        val route = _state.value.routes.firstOrNull { it.isRecommended }?.route
        val editionId = route?.gameEditionId ?: _state.value.editions.firstOrNull()?.id
        val edition = _state.value.editions.firstOrNull { it.id == editionId }

        _state.value = _state.value.copy(fpsState = FpsMeasureUiState.Measuring)
        FpsCaptureResult.flow.value = null
        requestConsent()

        viewModelScope.launch {
            val result = withTimeoutOrNull(20_000) {
                FpsCaptureResult.flow.filterNotNull().first()
            }
            when (result) {
                null -> _state.value = _state.value.copy(fpsState = FpsMeasureUiState.Failure("Timed out waiting for a measurement"))
                is BenchmarkResult.Failure -> _state.value = _state.value.copy(fpsState = FpsMeasureUiState.Failure(result.reason))
                is BenchmarkResult.Success -> {
                    persistAndRecompute(result.metrics, result.durationSeconds, result.grade, editionId, route)
                }
            }
        }
    }

    private suspend fun persistAndRecompute(
        metrics: BenchmarkMetrics,
        durationSeconds: Int?,
        grade: com.emutune.model.evidence.EvidenceGrade,
        editionId: com.emutune.model.ids.GameEditionId?,
        route: ExecutionRoute?,
    ) {
        if (editionId == null || route == null) {
            _state.value = _state.value.copy(fpsState = FpsMeasureUiState.Failure("No route to attach the measurement to"))
            return
        }

        val before = _state.value.recommendation?.status
        measurementRecorder.record(
            gameEditionId = editionId,
            executionRouteId = route.id,
            metrics = metrics,
            providerId = "screen-capture-frame-delta",
            durationSeconds = durationSeconds,
            evidenceGrade = grade,
        )
        activityRepository.record(
            type = ActivityEventType.MEASUREMENT_COMPLETED,
            title = "Measured ${_state.value.game?.title ?: "game"}",
            detail = metrics.averageFps?.let { "${"%.1f".format(it)} displayed updates/s" },
            gameId = _state.value.game?.id?.value,
        )

        val recommendation = recommendationRepository.recommendFor(editionId, OptimizationGoal.BALANCED)
        val after = recommendation.status
        if (before != null && after != before) {
            activityRepository.record(
                type = ActivityEventType.RECOMMENDATION_CHANGED,
                title = "Recommendation updated",
                detail = "${StatusPresentation.label(before)} → ${StatusPresentation.label(after)}",
                gameId = _state.value.game?.id?.value,
            )
        }

        val latestObservation = observationRepository.observeForEdition(editionId).first().firstOrNull()
        val editions = _state.value.editions
        val rawRoutes = gameRepository.observeAllRoutes().first()
            .filter { route -> editions.any { it.id == route.gameEditionId } }
        _state.value = buildState(
            game = _state.value.game,
            editions = editions,
            routes = rawRoutes,
            recommendation = recommendation,
            latestObservation = latestObservation,
            loading = false,
        ).copy(fpsState = FpsMeasureUiState.Success(metrics.averageFps ?: 0.0))
    }

    private fun buildState(
        game: Game?,
        editions: List<GameEdition>,
        routes: List<ExecutionRoute>,
        recommendation: Recommendation?,
        latestObservation: Observation?,
        loading: Boolean,
    ): GameUiState {
        val evaluationByRoute = recommendation?.evaluations?.associateBy { it.executionRouteId }
        return GameUiState(
            game = game,
            editions = editions,
            routes = routes.map { route ->
                val evaluation = evaluationByRoute?.get(route.id)
                RouteRowUi(
                    route = route,
                    emulatorName = registry.displayName(route.emulatorId),
                    platformName = registry.platformName(route.platformId),
                    averageFps = evaluation?.metrics?.averageFps,
                    isRecommended = route.id == recommendation?.recommendedRouteId,
                    disqualified = evaluation?.disqualified,
                )
            },
            recommendation = recommendation,
            loading = loading,
            latestObservation = latestObservation,
        )
    }
}
