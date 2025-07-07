package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bdbshs.crest.ui.viewmodels.UserType

// In HomeScreen.kt

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    userRole: UserType?,
    onNavigateToGroupUpload: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit, // This must be present
    onNavigateToTeacherUpload: () -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (userRole) {
            UserType.STUDENT -> StudentHomeScreen(
                modifier = Modifier.fillMaxSize(),
                onNavigateToUpload = onNavigateToGroupUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails
            )
            UserType.TEACHER -> TeacherHomeScreen(
                modifier = Modifier.fillMaxSize(),
                onNavigateToUploadResearch = onNavigateToTeacherUpload,
                onNavigateToResearchDetails = onNavigateToResearchDetails // Pass it down
            )
            else -> {
                CircularProgressIndicator()
            }
        }
    }
}