package com.james.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for scrolling performance in document lists.
 * Measures frame jank during list scrolling operations.
 */
@RunWith(AndroidJUnit4::class)
class ScrollingBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    companion object {
        private const val PACKAGE_NAME = "com.bdbshs.crest"
        private const val ITERATIONS = 5
        private const val TIMEOUT_MS = 10_000L
    }

    @Test
    fun scrollDocumentList() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()

        // Wait for the app to be fully loaded
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)
        
        // Find scrollable content and perform fling gestures
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.let {
            // Scroll down
            it.fling(Direction.DOWN)
            device.waitForIdle()
            
            // Scroll up
            it.fling(Direction.UP)
            device.waitForIdle()
            
            // Multiple smaller scrolls to measure list performance
            repeat(3) {
                scrollable.scroll(Direction.DOWN, 0.5f)
                device.waitForIdle()
            }
        }
    }

    @Test
    fun scrollListWithFrameTiming() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()
        
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)
        
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.let {
            // Perform continuous scrolling to measure frame drops
            repeat(5) {
                scrollable.scroll(Direction.DOWN, 0.8f)
            }
            repeat(5) {
                scrollable.scroll(Direction.UP, 0.8f)
            }
        }
    }
}
