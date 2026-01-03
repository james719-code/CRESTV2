package com.james.benchmark

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
 * Benchmark for authentication flow performance.
 * Measures time to show login screen and UI responsiveness.
 */
@RunWith(AndroidJUnit4::class)
class AuthenticationBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    companion object {
        private const val PACKAGE_NAME = "com.bdbshs.crest"
        private const val ITERATIONS = 5
        private const val TIMEOUT_MS = 10_000L
    }

    @Test
    fun timeToLoginScreen() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(
            StartupTimingMetric(),
            FrameTimingMetric()
        ),
        iterations = ITERATIONS,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()
        
        // Wait for login UI to be fully rendered
        device.wait(
            Until.hasObject(By.textContains("Sign in")),
            TIMEOUT_MS
        )
    }

    @Test
    fun loginScreenFramePerformance() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()
        
        // Measure frame timing while login screen is displayed
        device.wait(
            Until.hasObject(By.textContains("Sign in")),
            TIMEOUT_MS
        )
        
        // Simulate some UI interaction
        Thread.sleep(1000)
    }
}
