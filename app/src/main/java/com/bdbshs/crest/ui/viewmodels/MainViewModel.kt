package com.bdbshs.crest.ui.viewmodels

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.data.UserPrefs
import com.bdbshs.crest.data.UserPrefs.clear
import com.bdbshs.crest.data.UserPrefs.saveUserData
import com.bdbshs.crest.data.UserPrefs.userDataFlow
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


data class MainUiState(
    val currentUid: String? = null,
    val userRole: UserType? = null,
    val isAllowedOffline: Boolean = false, // True if user is accepted/has access, based on latest known data
    val isLoading: Boolean = true, // Represents the *initial* loading state of the app
    val error: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val auth = FirebaseClient.auth
    private val firestore = FirebaseClient.firestore
    private val ctx = getApplication<Application>()

    private val _uiState = MutableStateFlow(MainUiState(isLoading = true))
    val uiState = _uiState.asStateFlow()

    private var userDocListener: ListenerRegistration? = null

    init {
        // This is the main state initialization logic.
        viewModelScope.launch {
            // --- Step 1: Immediately load from local cache (UserPrefs) ---
            // This provides a fast, synchronous-like startup experience,
            // populating the UI with the last known user data.
            val cachedUserData = ctx.userDataFlow.first()
            Log.d(TAG, "Loaded cached data: UID=${cachedUserData.uid}, Role=${cachedUserData.role}")

            val cachedRole = if (!cachedUserData.role.isNullOrBlank()) {
                try {
                    UserType.valueOf(cachedUserData.role)
                } catch (e: IllegalArgumentException) {
                    Log.e(TAG, "Invalid UserType in cache: '${cachedUserData.role}'.", e)
                    null // Treat invalid role as unknown
                }
            } else {
                null
            }

            _uiState.update { currentState ->
                currentState.copy(
                    currentUid = cachedUserData.uid,
                    userRole = cachedRole,
                    isAllowedOffline = when (cachedRole) {
                        UserType.STUDENT -> cachedUserData.accepted
                        UserType.TEACHER -> cachedUserData.access
                        else -> false
                    },
                    // Crucially, set isLoading to false. The app has enough data to proceed.
                    isLoading = false
                )
            }
            Log.d(TAG, "Initial state set from cache. UI is now ready.")


            // --- Step 2: If online, start a background refresh from Firestore ---
            // This prioritizes fresh data without blocking the UI or causing state flickers.
            val currentFirebaseUser = auth.currentUser
            if (currentFirebaseUser != null && isOnline()) {
                Log.d(TAG, "User is online. Starting background sync with Firestore.")
                refreshFromFirestore(currentFirebaseUser.uid)
            } else if (currentFirebaseUser != null) {
                Log.d(TAG, "User is offline. Relying on cached data and Firestore's offline persistence.")
                // No action needed, listener will be attached if/when connectivity returns.
                // The `isAllowedOffline` flag from cache will correctly control access.
            } else {
                Log.d(TAG, "No user logged in. UI will navigate to Login screen.")
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
                val (role, collectionPath, permissionField) = detectRoleFromFirestore(uid)

                // Set up a real-time listener. This will provide live updates when online
                // and cached data when offline (if previously synced).
                userDocListener = firestore.collection(collectionPath).document(uid)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e(TAG, "Firestore listener error for $uid: ${e.message}", e)
                            val errorMessage = if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                                "You are offline. Displaying cached user status."
                            } else {
                                "Error fetching user status: ${e.message}"
                            }
                            _uiState.update { it.copy(error = errorMessage) } // Show non-blocking error
                            return@addSnapshotListener
                        }

                        val hasPermission = snapshot?.getBoolean(permissionField) ?: false
                        Log.d(TAG, "Live status update for $uid: Role=$role, Permission=$hasPermission")

                        // Update the UI state with the fresh data
                        _uiState.update { currentState ->
                            currentState.copy(
                                userRole = role,
                                isAllowedOffline = hasPermission,
                                error = null // Clear any previous error on successful update
                            )
                        }

                        // Persist the latest status to UserPrefs to keep the cache fresh for next launch
                        viewModelScope.launch {
                            ctx.saveUserData(
                                UserPrefs.UserData(
                                    uid = uid,
                                    role = role.name,
                                    accepted = role == UserType.STUDENT && hasPermission,
                                    access = role == UserType.TEACHER && hasPermission
                                )
                            )
                            Log.d(TAG, "Updated UserPrefs cache with fresh data from Firestore.")
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initiate Firestore refresh for $uid: ${e.message}", e)
                _uiState.update {
                    it.copy(error = "Could not verify user status. Please check your connection.")
                }
            }
        }
    }


    /**
     * Determines the user's role (Student or Teacher) by checking Firestore documents.
     * Uses `get().await()` which by default attempts to use the server first, then the cache.
     * If offline, it will only succeed if the document is in the cache.
     *
     * @param uid The Firebase user ID.
     * @return A Triple containing UserType, collection path, and permission field name.
     * @throws IllegalStateException if the user is authenticated but no corresponding document is found.
     */
    private suspend fun detectRoleFromFirestore(uid: String): Triple<UserType, String, String> {
        val studentsCollection = firestore.collection("users/user_details/students")
        val teachersCollection = firestore.collection("users/user_details/teachers")

        // Try to get student document first
        val studentDoc = try {
            studentsCollection.document(uid).get().await()
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "Failed to get student doc (online or from cache) for $uid: ${e.message}")
            null
        }

        if (studentDoc?.exists() == true) {
            Log.d(TAG, "User $uid identified as STUDENT via Firestore.")
            return Triple(UserType.STUDENT, "users/user_details/students", "accepted")
        }

        // If not a student, try to get teacher document
        val teacherDoc = try {
            teachersCollection.document(uid).get().await()
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "Failed to get teacher doc (online or from cache) for $uid: ${e.message}")
            null
        }

        if (teacherDoc?.exists() == true) {
            Log.d(TAG, "User $uid identified as TEACHER via Firestore.")
            return Triple(UserType.TEACHER, "users/user_details/teachers", "access")
        }

        // If user document not found in either collection, it's an unexpected state for an already signed-in user.
        // This scenario typically means the user needs to complete their signup details.
        // The NavHost's initial routing logic (e.g., from LoginViewModel) should handle navigation to SignUpDetails.
        // For the purpose of MainViewModel, if we cannot determine the role for permissions, we throw.
        Log.e(TAG, "User $uid signed in, but no student or teacher document found. This indicates an incomplete profile setup.")
        throw IllegalStateException("User document not found for UID: $uid in Firestore collections. Profile incomplete.")
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
                auth.signOut()
                Log.d(TAG, "Firebase user signed out.")

                // 3. Clear local persisted user data in DataStore
                ctx.clear() // This will now clear all user-specific data from DataStore
                Log.d(TAG, "Local user data cleared from preferences.")

                // 4. Update the UI state to reflect the logged-out status
                _uiState.update {
                    it.copy(
                        currentUid = null,
                        userRole = null,
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

    override fun onCleared() {
        userDocListener?.remove() // Ensure the listener is removed to prevent memory leaks
        super.onCleared()
        Log.d(TAG, "MainViewModel cleared. Firestore listener removed.")
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}