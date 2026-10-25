package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.bdbshs.crest.ui.viewmodels.UserType

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userRole: UserType?,
    onNavigateToGroupUpload: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit,
    onNavigateToTeacherUpload: () -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        when (userRole) {
            UserType.STUDENT -> StudentHomeScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = hiltViewModel(),
                onNavigateToUpload = onNavigateToGroupUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails,
            )
            UserType.TEACHER -> TeacherHomeScreen(
                modifier = Modifier.fillMaxSize(),
                viewModel = hiltViewModel(),
                onNavigateToUploadResearch = onNavigateToTeacherUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails,
                onNavigateToAccounts = onNavigateToAccounts
            )
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}