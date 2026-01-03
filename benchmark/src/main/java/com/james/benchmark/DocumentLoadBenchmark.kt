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
 * Benchmark for document loading and PDF rendering performance.
 * Measures frame times during document viewing and caching scenarios.
 */
@RunWith(AndroidJUnit4::class)
class DocumentLoadBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    companion object {
        private const val PACKAGE_NAME = "com.bdbshs.crest"
        private const val ITERATIONS = 5
        private const val TIMEOUT_MS = 15_000L
    }

    @Test
    fun documentListLoad() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()

        // Wait for app to be fully loaded
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)
        
        // Navigate to documents (if not already there)
        val documentsTab = device.findObject(By.textContains("Documents"))
        documentsTab?.click()
        device.waitForIdle()
        
        // Wait for document list to render
        device.wait(
            Until.hasObject(By.textContains("Search")),
            TIMEOUT_MS
        )
    }

    @Test
    fun documentListScroll() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)

        // Find scrollable list and perform scroll operations
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.let { list ->
            // Scroll down through document list
            repeat(3) {
                list.scroll(Direction.DOWN, 0.6f)
                device.waitForIdle()
            }
            
            // Scroll back up
            repeat(3) {
                list.scroll(Direction.UP, 0.6f)
                device.waitForIdle()
            }
        }
    }

    @Test
    fun documentFilterAndSort() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)

        // Open filter/sort sheet
        val filterButton = device.findObject(By.descContains("Filter"))
        filterButton?.click()
        device.waitForIdle()
        
        // Wait for sort options to appear
        device.wait(Until.hasObject(By.textContains("Sort")), TIMEOUT_MS)
        
        // Select a sort option if available
        val sortOption = device.findObject(By.textContains("Name"))
        sortOption?.click()
        device.waitForIdle()
    }

    @Test
    fun documentRefresh() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()

        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)

        // Find scrollable content for pull-to-refresh
        val scrollable = device.findObject(By.scrollable(true))
        scrollable?.let { list ->
            // Perform pull-to-refresh gesture (swipe down from top)
            list.scroll(Direction.DOWN, 1.5f)
            device.waitForIdle()
            
            // Wait for refresh to complete
            Thread.sleep(2000)
        }
    }
}
