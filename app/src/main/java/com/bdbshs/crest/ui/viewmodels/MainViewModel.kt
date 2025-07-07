package com.bdbshs.crest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class MainUiState(
    val userRole: UserType? = null,
    val isLoading: Boolean = true
)

class MainViewModel : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()

    init {
        fetchUserRole()
    }

    private fun fetchUserRole() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid

            if (uid == null) {
                // No user, stop loading. The AuthStateListener will handle navigation.
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            try {
                // --- THIS IS THE CRITICAL FIX ---
                // The .get() call is now safely wrapped in a try-catch block.
                val studentDoc = firestore.collection("users/user_details/students").document(uid).get().await()
                if (studentDoc.exists()) {
                    _uiState.update { it.copy(userRole = UserType.STUDENT, isLoading = false) }
                    return@launch
                }

                val teacherDoc = firestore.collection("users/user_details/teachers").document(uid).get().await()
                if (teacherDoc.exists()) {
                    _uiState.update { it.copy(userRole = UserType.TEACHER, isLoading = false) }
                    return@launch
                }

                // Fallback: User is authenticated but has no role document.
                _uiState.update { it.copy(isLoading = false, userRole = null) }

            } catch (e: Exception) {
                // This will catch the "client is offline" exception and prevent the crash.
                // We can't determine the role, so we stop loading and let the UI decide what to show.
                _uiState.update { it.copy(isLoading = false, userRole = null) }
            }
        }
    }
}