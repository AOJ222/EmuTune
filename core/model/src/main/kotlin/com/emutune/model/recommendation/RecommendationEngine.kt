package com.emutune.model.recommendation

import com.emutune.model.device.DeviceFingerprint
import com.emutune.model.device.HardwareMatchQuality
import com.emutune.model.device.HardwareMatcher
import com.emutune.model.evidence.BenchmarkMetrics
import com.emutune.model.evidence.CompatibilityDefect
import com.emutune.model.evidence.EvidenceGrade
import com.emutune.model.evidence.Observation
import com.emutune.model.evidence.RenderingCorrectness
import com.emutune.model.evidence.VisualQuality
import com.emutune.model.ids.DeviceFingerprintId
import com.emutune.model.ids.EmulatorBuildId
import com.emutune.model.ids.EmulatorId
import com.emutune.model.ids.ExecutionRouteId
import com.emutune.model.ids.GameEditionId
import com.emutune.model.route.ExecutionRoute
import java.time.Clock
import java.time.Duration
import kotlin.math.exp
import kotlin.math.pow

/** Per-goal scoring weights and target floor. Weights sum to 1.0. */
data class GoalWeights(
    val performance: Double,
    val stability: Double,
    val sustained: Double,
    val quality: Double,
    val power: Double,
    val targetFps: Double?,
)

data class RecommendationConfig(
    val performanceReferenceFps: Double = 60.0,
    val recencyHalfLifeDays: Double = 180.0,
    val staleBuildFactor: Double = 0.6,
    val crashDisqualifyThreshold: Int = 3,
    val recommendableConfidenceThreshold: Double = 0.45,
    val goalWeights: Map<OptimizationGoal, GoalWeights> = defaultGoalWeights(),
)

fun defaultGoalWeights(): Map<OptimizationGoal, GoalWeights> = mapOf(
    OptimizationGoal.BALANCED to GoalWeights(0.30, 0.40, 0.15, 0.15, 0.0, null),
    OptimizationGoal.MAX_PERFORMANCE to GoalWeights(0.75, 0.10, 0.05, 0.10, 0.0, null),
    OptimizationGoal.STABLE_30 to GoalWeights(0.15, 0.50, 0.20, 0.15, 0.0, 30.0),
    OptimizationGoal.STABLE_60 to GoalWeights(0.15, 0.50, 0.20, 0.15, 0.0, 60.0),
    OptimizationGoal.MAX_QUALITY_AT_30 to GoalWeights(0.10, 0.25, 0.15, 0.50, 0.0, 30.0),
    OptimizationGoal.MAX_QUALITY_AT_60 to GoalWeights(0.10, 0.25, 0.15, 0.50, 0.0, 60.0),
    OptimizationGoal.BATTERY_EFFICIENT to GoalWeights(0.20, 0.25, 0.25, 0.10, 0.20, null),
)

data class RecommendationRequest(
    val gameEditionId: GameEditionId,
    val goal: OptimizationGoal,
    val routes: List<ExecutionRoute>,
    val observations: List<Observation>,
    val targetDevice: DeviceFingerprint,
    val observationDevices: Map<DeviceFingerprintId, DeviceFingerprint> = emptyMap(),
    val comparableEditionIds: Set<GameEditionId> = emptySet(),
    val latestBuilds: Map<EmulatorId, EmulatorBuildId> = emptyMap(),
    val currentRouteId: ExecutionRouteId? = null,
)

/**
 * A deterministic, staged recommendation engine. There is no machine-learning in the
 * ranking path. Stages run in order: hard compatibility, optimisation target,
 * stability, sustained behaviour, quality, confidence.
 *
 * ## Invariants
 *
 * The ranking is a deterministic function of the request: identical inputs produce
 * identical recommendations. Each product rule below is an arithmetic invariant that
 * `RecommendationEngineTest` asserts directly — if one breaks, a test fails.
 *
 * - A candidate with a higher average FPS but substantially worse 1% lows loses under
 *   `BALANCED` (frame pacing dominates stability, and stability dominates BALANCED).
 * - `MAX_PERFORMANCE` selects the highest-scoring valid candidate; performance weight
 *   dominates so the highest sustainable FPS wins.
 * - `STABLE_30`/`STABLE_60` disqualify a candidate whose sustained FPS (falling back
 *   to average) cannot reach the target, even if it spikes higher transiently.
 * - Exact-device evidence outranks weaker hardware matches; stale-build evidence is
 *   discounted; crash-heavy routes are penalised and, past the threshold, disqualified.
 * - A materially different edition is never ranked against the requested edition.
 * - A provided `currentRouteId` produces `BETTER_ROUTE_AVAILABLE` (with a comparison)
 *   when the best route beats it, and `OPTIMAL` when it is already best.
 */
class RecommendationEngine(
    private val config: RecommendationConfig = RecommendationConfig(),
    private val confidenceModel: ConfidenceModel = ConfidenceModel(),
    private val hardwareMatcher: HardwareMatcher = HardwareMatcher(),
    private val clock: Clock = Clock.systemUTC(),
) {

    fun recommend(request: RecommendationRequest): Recommendation {
        val eligible = request.routes.filter { route ->
            route.gameEditionId == request.gameEditionId ||
                route.gameEditionId in request.comparableEditionIds
        }

        val observationsByRoute = request.observations.groupBy { it.executionRouteId }
        val evaluations = eligible.map { route ->
            evaluateRoute(route, observationsByRoute[route.id].orEmpty(), request)
        }

        val best = evaluations
            .filter { it.disqualified == null }
            .filter { it.confidence.numeric >= config.recommendableConfidenceThreshold }
            .maxByOrNull { it.score.value }

        val hasAnyEvidence = evaluations.any { it.observationCount > 0 }

        val status = when {
            !hasAnyEvidence -> OptimizationStatus.INSUFFICIENT_EVIDENCE
            best == null -> OptimizationStatus.UNVERIFIED
            request.currentRouteId == null -> OptimizationStatus.OPTIMAL
            best.executionRouteId == request.currentRouteId -> OptimizationStatus.OPTIMAL
            else -> OptimizationStatus.BETTER_ROUTE_AVAILABLE
        }

        val reasons = if (best != null) {
            buildReasons(best, evaluations, request)
        } else {
            emptyList()
        }

        val comparison = if (best != null && request.currentRouteId != null) {
            buildComparison(best, evaluations, request)
        } else {
            null
        }

        return Recommendation(
            gameEditionId = request.gameEditionId,
            goal = request.goal,
            recommendedRouteId = best?.executionRouteId,
            score = best?.score,
            confidence = best?.confidence ?: EvidenceConfidence(ConfidenceLevel.LOW, 0.0),
            status = status,
            reasons = reasons,
            evaluations = evaluations.sortedByDescending { it.score.value },
            comparison = comparison,
            computedAt = clock.instant(),
        )
    }

    private fun evaluateRoute(
        route: ExecutionRoute,
        observations: List<Observation>,
        request: RecommendationRequest,
    ): RouteEvaluation {
        if (observations.isEmpty()) {
            return RouteEvaluation(
                executionRouteId = route.id,
                gameEditionId = route.gameEditionId,
                metrics = BenchmarkMetrics(),
                score = RecommendationScore(0.0),
                confidence = EvidenceConfidence(ConfidenceLevel.LOW, 0.0),
                observationCount = 0,
            )
        }

        val now = clock.instant()
        val weighted = observations.map { obs ->
            val sampleDevice = request.observationDevices[obs.environment.deviceFingerprintId]
            val matchQuality = if (sampleDevice != null) {
                hardwareMatcher.match(request.targetDevice, sampleDevice)
            } else {
                HardwareMatchQuality.UNKNOWN_HARDWARE
            }
            val recency = recencyFactor(obs, now)
            val staleness = buildStalenessFactor(route, request)
            WeightedObservation(obs, matchQuality, recency, staleness)
        }

        val metrics = aggregateMetrics(weighted)
        val disqualification = stage1HardCompatibility(observations)
            ?: stage2Target(metrics, request.goal)

        val samples = weighted.map { w ->
            EvidenceSample(
                grade = w.observation.evidenceGrade,
                hardwareMatchQuality = w.matchQuality,
                recencyFactor = w.recency,
                buildStalenessFactor = w.staleness,
                success = w.observation.success,
                primaryMetric = w.observation.metrics.averageFps,
            )
        }
        val confidence = confidenceModel.compute(samples)

        val score = if (disqualification == null) {
            RecommendationScore(scoreFor(metrics, observations, request.goal))
        } else {
            RecommendationScore(0.0)
        }

        val bestGrade = weighted.map { it.observation.evidenceGrade }.minByOrNull { it.ordinal }

        return RouteEvaluation(
            executionRouteId = route.id,
            gameEditionId = route.gameEditionId,
            metrics = metrics,
            score = score,
            confidence = confidence,
            disqualified = disqualification,
            observationCount = observations.size,
            bestEvidenceGrade = bestGrade,
        )
    }

    private data class WeightedObservation(
        val observation: Observation,
        val matchQuality: HardwareMatchQuality,
        val recency: Double,
        val staleness: Double,
    )

    private fun evidenceWeight(w: WeightedObservation): Double =
        EvidenceWeights.gradeWeight(w.observation.evidenceGrade) *
            EvidenceWeights.matchWeight(w.matchQuality) *
            w.recency *
            w.staleness

    private fun recencyFactor(observation: Observation, now: java.time.Instant): Double {
        val ageDays = Duration.between(observation.recordedAt, now).toDays().toDouble().coerceAtLeast(0.0)
        return 0.5.pow(ageDays / config.recencyHalfLifeDays)
    }

    private fun buildStalenessFactor(route: ExecutionRoute, request: RecommendationRequest): Double {
        val buildId = route.emulatorBuildId ?: return 1.0
        val latest = request.latestBuilds[route.emulatorId] ?: return 1.0
        return if (buildId == latest) 1.0 else config.staleBuildFactor
    }

    private fun aggregateMetrics(weighted: List<WeightedObservation>): BenchmarkMetrics {
        val weights = weighted.map { evidenceWeight(it) }
        val total = weights.sum()
        if (total <= 0.0) return BenchmarkMetrics()

        fun weightedAvg(selector: (BenchmarkMetrics) -> Double?): Double? {
            var sum = 0.0
            var wsum = 0.0
            weighted.forEachIndexed { i, w ->
                val v = selector(w.observation.metrics) ?: return@forEachIndexed
                sum += v * weights[i]
                wsum += weights[i]
            }
            return if (wsum > 0.0) sum / wsum else null
        }

        val defects = weighted
            .flatMap { it.observation.metrics.compatibilityDefects }
            .distinct()
        val visualQuality = weighted
            .mapNotNull { it.observation.metrics.visualQuality }
            .lastOrNull()
        val renderingCorrectness = weighted
            .mapNotNull { it.observation.metrics.renderingCorrectness }
            .lastOrNull()

        return BenchmarkMetrics(
            averageFps = weightedAvg { it.averageFps },
            onePercentLowFps = weightedAvg { it.onePercentLowFps },
            pointOnePercentLowFps = weightedAvg { it.pointOnePercentLowFps },
            frameTimeVariance = weightedAvg { it.frameTimeVariance },
            stutterRate = weightedAvg { it.stutterRate },
            sustainedFps = weightedAvg { it.sustainedFps },
            thermalDegradation = weightedAvg { it.thermalDegradation },
            powerWatts = weightedAvg { it.powerWatts },
            visualQuality = visualQuality,
            renderingCorrectness = renderingCorrectness,
            compatibilityDefects = defects,
        )
    }

    private fun stage1HardCompatibility(observations: List<Observation>): DisqualificationReason? {
        val defects = observations.flatMap { it.metrics.compatibilityDefects }.toSet()
        if (CompatibilityDefect.FAILS_TO_BOOT in defects) return DisqualificationReason.FAILS_TO_BOOT
        if (CompatibilityDefect.ARCHITECTURALLY_INCOMPATIBLE in defects) {
            return DisqualificationReason.ARCHITECTURALLY_INCOMPATIBLE
        }
        if (CompatibilityDefect.UNSUPPORTED_DRIVER in defects) return DisqualificationReason.UNSUPPORTED_DRIVER
        if (CompatibilityDefect.SEVERE_CORRUPTION in defects) return DisqualificationReason.SEVERE_CORRUPTION
        val crashes = observations.sumOf { it.crashCount }
        if (crashes >= config.crashDisqualifyThreshold) return DisqualificationReason.CRASHES_MATERIALLY
        return null
    }

    private fun stage2Target(metrics: BenchmarkMetrics, goal: OptimizationGoal): DisqualificationReason? {
        val target = config.goalWeights[goal]?.targetFps ?: return null
        val sustained = metrics.sustainedFps ?: metrics.averageFps ?: return null
        return if (sustained < target) DisqualificationReason.FAILS_OPTIMIZATION_TARGET else null
    }

    private fun scoreFor(
        metrics: BenchmarkMetrics,
        observations: List<Observation>,
        goal: OptimizationGoal,
    ): Double {
        val weights = config.goalWeights[goal] ?: config.goalWeights.getValue(OptimizationGoal.BALANCED)
        val crashCount = observations.sumOf { it.crashCount }

        val performance = performanceScore(metrics)
        val stability = stabilityScore(metrics, crashCount)
        val sustained = sustainedScore(metrics)
        val quality = qualityScore(metrics)
        val power = powerScore(metrics)

        return performance * weights.performance +
            stability * weights.stability +
            sustained * weights.sustained +
            quality * weights.quality +
            power * weights.power
    }

    private fun performanceScore(metrics: BenchmarkMetrics): Double {
        val avg = metrics.averageFps ?: return 0.5
        return (avg / config.performanceReferenceFps).coerceIn(0.0, 1.0)
    }

    private fun stabilityScore(metrics: BenchmarkMetrics, crashCount: Int): Double {
        // Frame pacing (1% low vs average) is the dominant stability signal; the
        // remaining terms refine it. Missing metrics default to a neutral 0.5 so an
        // unavailable measurement neither helps nor hurts.
        val pacing = ratio(metrics.onePercentLowFps, metrics.averageFps)
        val stutter = metrics.stutterRate?.let { (1.0 - it).coerceIn(0.0, 1.0) } ?: 0.5
        val variance = metrics.frameTimeVariance?.let { kotlin.math.exp(-it / 50.0) } ?: 0.5
        val crash = 1.0 / (1.0 + crashCount)
        return 0.5 * pacing + 0.15 * stutter + 0.15 * variance + 0.2 * crash
    }

    private fun sustainedScore(metrics: BenchmarkMetrics): Double {
        val thermal = metrics.thermalDegradation?.let { (1.0 - it).coerceIn(0.0, 1.0) } ?: 0.5
        val ratio = ratio(metrics.sustainedFps, metrics.averageFps)
        return (thermal + ratio) / 2.0
    }

    private fun qualityScore(metrics: BenchmarkMetrics): Double {
        val visual = when (metrics.visualQuality) {
            VisualQuality.NATIVE -> 1.0
            VisualQuality.SUPERSAMPLED -> 1.0
            VisualQuality.UPSCALED -> 0.8
            VisualQuality.NATIVE_HALF -> 0.6
            VisualQuality.NATIVE_QUARTER -> 0.4
            VisualQuality.UNKNOWN, null -> 0.5
        }
        val correctness = when (metrics.renderingCorrectness) {
            RenderingCorrectness.CORRECT -> 1.0
            RenderingCorrectness.MINOR_DEFECTS -> 0.7
            RenderingCorrectness.MAJOR_DEFECTS -> 0.3
            RenderingCorrectness.UNKNOWN, null -> 0.5
        }
        val defectCount = metrics.compatibilityDefects.size
        val defects = (1.0 - 0.15 * defectCount).coerceIn(0.0, 1.0)
        return (visual + correctness + defects) / 3.0
    }

    private fun powerScore(metrics: BenchmarkMetrics): Double {
        val watts = metrics.powerWatts ?: return 0.5
        return 1.0 / (1.0 + watts / 10.0)
    }

    private fun ratio(numerator: Double?, denominator: Double?): Double {
        if (numerator == null || denominator == null || denominator == 0.0) return 0.5
        return (numerator / denominator).coerceIn(0.0, 1.0)
    }

    private fun buildReasons(
        best: RouteEvaluation,
        evaluations: List<RouteEvaluation>,
        request: RecommendationRequest,
    ): List<RecommendationReason> {
        val reasons = mutableListOf<RecommendationReason>()
        val current = request.currentRouteId?.let { id ->
            evaluations.firstOrNull { it.executionRouteId == id }
        }
        if (current == null) {
            if (best.confidence.level >= ConfidenceLevel.HIGH) reasons.add(RecommendationReason.STRONGER_EVIDENCE)
            return reasons
        }
        if (best.score.value > current.score.value) {
            if (best.metrics.averageFps != null && current.metrics.averageFps != null &&
                best.metrics.averageFps > current.metrics.averageFps
            ) {
                reasons.add(RecommendationReason.PERFORMANCE_GAIN)
            }
            if (best.metrics.onePercentLowFps != null && current.metrics.onePercentLowFps != null &&
                best.metrics.onePercentLowFps > current.metrics.onePercentLowFps
            ) {
                reasons.add(RecommendationReason.BETTER_FRAME_PACING)
            }
            if (best.metrics.thermalDegradation != null && current.metrics.thermalDegradation != null &&
                best.metrics.thermalDegradation < current.metrics.thermalDegradation
            ) {
                reasons.add(RecommendationReason.LOWER_THERMALS)
            }
        }
        if (best.confidence.numeric > current.confidence.numeric) {
            reasons.add(RecommendationReason.STRONGER_EVIDENCE)
        }
        return reasons.distinct()
    }

    private fun buildComparison(
        best: RouteEvaluation,
        evaluations: List<RouteEvaluation>,
        request: RecommendationRequest,
    ): RouteComparison? {
        val currentId = request.currentRouteId ?: return null
        val current = evaluations.firstOrNull { it.executionRouteId == currentId } ?: return null
        val recommendedAvg = best.metrics.averageFps
        val currentAvg = current.metrics.averageFps
        val percentChange = if (recommendedAvg != null && currentAvg != null && currentAvg != 0.0) {
            (recommendedAvg - currentAvg) / currentAvg * 100.0
        } else {
            null
        }
        return RouteComparison(
            currentRouteId = currentId,
            recommendedRouteId = best.executionRouteId,
            currentAverageFps = currentAvg,
            recommendedAverageFps = recommendedAvg,
            percentChange = percentChange,
        )
    }
}
