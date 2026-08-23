package com.emutune.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.emutune.designsystem.theme.EmuTuneTheme
import com.emutune.ui.addgame.AddGameScreen
import com.emutune.ui.game.GameScreen
import com.emutune.ui.navigation.AddGameDestination
import com.emutune.ui.navigation.GameDestination
import com.emutune.ui.navigation.MainDestination

/**
 * Root composable. Owns the navigation back stack: the top-level [MainDestination]
 * hosts the tabbed Home/Library/Device/Settings areas; [GameDestination],
 * [AddGameDestination] and [ActivityDestination] are pushed as detail screens.
 */
@Composable
fun EmuTuneApp(onMeasureFps: () -> Unit = {}) {
    EmuTuneTheme {
        val backStack = rememberNavBackStack(MainDestination)
        fun pop() {
            if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
        }

        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<MainDestination> {
                    MainScreen(
                        onOpenGame = { gameId -> backStack.add(GameDestination(gameId)) },
                        onAddGame = { backStack.add(AddGameDestination) },
                    )
                }
                entry<GameDestination> { key ->
                    GameScreen(
                        gameId = key.gameId,
                        onBack = { pop() },
                        onMeasureFps = onMeasureFps,
                    )
                }
                entry<AddGameDestination> {
                    AddGameScreen(
                        onBack = { pop() },
                        onSaved = { gameId ->
                            // Replace the add screen with the new game's detail page.
                            pop()
                            backStack.add(GameDestination(gameId))
                        },
                    )
                }
            },
        )
    }
}
