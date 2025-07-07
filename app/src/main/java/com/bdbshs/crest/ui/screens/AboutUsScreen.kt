// AboutUsScreen.kt
package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.R
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.viewmodels.UserType

@Composable
fun AboutUsScreen(
    modifier: Modifier = Modifier,
    userRole: UserType? = null // Keeping consistent with other MainScreen children, though not used here
) {
    // Make the content scrollable
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()), // THIS MAKES IT SCROLLABLE
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // NEW: Logo Image at the very top
        Spacer(modifier = Modifier.height(24.dp)) // Add some top padding

        // Placeholder for your actual logo
        Box(
            modifier = Modifier
                .size(100.dp) // Adjust size as needed
                .clip(RoundedCornerShape(20.dp)) // Or CircleShape, or no clip for square logo
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            // Replace this Image composable with your actual logo:
            // Example: Image(painter = painterResource(id = R.drawable.your_crest_logo), contentDescription = "CREST Logo")
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(320.dp)
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(24.dp)) // Space between logo and text

        // App Description Section
        Text(
            text = "CREST",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Compiled Research as an Educational Application for Students and Teachers",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
        )

        // Application Purpose Description
        Text(
            text = "CREST serves as a comprehensive platform designed to empower educators and students by providing centralized access to compiled research and educational documents. Its primary purpose is to streamline the organization, retrieval, and sharing of valuable academic resources, fostering an environment of collaborative learning and efficient knowledge dissemination.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        // Developer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp), // Increased vertical padding for card
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Placeholder Image for Developer
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Developer Placeholder",
                        modifier = Modifier.size(80.dp),
                        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onPrimaryContainer)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "James Ryan S. Gallego",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Developer",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Co-Researchers Section
        Text(
            text = "Co-Researchers",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // 2x2 grid of Co-Researcher Placeholders
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoResearcherCard(name = "Ralph Windel Azana")
                CoResearcherCard(name = "Grant Gabriel Versoza")
            }
            Spacer(modifier = Modifier.height(16.dp))
            // Row 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CoResearcherCard(name = "Herald Ian Lejarde")
                CoResearcherCard(name = "Uriel Sapian")
            }
        }
        Spacer(modifier = Modifier.height(24.dp)) // Add space at the bottom for scrolling
    }
}

@Composable
private fun CoResearcherCard(name: String) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    imageVector = Icons.Default.Person,
                    contentDescription = "$name Placeholder",
                    modifier = Modifier.size(40.dp),
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(MaterialTheme.colorScheme.onSecondaryContainer)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAboutUsScreen() {
    CRESTTheme { // Use your app's theme here
        AboutUsScreen()
    }
}