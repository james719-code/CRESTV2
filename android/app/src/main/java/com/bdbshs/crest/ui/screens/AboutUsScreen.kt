package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.components.AppLogo
import com.bdbshs.crest.ui.viewmodels.UserType

@Composable
fun AboutUsScreen(modifier: Modifier = Modifier, userRole: UserType? = null) {
    LazyColumn(
        modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { AppLogo(Modifier.padding(vertical = 16.dp)) }
        item {
            AboutSection("School research, shared.",
                "CREST helps students and teachers find, submit, review, and preserve school research.")
        }
        item {
            AboutSection("Our name",
                "Compiled Research as an Educational Application for Students and Teachers")
        }
        item {
            AboutSection("Our school",
                "Bonifacio D. Borebor Sr. High School")
        }
        item { DeveloperProfileCard() }
        item {
            Text("Co-researchers", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            CoResearchersGrid()
        }
        item {
            Text(
                "Version ${com.bdbshs.crest.BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutSection(title: String, body: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeveloperProfileCard() {
    AboutSection("Built by James Ryan S. Gallego", "Lead developer and proponent")
}

@Composable
private fun CoResearchersGrid() {
    AboutSection(
        "Research team",
        listOf("Ralph Windel Azana", "Grant Gabriel Versoza", "Herald Ian Lejarde", "Uriel Sapian").joinToString("\n")
    )
}
