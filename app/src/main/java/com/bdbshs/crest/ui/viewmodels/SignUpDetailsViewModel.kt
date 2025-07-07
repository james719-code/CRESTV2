package com.bdbshs.crest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

// Enum for User Type (can be in a shared file)
enum class UserType { STUDENT, TEACHER }

// Sealed class to hold the specific details for each user type
sealed class UserDetails {
    data class Student(
        val name: String,
        val lrn: Long,
        val strand: String,
        val gender: String,
        val groupId: String = "" // Optional, can be set later
    ) : UserDetails()

    data class Teacher(
        val name: String,
        val email: String
    ) : UserDetails()
}

// UI State for the screen
data class SignUpDetailsUiState(
    val selectedRole: UserType? = null,
    val isRoleSelectionDialogVisible: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SignUpDetailsViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(SignUpDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _navigateToHome = MutableSharedFlow<Unit>()
    val navigateToHome = _navigateToHome.asSharedFlow()

    fun onRoleSelected(role: UserType) {
        _uiState.update { it.copy(selectedRole = role) }
    }

    fun saveUserDetails(details: UserDetails) {
        // Launch a coroutine on the Main dispatcher. This is the default for viewModelScope.
        viewModelScope.launch {
            // 1. Immediately update the UI state to show the loading indicator.
            // This happens on the main thread for instant feedback.
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // 2. Switch to the IO dispatcher for all logic and network calls.
                withContext(Dispatchers.IO) {
                    val user = auth.currentUser
                    if (user == null) {
                        // We can't proceed, so we switch back to update the UI with an error.
                        // We need to do this on the main thread.
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(isLoading = false, error = "No user is logged in.") }
                        }
                        return@withContext // Exit the withContext(Dispatchers.IO) block
                    }

                    // This logic is now guaranteed to be on a background thread.
                    val collectionPath: String
                    val data: Map<String, Any>

                    when (details) {
                        is UserDetails.Student -> {
                            collectionPath = "users/user_details/students"
                            data = mapOf(
                                "name" to details.name,
                                "lrn" to details.lrn,
                                "strand" to details.strand,
                                "gender" to details.gender,
                                "group_id" to details.groupId,
                                "accepted" to false,
                                "research_accepted" to false,
                                "uid" to user.uid
                            )
                        }
                        is UserDetails.Teacher -> {
                            collectionPath = "users/user_details/teachers"
                            data = mapOf(
                                "name" to details.name,
                                "email" to details.email,
                                "access" to false,
                                "upload_count" to 0,
                                "uid" to user.uid
                            )
                        }
                    }

                    // The network call to Firestore is now also explicitly on the IO dispatcher.
                    firestore.collection(collectionPath).document(user.uid).set(data).await()
                }

                // 3. After the background work is done, execution resumes on the Main dispatcher.
                // We can now safely emit the navigation event.
                _navigateToHome.emit(Unit)

            } catch (e: Exception) {
                // 4. If any exception occurred (either in the IO block or elsewhere),
                // update the UI with the error message.
                _uiState.update { it.copy(isLoading = false, error = "Failed to save details: ${e.message}") }
            } finally {
                // 5. Regardless of success or failure, ensure the loading state is turned off.
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}