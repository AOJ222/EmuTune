package com.emutune.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.emulator.EmulatorRegistry
import com.emutune.data.repo.GameRepository
import com.emutune.data.repo.RecommendationRepository
import com.emutune.model.game.Game
import com.emutune.model.game.GameEdition
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.PlatformId
import com.emutune.model.recommendation.DisqualificationReason
import com.emutune.model.recommendation.OptimizationGoal
import com.emutune.model.recommendation.Recommendation
import com.emutune.model.route.ExecutionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RouteRowUi(
    val route: ExecutionRoute,
    val emulatorName: String,
    val platformName: String,
    val averageFps: Double?,
    val isRecommended: Boolean,
    val disqualified: DisqualificationReason?,
)

data class GameUiState(
    val game: Game? = null,
    val editions: List<GameEdition> = emptyList(),
    val routes: List<RouteRowUi> = emptyList(),
    val recommendation: Recommendation? = null,
    val loading: Boolean = true,
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val recommendationRepository: RecommendationRepository,
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
                        emulatorName = emulatorName(route.emulatorId),
                        platformName = platformName(route.platformId),
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

    fun emulatorName(id: EmulatorId): String = registry.find(id)?.displayName ?: id.value

    fun platformName(id: PlatformId): String = registry.platform(id)?.name ?: id.value
}
