package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.UserPrefs
import com.bdbshs.crest.data.UserPrefs.clear
import com.bdbshs.crest.data.UserPrefs.saveUserData
import com.bdbshs.crest.data.UserPrefs.userDataFlow
import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.UserRepository
import com.bdbshs.crest.data.repository.UserRole
import com.bdbshs.crest.data.repository.UserRoleLocation
import com.bdbshs.crest.data.repository.UserStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SessionSwitchingTest {
    private val dispatcher = StandardTestDispatcher()
    private val context = mockk<Context>()
    private val authRepository = mockk<AuthRepository>()
    private val users = mockk<UserRepository>()
    private val firebaseAuth = mockk<FirebaseAuth>()
    private val authListener = slot<FirebaseAuth.AuthStateListener>()
    private val callbacks = mutableMapOf<String, (Boolean?, FirebaseFirestoreException?) -> Unit>()
    private val stores = mutableListOf<ViewModelStore>()
    private var currentUser: FirebaseUser? = null
    private var preferences = UserPrefs.UserData()

    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.e(any(), any(), any()) } returns 0
        mockkObject(UserPrefs)
        every { context.userDataFlow } answers { flowOf(preferences) }
        coEvery { context.saveUserData(any()) } coAnswers { preferences = secondArg() }
        coEvery { context.clear() } coAnswers { preferences = UserPrefs.UserData() }
        val network = mockk<Network>()
        val capabilities = mockk<NetworkCapabilities>()
        every { capabilities.hasCapability(any()) } returns true
        every { capabilities.hasTransport(any()) } returns true
        val connectivity = mockk<ConnectivityManager>()
        every { connectivity.activeNetwork } returns network
        every { connectivity.getNetworkCapabilities(network) } returns capabilities
        every { context.getSystemService(Context.CONNECTIVITY_SERVICE) } returns connectivity
        every { firebaseAuth.currentUser } answers { currentUser }
        every { authRepository.getCurrentUser() } answers { currentUser }
        every { authRepository.getCurrentUserUid() } answers { currentUser?.uid }
        every { authRepository.addAuthStateListener(capture(authListener)) } just Runs
        every { authRepository.removeAuthStateListener(any()) } just Runs
        coEvery { authRepository.signOut() } coAnswers { changeUser(null) }
        every { users.observeUserStatus(any(), any(), any()) } answers {
            callbacks[firstArg()] = thirdArg()
            mockk(relaxed = true)
        }
    }

    @After fun cleanup() {
        stores.forEach { it.clear() }
        unmockkObject(UserPrefs)
        unmockkStatic(Log::class)
        Dispatchers.resetMain()
    }

    private fun <T : ViewModel> keep(model: T): T = model.also {
        stores += ViewModelStore().apply { put("test", model) }
    }

    private fun user(id: String) = mockk<FirebaseUser>(relaxed = true) {
        every { uid } returns id
        every { displayName } returns id
    }

    private fun changeUser(user: FirebaseUser?) {
        currentUser = user
        if (authListener.isCaptured) authListener.captured.onAuthStateChanged(firebaseAuth)
    }

    private fun location(role: UserRole) = UserRoleLocation(
        role,
        if (role == UserRole.STUDENT) FirestorePaths.STUDENTS else FirestorePaths.TEACHERS,
        if (role == UserRole.STUDENT) "accepted" else "access"
    )

    @Test fun `successful login waits for a screen to collect navigation`() = runTest(dispatcher) {
        currentUser = user("student")
        coEvery { users.getUserStatus("student") } returns UserStatus(UserRole.STUDENT, true)
        val model = keep(LoginViewModel(context, authRepository, users))
        advanceUntilIdle()

        // Account loading can temporarily take the login screen out of composition.
        val navigation = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            model.loginResult.first()
        }
        runCurrent()
        assertTrue("A completed login must not require another Google sign-in", navigation.isCompleted)
        assertEquals(LoginResult.NavigateToHome, navigation.await())
        assertEquals("student", preferences.uid)
    }

    @Test fun `late teacher lookup cannot replace the next student session`() = runTest(dispatcher) {
        val oldLookup = CompletableDeferred<UserRoleLocation?>()
        coEvery { users.detectUserRoleLocation("teacher") } coAnswers {
            withContext(NonCancellable) { oldLookup.await() }
        }
        coEvery { users.detectUserRoleLocation("student") } returns location(UserRole.STUDENT)
        val model = keep(MainViewModel(context, authRepository, users))
        changeUser(user("teacher"))
        runCurrent()
        changeUser(null)
        changeUser(user("student"))
        runCurrent()
        callbacks.getValue("student")(true, null)
        runCurrent()
        oldLookup.complete(location(UserRole.TEACHER))
        runCurrent()
        callbacks["teacher"]?.invoke(true, null)
        advanceUntilIdle()

        assertEquals("student", model.uiState.value.currentUid)
        assertEquals(UserType.STUDENT, model.uiState.value.userRole)
        assertEquals("student", preferences.uid)
        assertFalse("An old account must not attach another listener", callbacks.containsKey("teacher"))
    }

    @Test fun `old login verification cannot restore a signed-out teacher`() = runTest(dispatcher) {
        currentUser = user("teacher")
        val oldStatus = CompletableDeferred<UserStatus?>()
        coEvery { users.getUserStatus("teacher") } coAnswers { oldStatus.await() }
        val model = keep(LoginViewModel(context, authRepository, users))
        runCurrent()
        changeUser(user("student"))
        oldStatus.complete(UserStatus(UserRole.TEACHER, true))
        advanceUntilIdle()
        val navigation = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            model.loginResult.first()
        }
        runCurrent()
        assertFalse("An old login must not navigate for another account", navigation.isCompleted)
        assertEquals(UserPrefs.UserData(), preferences)
    }

    @Test fun `removed teacher listener cannot overwrite student permissions`() = runTest(dispatcher) {
        coEvery { users.detectUserRoleLocation("teacher") } returns location(UserRole.TEACHER)
        coEvery { users.detectUserRoleLocation("student") } returns location(UserRole.STUDENT)
        val model = keep(MainViewModel(context, authRepository, users))
        changeUser(user("teacher"))
        runCurrent()
        val oldCallback = callbacks.getValue("teacher")
        oldCallback(true, null)
        runCurrent()
        changeUser(null)
        changeUser(user("student"))
        runCurrent()
        callbacks.getValue("student")(true, null)
        runCurrent()
        oldCallback(false, null)
        advanceUntilIdle()

        assertEquals(UserType.STUDENT, model.uiState.value.userRole)
        assertTrue(model.uiState.value.isAllowedOffline)
        assertEquals("student", preferences.uid)
    }

    @Test fun `logout clears old preferences before exposing the login screen`() = runTest(dispatcher) {
        coEvery { users.detectUserRoleLocation("teacher") } returns location(UserRole.TEACHER)
        val clearFinished = CompletableDeferred<Unit>()
        coEvery { context.clear() } coAnswers {
            clearFinished.await()
            preferences = UserPrefs.UserData()
        }
        val model = keep(MainViewModel(context, authRepository, users))
        changeUser(user("teacher"))
        runCurrent()
        callbacks.getValue("teacher")(true, null)
        runCurrent()

        model.onSignOut()
        runCurrent()
        model.onSignOut()
        runCurrent()
        coVerify(exactly = 1) { context.clear() }
        coVerify(exactly = 0) { authRepository.signOut() }
        assertEquals("teacher", currentUser?.uid)
        clearFinished.complete(Unit)
        advanceUntilIdle()
        coVerify(exactly = 1) { authRepository.signOut() }
        assertNull(model.uiState.value.currentUid)
        assertEquals(UserPrefs.UserData(), preferences)
    }
}
