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
    onNavigateToTeacherUpload: () -> Unit
) {
    // The Box's modifier is removed, as the modifier will be passed down to the child.
    // This avoids applying padding twice.
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (userRole) {
            UserType.STUDENT -> StudentHomeScreen(
                // The modifier from the parent (which has padding) is passed here.
                modifier = modifier,
                // The default ViewModel is instantiated and scoped to this screen.
                viewModel = viewModel(),
                onNavigateToUpload = onNavigateToGroupUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails,
            )
            UserType.TEACHER -> TeacherHomeScreen(
                // The modifier from the parent is also passed here for consistency.
                modifier = modifier,
                viewModel = viewModel(),
                onNavigateToUploadResearch = onNavigateToTeacherUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails
            )
            else -> {
                // Shows a loading spinner until the userRole is determined.
                CircularProgressIndicator()
            }
        }
    }
}