package com.emutune.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.emutune.designsystem.theme.Spacing

/**
 * A four-segment confidence indicator with a label. The fraction (0..1) and colour are
 * supplied by the caller; the numeric-to-level thresholding stays in the domain.
 */
@Composable
fun ConfidenceIndicator(
    label: String,
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(4) { segment ->
                val filled = fraction >= (segment + 1) / 4f
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (filled) color else color.copy(alpha = 0.18f)),
                )
            }
        }
        Spacer(modifier = Modifier.width(Spacing.S))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
        )
    }
}
