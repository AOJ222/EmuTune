package com.emutune.model.evidence

import com.emutune.model.device.DeviceEnvironment
import com.emutune.model.device.HardwareMatchQuality
import com.emutune.model.ids.BenchmarkSessionId
import com.emutune.model.ids.EvidenceId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.ObservationId
import java.time.Instant
import kotlinx.serialization.Serializable

/**
 * A set of measured metrics for one route under one environment. Every numeric field
 * is nullable: an unavailable metric stays explicitly unavailable rather than being
 * synthesised to fill the model.
 */
@Serializable
data class BenchmarkMetrics(
    val averageFps: Double? = null,
    val onePercentLowFps: Double? = null,
    val pointOnePercentLowFps: Double? = null,
    val frameTimeVariance: Double? = null,
    val stutterRate: Double? = null,
    val sustainedFps: Double? = null,
    val thermalDegradation: Double? = null,
    val powerWatts: Double? = null,
    val visualQuality: VisualQuality? = null,
    val renderingCorrectness: RenderingCorrectness? = null,
    val compatibilityDefects: List<CompatibilityDefect> = emptyList(),
) {
    /** True when at least one numeric performance metric is present. */
    val hasAnyMeasurement: Boolean
        get() = averageFps != null ||
            onePercentLowFps != null ||
            sustainedFps != null ||
            frameTimeVariance != null
}

/** Resolution/quality tier a run was captured at. */
@Serializable
enum class VisualQuality {
    NATIVE,
    SUPERSAMPLED,
    UPSCALED,
    NATIVE_HALF,
    NATIVE_QUARTER,
    UNKNOWN,
}

/** Whether the run rendered correctly. */
@Serializable
enum class RenderingCorrectness {
    CORRECT,
    MINOR_DEFECTS,
    MAJOR_DEFECTS,
    UNKNOWN,
}

/** Structured compatibility failures that can disqualify a route in Stage 1. */
@Serializable
enum class CompatibilityDefect {
    FAILS_TO_BOOT,
    CRASHES,
    GRAPHICAL_CORRUPTION,
    ARCHITECTURALLY_INCOMPATIBLE,
    UNSUPPORTED_DRIVER,
    SEVERE_CORRUPTION,
    INPUT_DEFECT,
    AUDIO_DEFECT,
}

/** How a benchmark was produced. Grade A is not interchangeable with Grade E. */
@Serializable
enum class EvidenceGrade {
    A_DETERMINISTIC_BENCHMARK,
    B_MACHINE_GAMEPLAY,
    C_PARTIAL_MEASUREMENT,
    D_USER_SUBMITTED,
    E_EXTERNAL_RESEARCH,
}

/** The measurement method recorded for an observation. */
enum class BenchmarkMethod {
    MACHINE_BENCHMARK,
    MACHINE_GAMEPLAY,
    PARTIAL_MEASUREMENT,
    USER_SUBMITTED,
    EXTERNAL_RESEARCH,
}

/**
 * An immutable historical fact: something that was actually measured or reliably
 * established under a specific environment. Never mutated when a new build ships —
 * a new observation is recorded instead.
 */
data class Observation(
    val id: ObservationId = ObservationId(0),
    val gameEditionId: GameEditionId,
    val executionRouteId: ExecutionRouteId,
    val environment: DeviceEnvironment,
    val metrics: BenchmarkMetrics,
    val durationSeconds: Int? = null,
    val benchmarkMethod: BenchmarkMethod,
    val evidenceGrade: EvidenceGrade,
    val success: Boolean = true,
    val crashCount: Int = 0,
    val recordedAt: Instant,
    /** Which provider produced this observation (e.g. "screen-capture-frame-delta"). */
    val providerId: String? = null,
)

/**
 * A benchmark session: the container that produced observations. It records identity,
 * the route/config hash, timing, environment, provider, and success/failure, so an
 * observation can be traced back to the run that produced it.
 */
data class BenchmarkSession(
    val id: BenchmarkSessionId = BenchmarkSessionId(0),
    val executionRouteId: ExecutionRouteId,
    val routeConfigHash: String,
    val startedAt: Instant,
    val endedAt: Instant? = null,
    val environment: DeviceEnvironment,
    val providerId: String,
    val metrics: BenchmarkMetrics,
    val evidenceGrade: EvidenceGrade,
    val success: Boolean,
)

/**
 * The assessed evidentiary contribution of one [Observation] toward a recommendation
 * for a specific target environment. Derived at recommendation time — evidence is
 * computed, not persisted as a mutable table.
 */
data class Evidence(
    val id: EvidenceId = EvidenceId(0),
    val observationId: ObservationId,
    val gameEditionId: GameEditionId,
    val executionRouteId: ExecutionRouteId,
    val hardwareMatchQuality: HardwareMatchQuality,
    val weight: Double,
    val recencyFactor: Double,
)
