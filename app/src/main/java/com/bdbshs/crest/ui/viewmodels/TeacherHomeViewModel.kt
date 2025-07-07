package com.bdbshs.crest.ui.viewmodels

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.FirebaseClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Immutable
data class DashboardCardItem(
    val title: String,
    val value: String,
    val icon: ImageVector
)

// NEW: A simple data class for recent researches, separate from the more detailed ResearchItem
// This keeps the home screen's data model lean.
data class SimpleResearch(
    val id: String,
    val title: String
)

data class TeacherHomeUiState(
    val dashboardItems: List<DashboardCardItem> = emptyList(),
    val recentResearches: List<SimpleResearch> = emptyList(),
    val teacherName: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

class TeacherHomeViewModel : ViewModel() {

    private val firestore = FirebaseClient.firestore
    private val auth = FirebaseClient.auth

    private val _uiState = MutableStateFlow(TeacherHomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Switch to fetching real data
        fetchRealData()
    }

    private fun fetchRealData() {
        val uid = auth.currentUser?.uid ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // --- Fetch all data points concurrently for performance ---
                // These .get() calls will read from the cache when offline

                // 1. Fetch Teacher's Name
                val teacherNameDeferred = async {
                    firestore.collection("users/user_details/teachers").document(uid).get().await()
                        .getString("name") ?: "Teacher"
                }

                // 2. Fetch Dashboard Counts
                val totalResearchesDeferred = async {
                    (firestore.collection("researches/research_details/qualitative").get().await().size() +
                            firestore.collection("researches/research_details/quantitative").get().await().size()).toString()
                }
                val totalAccountsDeferred = async {
                    (firestore.collection("users/user_details/students").get().await().size() +
                            firestore.collection("users/user_details/teachers").get().await().size()).toString()
                }
                val pendingResearchesDeferred = async {
                    firestore.collection("groups").whereEqualTo("uploaded", true)
                        .whereEqualTo("accepted_research", false).get().await().size().toString()
                }
                val pendingAccountsDeferred = async {
                    val pendingStudents = firestore.collection("users/user_details/students")
                        .whereEqualTo("accepted", false).get().await().size()
                    val pendingTeachers = firestore.collection("users/user_details/teachers")
                        .whereEqualTo("access", false).get().await().size()
                    (pendingStudents + pendingTeachers).toString()
                }

                // 3. Fetch Recent Researches (can be simplified for home screen)
                val recentResearchesDeferred = async {
                    val qualitative = firestore.collection("researches/research_details/qualitative")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(3).get().await()
                    val quantitative = firestore.collection("researches/research_details/quantitative")
                        .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(3).get().await()

                    (qualitative.documents + quantitative.documents)
                        .mapNotNull { doc -> doc.getString("title")?.let { SimpleResearch(doc.id, it) } }
                        .sortedByDescending { it.id } // Crude sort for combining, assuming IDs are time-based
                        .take(5)
                }

                // --- Await all results and update the UI state once ---
                val dashboardItems = listOf(
                    DashboardCardItem("Total Researches", totalResearchesDeferred.await(), Icons.Default.Description),
                    DashboardCardItem("Total Accounts", totalAccountsDeferred.await(), Icons.Default.Group),
                    DashboardCardItem("Pending Researches", pendingResearchesDeferred.await(), Icons.Default.PendingActions),
                    DashboardCardItem("Pending Accounts", pendingAccountsDeferred.await(), Icons.Default.PersonAdd)
                )

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        teacherName = teacherNameDeferred.await(),
                        dashboardItems = dashboardItems,
                        recentResearches = recentResearchesDeferred.await(),
                        error = null
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Could not load dashboard. Please check your network connection.")
                }
            }
        }
    }

    fun onRefresh() {
        // Refresh now fetches real data
        fetchRealData()
    }
}