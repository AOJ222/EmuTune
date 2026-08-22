package com.emutune.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.emutune.data.repo.GameRepository
import com.emutune.model.game.Game
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LibraryViewModel @Inject constructor(
    gameRepository: GameRepository,
) : ViewModel() {

    val games: StateFlow<List<Game>> = gameRepository.observeGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
