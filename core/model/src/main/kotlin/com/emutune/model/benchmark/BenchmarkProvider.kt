package com.emutune.model.benchmark

import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.EvidenceGrade

/** A single performance axis a provider is capable of measuring. */
enum class BenchmarkMetric {
    AVERAGE_FPS,
    ONE_PERCENT_LOW,
    POINT_ONE_PERCENT_LOW,
    FRAME_TIME_VARIANCE,
    STUTTER_RATE,
}

/**
 * What a provider declares it can measure, at what evidence quality, and under what
 * constraints. A provider never produces a metric outside this declaration, and a
 * missing metric stays absent rather than being synthesised.
 */
data class BenchmarkCapability(
    val producedMetrics: Set<BenchmarkMetric>,
    val quality: EvidenceGrade,
    val environmentRequirements: List<String> = emptyList(),
    val limitations: List<String> = emptyList(),
)

/** Outcome of a measurement run. */
sealed interface BenchmarkResult {
    data class Success(val metrics: BenchmarkMetrics, val grade: EvidenceGrade) : BenchmarkResult
    data class Failure(val reason: String) : BenchmarkResult
}

/**
 * One captured frame observation. [changed] is true when this frame's pixels differ
 * from the immediately preceding capture — i.e. the game produced a new frame. The
 * first entry in a list is the first *comparable* sample (there is no baseline entry).
 */
data class FrameSample(
    val timestampNanos: Long,
    val changed: Boolean,
)

/**
 * A source of measured performance data. Implementations declare their
 * [BenchmarkCapability] and fail closed. The screen-capture frame-delta provider is
 * one Tier-1 (no-root) implementation; Lab Runner / Perfetto are future providers.
 */
interface BenchmarkProvider {
    val id: String
    val capability: BenchmarkCapability
}
