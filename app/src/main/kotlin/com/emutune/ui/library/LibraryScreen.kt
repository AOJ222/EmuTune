package com.emutune.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing

@Composable
fun LibraryScreen(
    onOpenGame: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val games by viewModel.games.collectAsStateWithLifecycle()

    if (games.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(Spacing.L),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "No games yet",
                style = MaterialTheme.typography.titleLarge,
                color = EmuTuneColors.TextPrimary,
            )
            Text(
                text = "Games you add will appear here. Manual addition is the honest first step — universal detection is not assumed.",
                style = MaterialTheme.typography.bodyMedium,
                color = EmuTuneColors.TextSecondary,
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(Spacing.L),
        verticalArrangement = Arrangement.spacedBy(Spacing.M),
    ) {
        items(games, key = { it.id.value }) { game ->
            OpticSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenGame(game.id.value) },
            ) {
                Text(
                    text = game.title,
                    style = MaterialTheme.typography.titleLarge,
                    color = EmuTuneColors.TextPrimary,
                )
            }
        }
    }
}
