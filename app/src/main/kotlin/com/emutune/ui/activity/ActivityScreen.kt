package com.emutune.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.emutune.designsystem.component.OpticSurface
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Spacing
import com.emutune.model.activity.ActivityEvent
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    .withZone(ZoneId.systemDefault())

@Composable
fun ActivityScreen(
    onBack: (() -> Unit)? = null,
    viewModel: ActivityViewModel = hiltViewModel(),
) {
    val events by viewModel.events.collectAsStateWithLifecycle()

    Column(modifier = Modifier.fillMaxSize()) {
        if (onBack != null) {
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
                    text = "Activity",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmuTuneColors.TextPrimary,
                )
            }
        }

        if (events.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(Spacing.L),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Nothing yet",
                    style = MaterialTheme.typography.titleLarge,
                    color = EmuTuneColors.TextPrimary,
                )
                Text(
                    text = "Measurements, recommendations and changes will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(Spacing.L),
            verticalArrangement = Arrangement.spacedBy(Spacing.M),
        ) {
            items(events, key = { it.id.value }) { event ->
                ActivityRow(event)
            }
        }
    }
}

@Composable
private fun ActivityRow(event: ActivityEvent) {
    OpticSurface(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = dateFormatter.format(event.occurredAt),
                style = MaterialTheme.typography.labelMedium,
                color = EmuTuneColors.TextTertiary,
            )
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium,
                color = EmuTuneColors.TextPrimary,
            )
            event.detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = EmuTuneColors.TextSecondary,
                )
            }
        }
    }
}
