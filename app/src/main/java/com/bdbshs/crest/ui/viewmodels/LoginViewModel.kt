package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.AuthRepository
import com.bdbshs.crest.data.repository.UserRepository
import com.bdbshs.crest.data.repository.UserRole
import androidx.credentials.GetCredentialResponse
import com.bdbshs.crest.data.UserPrefs
import com.bdbshs.crest.data.UserPrefs.clear
import com.bdbshs.crest.data.UserPrefs.saveUserData
import com.bdbshs.crest.data.UserPrefs.userDataFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


// Enum for User Type (can be in a shared file)
enum class UserType { STUDENT, TEACHER }


sealed class LoginResult {
    object NavigateToHome             : LoginResult()
    object NavigateToSignUpDetails    : LoginResult()
    object NavigateToPendingApproval  : LoginResult()
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val error:     String?  = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    @ApplicationContext private val ctx: Context
) : ViewModel() {

    private val _uiState    = MutableStateFlow(LoginUiState())
    val uiState             = _uiState.asStateFlow()

    private val _loginResult = MutableSharedFlow<LoginResult>()
    val loginResult         = _loginResult.asSharedFlow()

    init {
        checkForActiveSession()
    }

    private fun isOnline(): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun checkForActiveSession() {
        AuthRepository.getCurrentUser()?.let { user ->
            _uiState.update { it.copy(isLoading = true, error = null) }

            viewModelScope.launch {
                if (!isOnline()) {
                    // Offline: load last known role/perm from DataStore
                    val prefs = ctx.userDataFlow.first()
                    if (prefs.uid == user.uid) {
                        val result = when (prefs.role) {
                            UserType.STUDENT.name ->
                                if (prefs.accepted) LoginResult.NavigateToHome
                                else LoginResult.NavigateToPendingApproval
                            UserType.TEACHER.name ->
                                if (prefs.access)   LoginResult.NavigateToHome
                                else LoginResult.NavigateToPendingApproval
                            else -> LoginResult.NavigateToSignUpDetails
                        }
                        _loginResult.emit(result)
                        _uiState.update { it.copy(isLoading = false) }
                        return@launch
                    }
                    // No matching prefs → fall through to server check
                }
                // Online or no prefs: fetch fresh
                checkUserStatus(user)
            }
        }
    }

    fun signInWithGoogleCredential(response: GetCredentialResponse) {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val idToken = GoogleIdTokenCredential
                    .createFrom(response.credential.data)
                    .idToken
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authRes = AuthRepository.signInWithCredential(credential)
                checkUserStatus(requireNotNull(authRes.user))
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error     = "Sign-in requires an internet connection. Please try again."
                    )
                }
            }
        }
    }

    private fun checkUserStatus(user: FirebaseUser) {
        viewModelScope.launch {
            try {
                val userStatus = UserRepository.getUserStatus(user.uid)
                if (userStatus != null) {
                    val role = when (userStatus.role) {
                        UserRole.STUDENT -> UserType.STUDENT
                        UserRole.TEACHER -> UserType.TEACHER
                    }

                    savePrefs(
                        uid = user.uid,
                        role = role.name,
                        accepted = role == UserType.STUDENT && userStatus.hasPermission,
                        access = role == UserType.TEACHER && userStatus.hasPermission
                    )

                    val dest = if (userStatus.hasPermission) {
                        LoginResult.NavigateToHome
                    } else {
                        LoginResult.NavigateToPendingApproval
                    }

                    _loginResult.emit(dest)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                // new user
                clearPrefs()
                _loginResult.emit(LoginResult.NavigateToSignUpDetails)
                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                val msg = if (e is FirebaseFirestoreException &&
                    e.code == FirebaseFirestoreException.Code.UNAVAILABLE
                ) {
                    "You are offline. Please connect to continue."
                } else {
                    "An error occurred: ${e.message}"
                }
                _uiState.update { it.copy(isLoading = false, error = msg) }
            }
        }
    }

    private suspend fun savePrefs(
        uid: String,
        role: String,
        accepted: Boolean,
        access: Boolean
    ) {
        ctx.saveUserData(
            UserPrefs.UserData(
                uid      = uid,
                role     = role,
                accepted = accepted,
                access   = access
            )
        )
    }

    private suspend fun clearPrefs() {
        ctx.clear()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
