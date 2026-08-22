package com.emutune.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Captures the startup + core-navigation Baseline Profile so the cold-start path is
 * ahead-of-time compiled. Run with:
 *
 *   ./gradlew :benchmark:baselineprofile:generateBaselineProfile
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() = rule.collect(
        packageName = "com.emutune",
        maxIterations = 5,
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.text("Library")), 5_000)
        device.findObject(By.text("Library")).click()
        device.wait(Until.hasObject(By.text("Spider-Man: Web of Shadows")), 5_000)
        device.findObject(By.text("Spider-Man: Web of Shadows")).click()
        device.waitForIdle()
    }
}
