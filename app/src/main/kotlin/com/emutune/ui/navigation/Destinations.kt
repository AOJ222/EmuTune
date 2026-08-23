package com.emutune.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/** The single top-level destination hosting the Home/Library/Device/Settings tabs. */
@Serializable
data object MainDestination : NavKey

/** A game detail destination, keyed by the game's id. */
@Serializable
data class GameDestination(val gameId: Long) : NavKey

/** The Add Game destination (pushed from the Library). */
@Serializable
data object AddGameDestination : NavKey
