package com.bdbshs.crest.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import com.bdbshs.crest.navigation.SessionNavigationHost
import com.bdbshs.crest.ui.theme.CRESTTheme
import kotlinx.coroutines.channels.Channel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SessionNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun switchingAccountsKeepsTheLoginCollectorUntilItsFirstResult() {
        val loading = mutableStateOf(false)
        val destination = mutableStateOf("login")
        val events = Channel<String>(Channel.BUFFERED)
        var entered = 0
        var disposed = 0
        var completed = 0
        var initialRoute = ""
        compose.setContent {
            CRESTTheme {
                SessionNavigationHost(loading.value, { destination.value }) { start ->
                    initialRoute = start
                    DisposableEffect(Unit) {
                        entered++
                        onDispose { disposed++ }
                    }
                    LaunchedEffect(Unit) {
                        for (event in events) {
                            assertEquals("student", event)
                            completed++
                        }
                    }
                    Text("Login screen")
                }
            }
        }
        compose.runOnIdle { loading.value = true }
        compose.waitForIdle()
        compose.runOnIdle { events.trySend("student") }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, completed)
            assertEquals(1, entered)
            assertEquals(0, disposed)
            destination.value = "student home"
            loading.value = false
        }
        compose.runOnIdle {
            assertEquals("login", initialRoute)
            assertEquals(1, entered)
            assertEquals(0, disposed)
        }
    }

    @Test fun coldStartWaitsForTheInitialAccountLookup() {
        val loading = mutableStateOf(true)
        var entered = 0
        var startRoute = ""
        compose.setContent {
            CRESTTheme {
                SessionNavigationHost(loading.value, { "teacher home" }) { start ->
                    DisposableEffect(Unit) {
                        entered++
                        onDispose { }
                    }
                    startRoute = start
                    Text(start)
                }
            }
        }
        compose.runOnIdle {
            assertEquals(0, entered)
            loading.value = false
        }
        compose.runOnIdle {
            assertEquals(1, entered)
            assertEquals("teacher home", startRoute)
        }
    }
}
