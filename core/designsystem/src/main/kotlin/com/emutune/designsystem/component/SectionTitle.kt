package com.emutune.designsystem.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.emutune.designsystem.theme.EmuTuneColors

/**
 * A section heading used to label a group of surfaces. Shared across screens so the
 * label typography stays consistent and no screen defines its own copy.
 */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = EmuTuneColors.TextSecondary,
        modifier = modifier,
    )
}
