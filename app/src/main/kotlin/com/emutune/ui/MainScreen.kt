package com.emutune.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Radius
import com.emutune.designsystem.theme.Spacing
import com.emutune.ui.device.DeviceScreen
import com.emutune.ui.home.HomeScreen
import com.emutune.ui.library.LibraryScreen
import com.emutune.ui.settings.SettingsScreen

/** The four primary areas reachable from the top-level tab bar. */
enum class TopDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    LIBRARY("Library", Icons.AutoMirrored.Outlined.MenuBook),
    DEVICE("Device", Icons.Outlined.Smartphone),
    SETTINGS("Settings", Icons.Outlined.Settings),
}

@Composable
fun MainScreen(
    onOpenGame: (Long) -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    var selected by remember { mutableStateOf(TopDestination.HOME) }

    Scaffold(
        containerColor = EmuTuneColors.Background,
        topBar = { TopTabBar(selected = selected, onSelect = { selected = it }) },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                TopDestination.HOME -> HomeScreen(onOpenGame = onOpenGame)
                TopDestination.LIBRARY -> LibraryScreen(onOpenGame = onOpenGame)
                TopDestination.DEVICE -> DeviceScreen()
                TopDestination.SETTINGS -> SettingsScreen()
            }
        }
    }
}

@Composable
private fun TopTabBar(selected: TopDestination, onSelect: (TopDestination) -> Unit) {
    Surface(color = EmuTuneColors.Surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.S, vertical = Spacing.XS),
        ) {
            TopDestination.entries.forEach { destination ->
                TabItem(
                    destination = destination,
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TabItem(
    destination: TopDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(Radius.S)

    Column(
        modifier = modifier
            .clip(shape)
            .focusRequester(focusRequester)
            .onFocusChanged { focused = it.isFocused }
            .clickable(onClick = onClick)
            .background(if (selected) EmuTuneColors.Accent.copy(alpha = 0.12f) else Color.Transparent)
            .border(
                width = 1.dp,
                color = when {
                    focused -> EmuTuneColors.Accent
                    selected -> EmuTuneColors.BorderStrong
                    else -> Color.Transparent
                },
                shape = shape,
            )
            .padding(vertical = Spacing.S),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = destination.icon,
            contentDescription = destination.label,
            tint = if (selected) EmuTuneColors.Accent else EmuTuneColors.TextSecondary,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = destination.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) EmuTuneColors.Accent else EmuTuneColors.TextSecondary,
        )
    }
}
