package com.emutune.model.recommendation

import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.ids.RecommendationId
import java.time.Instant

/** The closed set of optimisation goals the system optimises for. */
enum class OptimizationGoal {
    BALANCED,
    MAX_PERFORMANCE,
    STABLE_30,
    STABLE_60,
    MAX_QUALITY_AT_30,
    MAX_QUALITY_AT_60,
    BATTERY_EFFICIENT,
}

/** The closed set of domain statuses. Presentation maps these centrally. */
enum class OptimizationStatus {
    OPTIMAL,
    IMPROVEMENT_AVAILABLE,
    BETTER_ROUTE_AVAILABLE,
    UPDATE_RECOMMENDED,
    REGRESSION_DETECTED,
    HARDWARE_LIMITED,
    UNVERIFIED,
    INSUFFICIENT_EVIDENCE,
    UNSUPPORTED,
}

/** Controlled, presentation-facing confidence levels. Thresholds are centralized. */
enum class ConfidenceLevel {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH,
}

/**
 * Confidence answers "how strongly does the evidence support this recommendation for
 * this exact environment?" It is *not* the recommendation score; the two are kept
 * separate so a high-confidence but modest recommendation is distinguishable from a
 * high-scoring but poorly-evidenced one.
 */
data class EvidenceConfidence(
    val level: ConfidenceLevel,
    val numeric: Double,
)

/** The deterministic recommendation score, kept separate from confidence. */
data class RecommendationScore(val value: Double)

/** Structured reasons the presentation layer converts into controlled language. */
enum class RecommendationReason {
    PERFORMANCE_GAIN,
    BETTER_FRAME_PACING,
    LOWER_THERMALS,
    HIGHER_VISUAL_QUALITY,
    STRONGER_EVIDENCE,
    BUILD_REGRESSION,
}

/** Stage 1 hard-compatibility disqualification reasons. */
enum class DisqualificationReason {
    FAILS_TO_BOOT,
    CRASHES_MATERIALLY,
    ARCHITECTURALLY_INCOMPATIBLE,
    UNSUPPORTED_DRIVER,
    SEVERE_CORRUPTION,
    INVALID_FOR_TARGET,
    FAILS_OPTIMIZATION_TARGET,
}

/** The evaluation of one route considered for a recommendation. */
data class RouteEvaluation(
    val executionRouteId: ExecutionRouteId,
    val gameEditionId: GameEditionId,
    val metrics: BenchmarkMetrics,
    val score: RecommendationScore,
    val confidence: EvidenceConfidence,
    val disqualified: DisqualificationReason? = null,
    val observationCount: Int = 0,
    val bestEvidenceGrade: EvidenceGrade? = null,
)

/** A comparison of the recommended route against the user's current route. */
data class RouteComparison(
    val currentRouteId: ExecutionRouteId,
    val recommendedRouteId: ExecutionRouteId,
    val currentAverageFps: Double?,
    val recommendedAverageFps: Double?,
    val percentChange: Double?,
)

/**
 * A derived conclusion over current evidence. Recommendations are disposable and
 * recomputable — the authoritative data is the evidence, never a mutable
 * `bestSettings` record.
 */
data class Recommendation(
    val id: RecommendationId = RecommendationId(0),
    val gameEditionId: GameEditionId,
    val goal: OptimizationGoal,
    val recommendedRouteId: ExecutionRouteId?,
    val score: RecommendationScore?,
    val confidence: EvidenceConfidence,
    val status: OptimizationStatus,
    val reasons: List<RecommendationReason> = emptyList(),
    val evaluations: List<RouteEvaluation> = emptyList(),
    val comparison: RouteComparison? = null,
    val computedAt: Instant,
)
