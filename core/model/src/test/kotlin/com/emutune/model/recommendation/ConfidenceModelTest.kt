package com.emutune.model.recommendation

import com.emutune.model.device.HardwareMatchQuality
import com.emutune.model.evidence.EvidenceGrade
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ConfidenceModelTest {

    private val model = ConfidenceModel()

    @Test
    fun `one unverified report produces low confidence`() {
        val confidence = model.compute(
            listOf(
                EvidenceSample(
                    grade = EvidenceGrade.E_EXTERNAL_RESEARCH,
                    hardwareMatchQuality = HardwareMatchQuality.UNKNOWN_HARDWARE,
                    recencyFactor = 1.0,
                    buildStalenessFactor = 1.0,
                    success = true,
                    primaryMetric = 30.0,
                ),
            ),
        )
        assertEquals(ConfidenceLevel.LOW, confidence.level)
    }

    @Test
    fun `repeated exact hardware machine measurements raise confidence`() {
        val sample = EvidenceSample(
            grade = EvidenceGrade.A_DETERMINISTIC_BENCHMARK,
            hardwareMatchQuality = HardwareMatchQuality.EXACT_DEVICE,
            recencyFactor = 1.0,
            buildStalenessFactor = 1.0,
            success = true,
            primaryMetric = 48.0,
        )
        val one = model.compute(listOf(sample))
        val three = model.compute(listOf(sample, sample, sample))

        assertTrue(three.numeric > one.numeric)
    }

    @Test
    fun `contradictory evidence lowers confidence`() {
        val consistent = model.compute(
            listOf(
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 1.0, true, 48.0,
                ),
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 1.0, true, 50.0,
                ),
            ),
        )
        val contradictory = model.compute(
            listOf(
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 1.0, true, 20.0,
                ),
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 1.0, true, 60.0,
                ),
            ),
        )

        assertTrue(contradictory.numeric < consistent.numeric)
    }

    @Test
    fun `hardware mismatch lowers confidence`() {
        val exact = model.compute(
            listOf(
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 1.0, true, 48.0,
                ),
            ),
        )
        val unknown = model.compute(
            listOf(
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.UNKNOWN_HARDWARE, 1.0, 1.0, true, 48.0,
                ),
            ),
        )

        assertTrue(unknown.numeric < exact.numeric)
    }

    @Test
    fun `build mismatch lowers confidence`() {
        val current = model.compute(
            listOf(
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 1.0, true, 48.0,
                ),
            ),
        )
        val stale = model.compute(
            listOf(
                EvidenceSample(
                    EvidenceGrade.A_DETERMINISTIC_BENCHMARK, HardwareMatchQuality.EXACT_DEVICE, 1.0, 0.6, true, 48.0,
                ),
            ),
        )

        assertTrue(stale.numeric < current.numeric)
    }
}
