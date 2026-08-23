package com.emutune.ui.addgame

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.component.SectionTitle
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing

@Composable
fun AddGameScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: AddGameViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedGameId) {
        state.savedGameId?.let { onSaved(it.value) }
    }

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
                text = "Add Game",
                style = MaterialTheme.typography.titleLarge,
                color = EmuTuneColors.TextPrimary,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.L),
            verticalArrangement = Arrangement.spacedBy(Spacing.L),
        ) {
            SectionTitle("TITLE")
            TitleField(state, viewModel::onTitleChange)

            SectionTitle("PLATFORM")
            PlatformSelector(state, viewModel::onPlatformSelect)

            SectionTitle("EDITION")
            OpticSurface(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    OutlinedTextField(
                        value = state.editionName,
                        onValueChange = viewModel::onEditionNameChange,
                        label = { Text("Edition name (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                    OutlinedTextField(
                        value = state.platformIdentifier,
                        onValueChange = viewModel::onPlatformIdentifierChange,
                        label = { Text("Platform ID (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                    )
                }
            }

            val emulators = state.platforms
                .firstOrNull { it.platform.id == state.selectedPlatformId }
                ?.emulators
                .orEmpty()
            if (emulators.isNotEmpty()) {
                SectionTitle("AVAILABLE EMULATOR")
                EmulatorSelector(state, emulators, viewModel::onEmulatorSelect)
            }

            state.titleError?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = EmuTuneColors.Danger)
            }
            state.duplicateError?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = EmuTuneColors.Danger)
            }

            Button(
                onClick = viewModel::save,
                enabled = !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        modifier = Modifier.height(18.dp),
                        color = EmuTuneColors.Background,
                    )
                } else {
                    Text("ADD TO LIBRARY")
                }
            }
        }
    }
}

@Composable
private fun TitleField(state: AddGameUiState, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = state.title,
        onValueChange = onChange,
        label = { Text("Game title") },
        singleLine = true,
        isError = state.titleError != null || state.duplicateError != null,
        modifier = Modifier.fillMaxWidth(),
        colors = fieldColors(),
    )
}

@Composable
private fun PlatformSelector(state: AddGameUiState, onSelect: (com.emutune.model.ids.PlatformId) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        state.platforms.forEach { option ->
            val selected = option.platform.id == state.selectedPlatformId
            OpticSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(option.platform.id) },
            ) {
                Text(
                    text = option.platform.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) EmuTuneColors.Accent else EmuTuneColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun EmulatorSelector(
    state: AddGameUiState,
    emulators: List<com.emutune.model.route.Emulator>,
    onSelect: (com.emutune.model.ids.EmulatorId) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
        emulators.forEach { emulator ->
            val selected = emulator.id == state.selectedEmulatorId
            OpticSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(emulator.id) },
            ) {
                Text(
                    text = emulator.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) EmuTuneColors.Accent else EmuTuneColors.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmuTuneColors.Accent,
    unfocusedBorderColor = EmuTuneColors.BorderStrong,
    focusedLabelColor = EmuTuneColors.Accent,
    unfocusedLabelColor = EmuTuneColors.TextSecondary,
    cursorColor = EmuTuneColors.Accent,
    focusedTextColor = EmuTuneColors.TextPrimary,
    unfocusedTextColor = EmuTuneColors.TextPrimary,
    errorBorderColor = EmuTuneColors.Danger,
    errorLabelColor = EmuTuneColors.Danger,
)
