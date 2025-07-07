package com.bdbshs.crest.ui.viewmodels

import androidx.credentials.GetCredentialResponse
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.FirebaseClient // <-- Import the new client
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class LoginResult {
    object NavigateToHome : LoginResult()
    object NavigateToSignUpDetails : LoginResult()
    object NavigateToPendingApproval : LoginResult()
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel : ViewModel() {

    // --- REFACTORED: Use the centralized FirebaseClient ---
    private val auth = FirebaseClient.auth
    private val firestore = FirebaseClient.firestore

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult = _loginResult.asSharedFlow()

    init {
        checkForActiveSession()
    }

    private fun checkForActiveSession() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            _uiState.update { it.copy(isLoading = true) }
            checkUserStatus(currentUser)
        }
    }

    fun signInWithGoogleCredential(response: GetCredentialResponse) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(response.credential.data)
                val idToken = googleIdTokenCredential.idToken
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                checkUserStatus(authResult.user!!)

            } catch (e: Exception) {
                // --- UPDATED: Better offline error message ---
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Sign-in requires an internet connection. Please check your network and try again."
                    )
                }
            }
        }
    }

    // This function is now offline-capable because it uses the configured Firestore instance
    private fun checkUserStatus(user: FirebaseUser) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // This is the source of the crash. We will now handle the exception gracefully.
                val studentDoc = firestore.collection("users/user_details/students").document(user.uid).get().await()
                if (studentDoc.exists()) {
                    val isAccepted = studentDoc.getBoolean("accepted") ?: false
                    if (isAccepted) {
                        _loginResult.emit(LoginResult.NavigateToHome)
                    } else {
                        _loginResult.emit(LoginResult.NavigateToPendingApproval)
                    }
                    return@launch
                }

                val teacherDoc = firestore.collection("users/user_details/teachers").document(user.uid).get().await()
                if (teacherDoc.exists()) {
                    val hasAccess = teacherDoc.getBoolean("access") ?: false
                    if (hasAccess) {
                        _loginResult.emit(LoginResult.NavigateToHome)
                    } else {
                        _loginResult.emit(LoginResult.NavigateToPendingApproval)
                    }
                    return@launch
                }

                _loginResult.emit(LoginResult.NavigateToSignUpDetails)

            } catch (e: Exception) {
                // --- THIS IS THE CRITICAL FIX ---
                // Catch the specific offline exception and update the UI state instead of crashing.
                if (e is FirebaseFirestoreException && e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                    _uiState.update {
                        it.copy(error = "You are offline. Please connect to continue.")
                    }
                } else {
                    // Handle other potential errors
                    _uiState.update { it.copy(error = "An error occurred: ${e.message}") }
                }
            } finally {
                // This ensures the loading spinner always stops.
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }



    // The findUserType function is not used in the current login flow, but if it were,
    // it would also benefit from offline support automatically.
    private suspend fun findUserType(uid: String): UserType? {
        val base = firestore.collection("users").document("user_details")
        val student = base.collection("students").document(uid).get().await()
        if (student.exists()) return UserType.STUDENT

        val teacher = base.collection("teachers").document(uid).get().await()
        if (teacher.exists()) return UserType.TEACHER

        return null
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}