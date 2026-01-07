package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
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
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (userRole) {
            UserType.STUDENT -> StudentHomeScreen(
                modifier = modifier,
                viewModel = viewModel(),
                onNavigateToUpload = onNavigateToGroupUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails,
            )
            UserType.TEACHER -> TeacherHomeScreen(
                modifier = modifier,
                viewModel = viewModel(),
                onNavigateToUploadResearch = onNavigateToTeacherUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails,
                onNavigateToAccounts = onNavigateToAccounts
            )
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}