package com.james.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark for search functionality performance.
 * Measures search input latency and result rendering.
 * 
 * Improved with better null handling and proper timeouts.
 */
@RunWith(AndroidJUnit4::class)
class SearchBenchmark {
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    companion object {
        private const val PACKAGE_NAME = "com.bdbshs.crest"
        private const val ITERATIONS = 5
        private const val TIMEOUT_MS = 10_000L
        private const val SHORT_DELAY_MS = 500L
        private const val RESULT_RENDER_DELAY_MS = 1500L
    }

    @Test
    fun searchInputPerformance() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()

        // Wait for app to be fully loaded
        val appLoaded = device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)
        if (!appLoaded) return@measureRepeated

        val searchField = device.findObject(By.textContains("Search"))
            ?: device.findObject(By.descContains("Search"))
            ?: device.findObject(By.clazz("android.widget.EditText")) // Fallback to finding any EditText

        searchField?.let {
            it.click()
            device.waitForIdle()

            // Wait for keyboard/input focus
            device.wait(Until.hasObject(By.focused(true)), SHORT_DELAY_MS)

            // Type search query character by character to measure input latency
            val focusedField = device.findObject(By.focused(true))
            if (focusedField != null) {
                val searchQuery = "research"
                searchQuery.forEach { char ->
                    focusedField.text = (focusedField.text ?: "") + char.toString()
                    device.waitForIdle()
                }
            }
        }
    }

    @Test
    fun searchResultsRendering() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()
        
        // Wait for app to be fully loaded
        val appLoaded = device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)
        if (!appLoaded) return@measureRepeated
        
        // Find and interact with search
        val searchField = device.findObject(By.textContains("Search"))
            ?: device.findObject(By.descContains("Search"))
        
        searchField?.let {
            it.click()
            Thread.sleep(SHORT_DELAY_MS)
            
            // Wait for focus and type search
            val focusedField = device.findObject(By.focused(true))
            focusedField?.let { field ->
                field.text = "STEM"
                device.waitForIdle()
            }
            
            // Wait for results to render
            Thread.sleep(RESULT_RENDER_DELAY_MS)
        }
    }

    @Test
    fun searchClearAndRetype() = benchmarkRule.measureRepeated(
        packageName = PACKAGE_NAME,
        metrics = listOf(FrameTimingMetric()),
        iterations = ITERATIONS,
        startupMode = StartupMode.HOT,
        compilationMode = CompilationMode.Partial()
    ) {
        pressHome()
        startActivityAndWait()
        
        val appLoaded = device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT_MS)
        if (!appLoaded) return@measureRepeated
        
        val searchField = device.findObject(By.textContains("Search"))
        searchField?.let {
            it.click()
            device.waitForIdle()
            
            val focusedField = device.findObject(By.focused(true))
            focusedField?.let { field ->
                // Type first query
                field.text = "ABM"
                device.waitForIdle()
                Thread.sleep(SHORT_DELAY_MS)
                
                // Clear and type new query
                field.text = ""
                device.waitForIdle()
                field.text = "STEM"
                device.waitForIdle()
                Thread.sleep(RESULT_RENDER_DELAY_MS)
            }
        }
    }
}
