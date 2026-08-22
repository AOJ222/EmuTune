package com.emutune.designsystem.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.EmuTuneType

/**
 * A single measured value with an optional delta. The value renders in monospace so
 * numbers read as data rather than prose. Values are single-line and ellipsized so a
 * metric never wraps or spills into an adjacent column.
 */
@Composable
fun Metric(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = EmuTuneColors.TextPrimary,
    delta: String? = null,
    deltaColor: Color = EmuTuneColors.Accent,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = EmuTuneColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFamily = EmuTuneType.Mono),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (delta != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = delta,
                style = MaterialTheme.typography.labelLarge,
                color = deltaColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
