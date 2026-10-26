package com.bdbshs.crest.data.repository

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialUnknownException
import com.bdbshs.crest.notifications.PushNotifications
import com.google.firebase.auth.FirebaseAuth
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

class AuthSignOutTest {
    private val context = mockk<Context>()
    private val auth = mockk<FirebaseAuth>()
    private val push = mockk<PushNotifications>()
    private val credentials = mockk<CredentialManager>()

    @Before fun setup() {
        mockkObject(CredentialManager.Companion)
        every { CredentialManager.create(context) } returns credentials
        coEvery { push.disable() } just Runs
        coEvery { credentials.clearCredentialState(any()) } just Runs
        every { auth.signOut() } just Runs
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
    }

    @After fun cleanup() {
        unmockkObject(CredentialManager.Companion)
        unmockkStatic(Log::class)
    }

    @Test fun `sign-out clears the previous Google session before Firebase publishes logout`() = runTest {
        AuthRepository(auth, push, context).signOut()
        coVerifyOrder {
            push.disable()
            credentials.clearCredentialState(any())
            auth.signOut()
        }
    }

    @Test fun `credential provider cleanup failure still signs out of Firebase`() = runTest {
        coEvery { credentials.clearCredentialState(any()) } throws ClearCredentialUnknownException("Unavailable")
        AuthRepository(auth, push, context).signOut()
        verify(exactly = 1) { auth.signOut() }
    }
}
