package com.bdbshs.crest.ui.viewmodels

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.UserPrefs
import com.bdbshs.crest.data.UserPrefs.clear
import com.bdbshs.crest.data.UserPrefs.saveUserData
import com.bdbshs.crest.data.UserPrefs.saveTheme
import com.bdbshs.crest.data.UserPrefs.userDataFlow
import com.bdbshs.crest.data.ThemeMode
import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.UserRepository
import com.bdbshs.crest.data.repository.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.FirebaseAuth
import javax.inject.Inject


data class MainUiState(
    val currentUid: String? = null,
    val userRole: UserType? = null,
    val userName: String? = null,
    val userEmail: String? = null,
    val userPhotoUrl: String? = null,
    val isAllowedOffline: Boolean = false, // True if user is accepted/has access, based on latest known data
    val isLoading: Boolean = true, // Represents the *initial* loading state of the app
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val error: String? = null
)

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var userDocListener: ListenerRegistration? = null

    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val firebaseUser = firebaseAuth.currentUser
        if (firebaseUser != null) {
            Log.d(TAG, "Auth state change: User logged in (${firebaseUser.uid}). Refreshing data...")
            loadAndSyncUserData(firebaseUser)
        } else {
            Log.d(TAG, "Auth state change: No user logged in. Clearing listener and state.")
            userDocListener?.remove()
            userDocListener = null
            _uiState.update { MainUiState(isLoading = false, themeMode = it.themeMode) }
        }
    }

    init {
        // Observe auth state changes to refresh user data automatically.
        // This ensures that when a user logs out and a new one logs in,
        // the ViewModel refreshes its state even if it stays in memory.
        AuthRepository.addAuthStateListener(authStateListener)
    }

    /**
     * Loads user data from local cache and starts background sync with Firestore.
     */
    private fun loadAndSyncUserData(firebaseUser: FirebaseUser) {
        viewModelScope.launch {
            // --- Step 1: Immediately load from local cache (UserPrefs) ---
            val cachedUserData = ctx.userDataFlow.first()
            
            // Only use cache if it belongs to the current user
            val useCache = cachedUserData.uid == firebaseUser.uid
            
            val cachedRole = if (useCache && !cachedUserData.role.isNullOrBlank()) {
                try {
                    UserType.valueOf(cachedUserData.role)
                } catch (e: IllegalArgumentException) {
                    null
                }
            } else {
                null
            }

            // Update UI with initial data (from cache or basic auth profile)
            _uiState.update { currentState ->
                currentState.copy(
                    currentUid = firebaseUser.uid,
                    userRole = cachedRole,
                    userName = firebaseUser.displayName,
                    userEmail = firebaseUser.email,
                    userPhotoUrl = firebaseUser.photoUrl?.toString(),
                    isAllowedOffline = if (useCache) {
                        when (cachedRole) {
                            UserType.STUDENT -> cachedUserData.accepted
                            UserType.TEACHER -> cachedUserData.access
                            else -> false
                        }
                    } else false,
                    themeMode = if (useCache) cachedUserData.theme else currentState.themeMode,
                    isLoading = cachedRole == null // Still loading if no cached role
                )
            }

            // --- Step 2: Refresh from Firestore ---
            if (isOnline()) {
                refreshFromFirestore(firebaseUser.uid)
            } else {
                Log.d(TAG, "Offline: Relying on cached data for ${firebaseUser.uid}")
                if (cachedRole == null) {
                    _uiState.update { it.copy(isLoading = false, error = "You are offline.") }
                }
            }
        }
    }

    /**
     * Refreshes user data from Firestore and sets up a real-time listener.
     * This function is treated as a background sync and does NOT set `isLoading = true`
     * to avoid interrupting the user with a loading screen after the initial load.
     */
    private fun refreshFromFirestore(uid: String) {
        userDocListener?.remove() // Ensure no old listeners are active

        viewModelScope.launch {
            try {
                // Determine user role and document path from Firestore
                val roleLocation = UserRepository.detectUserRoleLocation(uid)
                    ?: throw IllegalStateException("User document not found for UID: $uid in Firestore collections. Profile incomplete.")

                // Set up a real-time listener. This will provide live updates when online
                // and cached data when offline (if previously synced).
                userDocListener = UserRepository.observeUserStatus(uid, roleLocation) { hasPermission, error ->
                        if (error != null) {
                            Log.e(TAG, "Firestore listener error for $uid: ${error.message}", error)
                            val errorMessage = if (error.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                                "You are offline. Displaying cached user status."
                            } else {
                                "Error fetching user status: ${error.message}"
                            }
                            _uiState.update { it.copy(error = errorMessage) } // Show non-blocking error
                            return@observeUserStatus
                        }

                        val isAllowed = hasPermission ?: false
                        val role = roleLocation.role.toUiUserType()

                        Log.d(TAG, "Live status update for $uid: Role=$role, Permission=$hasPermission")

                        // Update the UI state with the fresh data
                        _uiState.update { currentState ->
                            currentState.copy(
                                userRole = role,
                                isAllowedOffline = isAllowed,
                                isLoading = false,
                                error = null // Clear any previous error on successful update
                            )
                        }

                        // Persist the latest status to UserPrefs to keep the cache fresh for next launch
                        viewModelScope.launch {
                            ctx.saveUserData(
                                UserPrefs.UserData(
                                    uid = uid,
                                    role = role.name,
                                    accepted = role == UserType.STUDENT && isAllowed,
                                    access = role == UserType.TEACHER && isAllowed,
                                    theme = _uiState.value.themeMode
                                )
                            )
                            Log.d(TAG, "Updated UserPrefs cache with fresh data from Firestore.")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate Firestore refresh for $uid: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Could not verify user status. Please check your connection."
                    )
                }
            }
        }
    }

    @SuppressLint("ServiceCast")
    private fun isOnline(): Boolean {
        val connectivityManager = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false // No active network
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false // No network capabilities
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /**
     * Handles user sign-out process.
     * Signs out from Firebase, clears local user preferences, and updates UI state.
     */
    fun onSignOut() {
        viewModelScope.launch {
            try {
                // 1. Remove the active Firestore listener
                userDocListener?.remove()
                userDocListener = null // Clear reference

                // 2. Sign out from Firebase Authentication
                AuthRepository.signOut()
                Log.d(TAG, "Firebase user signed out.")

                // 3. Clear local persisted user data in DataStore
                ctx.clear() // This will now clear all user-specific data from DataStore
                Log.d(TAG, "Local user data cleared from preferences.")

                // 4. Update the UI state to reflect the logged-out status
                _uiState.update {
                    it.copy(
                        currentUid = null,
                        userRole = null,
                        userName = null,
                        userEmail = null,
                        userPhotoUrl = null,
                        isAllowedOffline = false,
                        isLoading = false, // Stop any ongoing loading
                        error = null // Clear any previous errors
                    )
                }
                Log.d(TAG, "UI state updated to logged out.")

            } catch (e: Exception) {
                // Log and update UI state with error if sign-out fails
                Log.e(TAG, "Error during sign out: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to sign out: ${e.localizedMessage ?: e.message}"
                    )
                }
            }
        }
    }

    /**
     * Updates the user's theme preference.
     */
    fun onThemeChanged(theme: ThemeMode) {
        viewModelScope.launch {
            ctx.saveTheme(theme)
            _uiState.update { it.copy(themeMode = theme) }
            Log.d(TAG, "Theme updated to $theme")
        }
    }

    override fun onCleared() {
        AuthRepository.removeAuthStateListener(authStateListener)
        userDocListener?.remove() // Ensure the listener is removed to prevent memory leaks
        super.onCleared()
        Log.d(TAG, "MainViewModel cleared. Firestore and Auth listeners removed.")
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

private fun UserRole.toUiUserType(): UserType {
    return when (this) {
        UserRole.STUDENT -> UserType.STUDENT
        UserRole.TEACHER -> UserType.TEACHER
    }
}