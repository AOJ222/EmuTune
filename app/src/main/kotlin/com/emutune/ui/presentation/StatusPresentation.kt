package com.emutune.ui.presentation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Update
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.emutune.designsystem.theme.EmuTuneColors
import com.emutune.model.recommendation.ConfidenceLevel
import com.emutune.model.recommendation.OptimizationStatus

/**
 * The single mapping from domain status and confidence to presentation values. Screens
 * never invent status strings, colours or icons — they ask this object.
 */
object StatusPresentation {

    fun label(status: OptimizationStatus): String = when (status) {
        OptimizationStatus.OPTIMAL -> "Optimal"
        OptimizationStatus.IMPROVEMENT_AVAILABLE -> "Improvement available"
        OptimizationStatus.BETTER_ROUTE_AVAILABLE -> "Better route available"
        OptimizationStatus.UPDATE_RECOMMENDED -> "Update recommended"
        OptimizationStatus.REGRESSION_DETECTED -> "Regression detected"
        OptimizationStatus.HARDWARE_LIMITED -> "Hardware limited"
        OptimizationStatus.UNVERIFIED -> "Unverified"
        OptimizationStatus.INSUFFICIENT_EVIDENCE -> "Insufficient evidence"
        OptimizationStatus.UNSUPPORTED -> "Unsupported"
    }

    fun color(status: OptimizationStatus): Color = when (status) {
        OptimizationStatus.OPTIMAL -> EmuTuneColors.Success
        OptimizationStatus.IMPROVEMENT_AVAILABLE,
        OptimizationStatus.BETTER_ROUTE_AVAILABLE,
        -> EmuTuneColors.Accent
        OptimizationStatus.UPDATE_RECOMMENDED,
        OptimizationStatus.HARDWARE_LIMITED,
        -> EmuTuneColors.Warning
        OptimizationStatus.REGRESSION_DETECTED -> EmuTuneColors.Danger
        OptimizationStatus.UNVERIFIED,
        OptimizationStatus.INSUFFICIENT_EVIDENCE,
        OptimizationStatus.UNSUPPORTED,
        -> EmuTuneColors.Neutral
    }

    fun icon(status: OptimizationStatus): ImageVector = when (status) {
        OptimizationStatus.OPTIMAL -> Icons.Outlined.CheckCircle
        OptimizationStatus.IMPROVEMENT_AVAILABLE,
        OptimizationStatus.BETTER_ROUTE_AVAILABLE,
        -> Icons.AutoMirrored.Outlined.TrendingUp
        OptimizationStatus.UPDATE_RECOMMENDED -> Icons.Outlined.Update
        OptimizationStatus.REGRESSION_DETECTED -> Icons.AutoMirrored.Outlined.TrendingDown
        OptimizationStatus.HARDWARE_LIMITED -> Icons.Outlined.Speed
        OptimizationStatus.UNVERIFIED,
        OptimizationStatus.INSUFFICIENT_EVIDENCE,
        OptimizationStatus.UNSUPPORTED,
        -> Icons.AutoMirrored.Outlined.HelpOutline
    }

    fun confidenceLabel(level: ConfidenceLevel): String = when (level) {
        ConfidenceLevel.LOW -> "Low"
        ConfidenceLevel.MEDIUM -> "Medium"
        ConfidenceLevel.HIGH -> "High"
        ConfidenceLevel.VERY_HIGH -> "Very high"
    }

    fun confidenceColor(level: ConfidenceLevel): Color = when (level) {
        ConfidenceLevel.LOW -> EmuTuneColors.Neutral
        ConfidenceLevel.MEDIUM -> EmuTuneColors.Warning
        ConfidenceLevel.HIGH -> EmuTuneColors.Accent
        ConfidenceLevel.VERY_HIGH -> EmuTuneColors.Success
    }

    fun confidenceFraction(level: ConfidenceLevel): Float = when (level) {
        ConfidenceLevel.LOW -> 0.25f
        ConfidenceLevel.MEDIUM -> 0.5f
        ConfidenceLevel.HIGH -> 0.75f
        ConfidenceLevel.VERY_HIGH -> 1f
    }
}
