package com.emutune.model.recommendation

import com.emutune.model.device.HardwareMatchQuality
import com.emutune.model.evidence.EvidenceGrade
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * The shared weighting of evidence grade and hardware match. Used by both the
 * confidence model and the recommendation engine's aggregation so the two can never
 * disagree on how much a Grade A exact-device measurement is worth.
 */
object EvidenceWeights {

    fun gradeWeight(grade: EvidenceGrade): Double = when (grade) {
        EvidenceGrade.A_DETERMINISTIC_BENCHMARK -> 1.0
        EvidenceGrade.B_MACHINE_GAMEPLAY -> 0.85
        EvidenceGrade.C_PARTIAL_MEASUREMENT -> 0.65
        EvidenceGrade.D_USER_SUBMITTED -> 0.4
        EvidenceGrade.E_EXTERNAL_RESEARCH -> 0.2
    }

    fun matchWeight(quality: HardwareMatchQuality): Double = when (quality) {
        HardwareMatchQuality.EXACT_DEVICE -> 1.0
        HardwareMatchQuality.EXACT_SOC_GPU -> 0.85
        HardwareMatchQuality.RELATED_HARDWARE -> 0.5
        HardwareMatchQuality.UNKNOWN_HARDWARE -> 0.3
    }
}

/** A single piece of evidence, reduced to the fields the confidence model needs. */
data class EvidenceSample(
    val grade: EvidenceGrade,
    val hardwareMatchQuality: HardwareMatchQuality,
    val recencyFactor: Double,
    val buildStalenessFactor: Double,
    val success: Boolean,
    val primaryMetric: Double? = null,
)

data class ConfidenceConfig(
    val veryHighThreshold: Double = 0.85,
    val highThreshold: Double = 0.7,
    val mediumThreshold: Double = 0.45,
    val minQuantityShare: Double = 0.5,
    val contradictionThreshold: Double = 0.35,
    val contradictionPenalty: Double = 0.6,
    val failurePenalty: Double = 0.8,
)

/**
 * Determines how strongly the evidence supports a recommendation for a specific
 * environment. Deterministic, with no machine-learning in the path.
 *
 * Confidence combines: the strongest available evidence (grade + hardware match +
 * recency + build currency), a diminishing-returns quantity boost, a penalty for
 * contradictory measurements, and a penalty for failed runs.
 */
class ConfidenceModel(private val config: ConfidenceConfig = ConfidenceConfig()) {

    fun compute(samples: List<EvidenceSample>): EvidenceConfidence {
        if (samples.isEmpty()) return EvidenceConfidence(ConfidenceLevel.LOW, 0.0)

        val signal = samples.maxOf { sample ->
            EvidenceWeights.gradeWeight(sample.grade) *
                EvidenceWeights.matchWeight(sample.hardwareMatchQuality) *
                sample.recencyFactor.coerceIn(0.0, 1.0) *
                sample.buildStalenessFactor.coerceIn(0.0, 1.0)
        }

        val quantity = 1.0 - 0.5.pow(samples.size.toDouble())
        val boosted = signal * (config.minQuantityShare + (1.0 - config.minQuantityShare) * quantity)

        val contradiction = coefficientOfVariation(samples.mapNotNull { it.primaryMetric })
        val contradictionFactor = if (contradiction > config.contradictionThreshold) {
            config.contradictionPenalty
        } else {
            1.0
        }

        val failureFactor = if (samples.any { !it.success }) config.failurePenalty else 1.0

        val numeric = (boosted * contradictionFactor * failureFactor).coerceIn(0.0, 1.0)
        return EvidenceConfidence(levelFor(numeric), numeric)
    }

    fun levelFor(numeric: Double): ConfidenceLevel = when {
        numeric >= config.veryHighThreshold -> ConfidenceLevel.VERY_HIGH
        numeric >= config.highThreshold -> ConfidenceLevel.HIGH
        numeric >= config.mediumThreshold -> ConfidenceLevel.MEDIUM
        else -> ConfidenceLevel.LOW
    }

    private fun coefficientOfVariation(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val mean = values.average()
        if (mean == 0.0) return 0.0
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance) / mean
    }
}
