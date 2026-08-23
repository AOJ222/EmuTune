package com.emutune.ui.addgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.emulator.EmulatorRegistry
import com.emutune.data.repo.ActivityRepository
import com.emutune.data.repo.DeviceRepository
import com.emutune.data.repo.GameRepository
import com.emutune.data.repo.newGame
import com.emutune.model.activity.ActivityEventType
import com.emutune.model.game.GameEdition
import com.emutune.model.game.Platform
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.GameId
import com.emutune.model.ids.PlatformId
import com.emutune.model.route.Emulator
import com.emutune.model.route.ExecutionRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A platform + optional emulator pairing offered by the registry for a new edition. */
data class PlatformOption(
    val platform: Platform,
    val emulators: List<Emulator>,
)

data class AddGameUiState(
    val title: String = "",
    val titleError: String? = null,
    val platforms: List<PlatformOption> = emptyList(),
    val selectedPlatformId: PlatformId? = null,
    val editionName: String = "",
    val platformIdentifier: String = "",
    val selectedEmulatorId: EmulatorId? = null,
    val duplicateError: String? = null,
    val saving: Boolean = false,
    val savedGameId: GameId? = null,
    /** Emulators actually installed on this device (distinct from those merely known). */
    val installedEmulatorIds: Set<EmulatorId> = emptySet(),
)

@HiltViewModel
class AddGameViewModel @Inject constructor(
    private val gameRepository: GameRepository,
    private val activityRepository: ActivityRepository,
    deviceRepository: DeviceRepository,
    registry: EmulatorRegistry,
) : ViewModel() {

    private val _state = MutableStateFlow(AddGameUiState())
    val state: StateFlow<AddGameUiState> = _state

    init {
        val platforms = registry.platforms().map { platform ->
            PlatformOption(
                platform = platform,
                emulators = registry.all()
                    .filter { emulator -> platform.id in emulator.supportedPlatformIds },
            )
        }
        _state.update { it.copy(platforms = platforms) }

        viewModelScope.launch {
            val installed = deviceRepository.observeInstallations().first()
                .map { it.emulatorId }
                .toSet()
            _state.update { it.copy(installedEmulatorIds = installed) }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, titleError = null, duplicateError = null) }

    fun onEditionNameChange(value: String) = _state.update { it.copy(editionName = value) }

    fun onPlatformIdentifierChange(value: String) = _state.update { it.copy(platformIdentifier = value) }

    fun onPlatformSelect(platformId: PlatformId) = _state.update {
        it.copy(selectedPlatformId = platformId, selectedEmulatorId = null)
    }

    fun onEmulatorSelect(emulatorId: EmulatorId) = _state.update { it.copy(selectedEmulatorId = emulatorId) }

    fun save() {
        val title = _state.value.title.trim()
        val platformId = _state.value.selectedPlatformId
        val titleError = if (title.isEmpty()) "Enter a game title" else null
        if (titleError != null || platformId == null) {
            _state.update { it.copy(titleError = titleError) }
            return
        }

        viewModelScope.launch {
            val existing = gameRepository.findByTitle(title)
            if (existing != null) {
                _state.update { it.copy(duplicateError = "This game is already in your library") }
                return@launch
            }

            _state.update { it.copy(saving = true) }

            val platformName = _state.value.platforms
                .firstOrNull { it.platform.id == platformId }
                ?.platform
                ?.name
                ?: platformId.value
            val editionName = _state.value.editionName.trim().ifEmpty { platformName }
            val edition = GameEdition(
                id = GameEditionId(0),
                gameId = GameId(0),
                platformId = platformId,
                name = editionName,
                platformIdentifier = _state.value.platformIdentifier.trim().ifBlank { null },
            )
            val route = _state.value.selectedEmulatorId?.let { emulatorId ->
                ExecutionRoute(
                    id = com.emutune.model.ids.ExecutionRouteId(0),
                    gameEditionId = GameEditionId(0),
                    platformId = platformId,
                    emulatorId = emulatorId,
                )
            }

            val gameId = gameRepository.addGame(newGame(title), edition, route)
            activityRepository.record(
                type = ActivityEventType.GAME_ADDED,
                title = "Added $title",
                detail = editionName,
                gameId = gameId.value,
            )
            _state.update { it.copy(saving = false, savedGameId = gameId) }
        }
    }
}
