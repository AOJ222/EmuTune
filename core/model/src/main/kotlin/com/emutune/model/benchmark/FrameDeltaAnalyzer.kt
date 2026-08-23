package com.emutune.model.benchmark

import com.emutune.model.evidence.BenchmarkMetrics

/**
 * Derives frame-rate metrics from a stream of screen-capture frame samples. Pure and
 * deterministic: given when the screen changed and at what timestamp, it computes
 * average FPS, 1% low, frame-time variance and stutter rate.
 *
 * This is the honest core of screen-capture measurement — it observes the *screen
 * update rate*, not the emulator's internal render rate. That distinction is recorded
 * as a limitation by the provider that uses this analyzer, never hidden.
 */
object FrameDeltaAnalyzer {

    fun analyze(samples: List<FrameSample>): BenchmarkMetrics {
        if (samples.size < 2) return BenchmarkMetrics()

        val changed = samples.filter { it.changed }
        if (changed.size < 2) return BenchmarkMetrics()

        val intervalsNanos = changed.zipWithNext { a, b -> b.timestampNanos - a.timestampNanos }
            .filter { it > 0 }
        if (intervalsNanos.isEmpty()) return BenchmarkMetrics()

        val totalNanos = changed.last().timestampNanos - changed.first().timestampNanos
        val durationSeconds = totalNanos / 1e9
        val averageFps = if (durationSeconds > 0.0) intervalsNanos.size / durationSeconds else null

        val intervalsMs = intervalsNanos.map { it / 1e6 }
        val instantaneousFps = intervalsNanos.map { 1.0 / (it / 1e9) }

        return BenchmarkMetrics(
            averageFps = averageFps,
            onePercentLowFps = percentileLowMean(instantaneousFps, 0.01),
            frameTimeVariance = variance(intervalsMs),
            stutterRate = stutterRate(intervalsMs),
        )
    }

    /** Mean of the slowest [fraction] of frames (at least one). */
    private fun percentileLowMean(values: List<Double>, fraction: Double): Double {
        val sorted = values.sorted()
        val count = (sorted.size * fraction).toInt().coerceAtLeast(1)
        return sorted.take(count).average()
    }

    private fun variance(values: List<Double>): Double? {
        if (values.size < 2) return null
        val mean = values.average()
        return values.map { (it - mean) * (it - mean) }.average()
    }

    /** Fraction of frame intervals that are notably longer than the median (a hitch). */
    private fun stutterRate(intervalsMs: List<Double>): Double? {
        if (intervalsMs.size < 3) return null
        val sorted = intervalsMs.sorted()
        val median = sorted[sorted.size / 2]
        if (median <= 0.0) return null
        return intervalsMs.count { it > median * 1.5 } / intervalsMs.size.toDouble()
    }
}
