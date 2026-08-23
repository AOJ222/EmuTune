package com.emutune.model.benchmark

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FrameDeltaAnalyzerTest {

    private fun samplesAtHz(fps: Int, seconds: Int, captureHz: Int = 120): List<FrameSample> {
        val intervalNanos = (1_000_000_000L / captureHz)
        val count = seconds * captureHz + 1
        val stride = captureHz / fps
        return (0 until count).map { i ->
            FrameSample(
                timestampNanos = i * intervalNanos,
                changed = i % stride == 0,
            )
        }
    }

    @Test
    fun `constant 60 fps is measured as 60`() {
        val metrics = FrameDeltaAnalyzer.analyze(samplesAtHz(fps = 60, seconds = 2))
        val avg = requireNotNull(metrics.averageFps)
        assertTrue(avg in 59.0..61.0, "expected ~60, got $avg")
        assertTrue((metrics.onePercentLowFps ?: 0.0) in 59.0..61.0)
        assertTrue((metrics.stutterRate ?: 1.0) < 0.05)
    }

    @Test
    fun `constant 30 fps on 60hz capture is measured as 30`() {
        val metrics = FrameDeltaAnalyzer.analyze(samplesAtHz(fps = 30, seconds = 2))
        val avg = requireNotNull(metrics.averageFps)
        assertTrue(avg in 29.0..31.0, "expected ~30, got $avg")
    }

    @Test
    fun `a single hitch lowers the 1 percent low and raises stutter`() {
        val smooth = samplesAtHz(fps = 60, seconds = 2)
        // Insert a 100ms stall by shifting every timestamp after the midpoint.
        val midpoint = smooth.size / 2
        val stalled = smooth.mapIndexed { i, sample ->
            if (i > midpoint) sample.copy(timestampNanos = sample.timestampNanos + 100_000_000L) else sample
        }
        val metrics = FrameDeltaAnalyzer.analyze(stalled)

        val oneLow = requireNotNull(metrics.onePercentLowFps)
        val avg = requireNotNull(metrics.averageFps)
        assertTrue(oneLow < avg, "1% low $oneLow should be below average $avg after a hitch")
        assertTrue((metrics.stutterRate ?: 0.0) > 0.0)
    }

    @Test
    fun `empty input yields empty metrics`() {
        val metrics = FrameDeltaAnalyzer.analyze(emptyList())
        assertNull(metrics.averageFps)
        assertNull(metrics.onePercentLowFps)
    }

    @Test
    fun `no changed frames yields empty metrics`() {
        val noChange = (0 until 10).map { FrameSample(timestampNanos = it * 16_666_667L, changed = false) }
        val metrics = FrameDeltaAnalyzer.analyze(noChange)
        assertNull(metrics.averageFps)
    }

    @Test
    fun `one changed frame yields empty metrics`() {
        val one = listOf(
            FrameSample(timestampNanos = 0, changed = true),
            FrameSample(timestampNanos = 16_666_667L, changed = false),
        )
        assertNull(FrameDeltaAnalyzer.analyze(one).averageFps)
    }

    @Test
    fun `average fps matches the number of distinct frames per second`() {
        // 40 distinct frames over exactly 1 second at a 120Hz capture.
        val intervalNanos = 1_000_000_000L / 120
        val samples = (0..120).map { i ->
            FrameSample(timestampNanos = i * intervalNanos, changed = i % 3 == 0)
        }
        val avg = requireNotNull(FrameDeltaAnalyzer.analyze(samples).averageFps)
        assertEquals(40.0, avg, 1.0)
    }
}
