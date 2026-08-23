package com.emutune.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.benchmark.FpsCaptureResult
import com.emutune.data.emulator.AdapterResolver
import com.emutune.data.emulator.EmulatorRegistry
import com.emutune.data.repo.GameRepository
import com.emutune.data.repo.LaunchOutcome
import com.emutune.data.repo.RecommendationRepository
import com.emutune.data.repo.SessionRepository
import com.emutune.model.benchmark.BenchmarkResult
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.GameId
import com.emutune.model.recommendation.DisqualificationReason
import com.emutune.model.recommendation.OptimizationGoal
import com.emutune.model.recommendation.Recommendation
import com.emutune.model.route.ExecutionRoute
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
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val recommendationRepository: RecommendationRepository,
    private val sessionRepository: SessionRepository,
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
            val evaluationByRoute = recommendation?.evaluations?.associateBy { it.executionRouteId }
            _state.value = GameUiState(
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
                loading = false,
            )
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
     * Measures the current game's frame rate from screen capture. [requestConsent] is
     * the Activity-level callback that launches the MediaProjection consent dialog; the
     * result is polled from the capture service once consent is granted.
     */
    fun startFpsMeasurement(requestConsent: () -> Unit) {
        if (_state.value.fpsState == FpsMeasureUiState.Measuring) return
        _state.value = _state.value.copy(fpsState = FpsMeasureUiState.Measuring)
        FpsCaptureResult.flow.value = null
        requestConsent()
        viewModelScope.launch {
            val result = withTimeoutOrNull(20_000) {
                FpsCaptureResult.flow.filterNotNull().first()
            }
            _state.value = _state.value.copy(
                fpsState = when (result) {
                    null -> FpsMeasureUiState.Failure("Timed out waiting for a measurement")
                    is BenchmarkResult.Success -> FpsMeasureUiState.Success(requireNotNull(result.metrics.averageFps))
                    is BenchmarkResult.Failure -> FpsMeasureUiState.Failure(result.reason)
                },
            )
        }
    }
}
