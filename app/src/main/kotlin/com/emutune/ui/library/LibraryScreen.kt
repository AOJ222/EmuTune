package com.emutune.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
    onAddGame: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel(),
) {
    val games by viewModel.games.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        Button(
            onClick = onAddGame,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.L, vertical = Spacing.S),
        ) {
            Icon(imageVector = Icons.Outlined.Add, contentDescription = null)
            Text("  Add Game")
        }

        if (games.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(Spacing.L),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "No games yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmuTuneColors.TextPrimary,
                )
                Text(
                    text = "Add your first game to see its best verified route.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.L),
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
}
