package com.emutune.data.benchmark

import com.emutune.model.benchmark.BenchmarkCapability
import com.emutune.model.benchmark.BenchmarkMetric
import com.emutune.model.benchmark.BenchmarkProvider
import com.emutune.model.evidence.EvidenceGrade
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The screen-capture frame-delta provider: measures FPS by observing screen updates
 * through MediaProjection. This is a Tier-1 (no-root) measurement source — the same
 * mechanism screen recorders and dual-screen mapping tools use.
 *
 * Capability is declared honestly: it produces frame-rate metrics at
 * [EvidenceGrade.C_PARTIAL_MEASUREMENT] quality, and it measures the *screen update
 * rate*, not the emulator's internal render rate (a 30 FPS game on a 60 Hz display
 * still refreshes at 60). The actual capture happens in the app's
 * [com.emutune.benchmark.FpsCaptureService]; this provider is the single authoritative
 * declaration of what that capture can measure.
 */
@Singleton
class ScreenCaptureFpsProvider @Inject constructor() : BenchmarkProvider {

    override val id: String = "screen-capture-frame-delta"

    override val capability: BenchmarkCapability = BenchmarkCapability(
        producedMetrics = setOf(
            BenchmarkMetric.AVERAGE_FPS,
            BenchmarkMetric.ONE_PERCENT_LOW,
            BenchmarkMetric.FRAME_TIME_VARIANCE,
            BenchmarkMetric.STUTTER_RATE,
        ),
        quality = EvidenceGrade.C_PARTIAL_MEASUREMENT,
        environmentRequirements = listOf(
            "User-granted screen capture consent (one system dialog per session)",
            "A visible capture indicator while measuring",
        ),
        limitations = listOf(
            "Measures screen update rate, not the emulator's internal render rate",
            "Cannot distinguish a game frame from a UI overlay update",
        ),
    )
}
