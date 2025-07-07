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
    val isLoading: Boolean = false,
    val error: String? = null,
    // Fields for strand dropdown
    val selectedStrand: String = "", // Holds the selected strand text
    val isStrandDropdownExpanded: Boolean = false, // Controls dropdown expansion
    // New fields for gender dropdown
    val selectedGender: String = "", // Holds the selected gender text
    val isGenderDropdownExpanded: Boolean = false // Controls gender dropdown expansion
)

class SignUpDetailsViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(SignUpDetailsUiState())
    val uiState = _uiState.asStateFlow()

    // Changed: This flow now emits a LoginResult to indicate the post-signup navigation.
    // This aligns with the LoginViewModel's navigation logic for consistency.
    private val _signUpNavigationEvent = MutableSharedFlow<LoginResult>()
    val signUpNavigationEvent = _signUpNavigationEvent.asSharedFlow()

    // Function to set the selected role
    fun onRoleSelected(role: UserType) {
        _uiState.update { it.copy(
            selectedRole = role,
            // Reset strand/gender if role changes, ensuring a clean form state
            selectedStrand = "",
            isStrandDropdownExpanded = false,
            selectedGender = "",
            isGenderDropdownExpanded = false
        ) }
    }

    // Functions for strand dropdown
    fun onStrandSelected(strand: String) {
        _uiState.update { it.copy(selectedStrand = strand, isStrandDropdownExpanded = false) }
    }

    fun onStrandDropdownExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isStrandDropdownExpanded = expanded) }
    }

    // Functions for gender dropdown
    fun onGenderSelected(gender: String) {
        _uiState.update { it.copy(selectedGender = gender, isGenderDropdownExpanded = false) }
    }

    fun onGenderDropdownExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isGenderDropdownExpanded = expanded) }
    }

    fun saveUserDetails(details: UserDetails) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Get current user in IO dispatcher
                val user = withContext(Dispatchers.IO) {
                    auth.currentUser
                }

                if (user == null) {
                    _uiState.update { it.copy(isLoading = false, error = "No user is logged in.") }
                    return@launch // Exit if no user is logged in
                }

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
                            "accepted" to false, // Correctly set to false, awaiting admin approval
                            "research_accepted" to false, // Assuming initial status
                            "uid" to user.uid
                        )
                    }
                    is UserDetails.Teacher -> {
                        collectionPath = "users/user_details/teachers"
                        data = mapOf(
                            "name" to details.name,
                            "email" to details.email,
                            "access" to false, // Correctly set to false, awaiting admin approval
                            "upload_count" to 0,
                            "uid" to user.uid
                        )
                    }
                }

                // Perform Firestore write operation in the IO dispatcher
                withContext(Dispatchers.IO) {
                    firestore.collection(collectionPath).document(user.uid).set(data).await()
                }

                // After successfully saving details with initial 'false' status,
                // navigate to the pending approval screen.
                _signUpNavigationEvent.emit(LoginResult.NavigateToPendingApproval)

            } catch (e: Exception) {
                // Catch any exception during the process and update the UI with an error message.
                _uiState.update { it.copy(isLoading = false, error = "Failed to save details: ${e.localizedMessage ?: e.message}") }
            } finally {
                // Ensure isLoading is always set to false regardless of success or failure.
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // New function to allow changing role after initial selection
    fun resetRoleSelection() {
        _uiState.update {
            it.copy(
                selectedRole = null,
                selectedStrand = "",
                isStrandDropdownExpanded = false,
                selectedGender = "",
                isGenderDropdownExpanded = false
            )
        }
    }
}