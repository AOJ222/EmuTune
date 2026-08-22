package com.emutune.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.designsystem.theme.Radius
import com.emutune.designsystem.theme.Spacing

/**
 * The foundational layered surface. A subtle vertical gradient over a dark base with
 * a hairline border gives the "optical glass" character without heavy blur or glow.
 *
 * Children stack vertically (a [Column]), so callers can pass several elements without
 * them painting over one another. Spacing between children is the caller's choice via
 * [Spacing] tokens or an inner arrangement.
 */
@Composable
fun OpticSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(Radius.M),
    contentPadding: PaddingValues = PaddingValues(Spacing.L),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(EmuTuneColors.SurfaceElevated, EmuTuneColors.Surface),
                ),
            )
            .border(1.dp, EmuTuneColors.Border, shape)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}
