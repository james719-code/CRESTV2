package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.components.AppLogo
import com.bdbshs.crest.R
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
                "CREST brings our school’s research into one accessible library, connecting the work students create with the people who can learn from it.")
        }
        item {
            AboutSection("Our name",
                "Compiled Research as an Educational Application for Students and Teachers")
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Image(painterResource(R.drawable.bdbshs_school_logo),
                        contentDescription = "Bonifacio D. Borebor Sr. High School seal", modifier = Modifier.size(120.dp))
                    Text("Bonifacio D. Borebor Sr. High School", style = MaterialTheme.typography.titleLarge)
                    Text("Built for our school community, CREST gives students and teachers a shared space to develop research, exchange feedback, and preserve completed work for future learners.",
                        style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            AboutSection("Why we built CREST",
                "Research should keep helping people after the final submission. When papers are kept in separate files or printed copies, useful ideas can be difficult to find and valuable student work can be overlooked. We built CREST to make school research easier to discover, organize, and preserve.")
        }
        item {
            AboutSection("From a first draft to a shared discovery",
                "Students can form groups and submit manuscripts. Teachers can review submissions, give clear feedback, and approve completed research for the library. Having these steps together helps every group understand what comes next.")
        }
        item {
            AboutSection("A resource for the next researchers",
                "Explore previous studies, learn how research is presented, and build on ideas with proper acknowledgment of the original authors. Shared templates, rubrics, and school documents keep useful guidance close at hand. Through the Android app and teacher website, CREST helps make research a lasting contribution to our school community.")
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
