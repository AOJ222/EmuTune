package com.emutune.macrobenchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Measures the key journeys called out in the product spec: cold launch, and the
 * library → game-detail navigation. Run with:
 *
 *   ./gradlew :benchmark:macrobenchmark:connectedBenchmarkAndroidTest
 *
 * Each test runs against the installed :app on a connected device or GMD.
 */
@RunWith(AndroidJUnit4::class)
class EmuTuneBenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldLaunch() = benchmarkRule.measureRepeated(
        packageName = "com.emutune",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = CompilationMode.None(),
        startupMode = StartupMode.COLD,
        iterations = 5,
    ) {
        pressHome()
        startActivityAndWait()
    }

    @Test
    fun gameDetailNavigation() = benchmarkRule.measureRepeated(
        packageName = "com.emutune",
        metrics = listOf(FrameTimingMetric()),
        compilationMode = CompilationMode.None(),
        iterations = 3,
    ) {
        pressHome()
        startActivityAndWait()

        // Open Library, then the demo game.
        device.wait(Until.hasObject(By.text("Library")), 5_000)
        device.findObject(By.text("Library")).click()
        device.wait(Until.hasObject(By.text("Spider-Man: Web of Shadows")), 5_000)
        device.findObject(By.text("Spider-Man: Web of Shadows")).click()
        device.waitForIdle()
    }
}
