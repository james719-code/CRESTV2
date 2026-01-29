package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bdbshs.crest.R
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.bdbshs.crest.ui.theme.LocalSpacing
import com.bdbshs.crest.ui.viewmodels.UserType

@Composable
fun AboutUsScreen(
    modifier: Modifier = Modifier,
    userRole: UserType? = null
) {
    val spacing = LocalSpacing.current
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Premium Header Section ---
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp) // Slightly taller to accommodate larger text
            ) {
                // Background Gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    colorScheme.primary.copy(alpha = 0.25f), // Slightly stronger gradient
                                    colorScheme.background
                                )
                            )
                        )
                )

                // Circle Decorations
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .offset(x = (-100).dp, y = (-100).dp)
                        .clip(CircleShape)
                        .background(colorScheme.primary.copy(alpha = 0.08f))
                )
                
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 50.dp, y = (-50).dp)
                        .clip(CircleShape)
                        .background(colorScheme.secondary.copy(alpha = 0.08f))
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = spacing.large, vertical = spacing.extraLarge)
                        .statusBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Elevated Logo Container
                    Surface(
                        shape = CircleShape,
                        color = colorScheme.surface,
                        shadowElevation = 16.dp, // Increased elevation
                        tonalElevation = 12.dp,
                        modifier = Modifier
                            .size(110.dp) // Slight increase
                            .border(
                                width = 1.dp,
                                color = colorScheme.outlineVariant.copy(alpha = 0.4f),
                                shape = CircleShape
                            )
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(16.dp) // Fixed padding inside circle
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "App Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(spacing.medium))

                    Text(
                        text = "CREST",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontWeight = FontWeight.Black, // Boldest weight
                            letterSpacing = (-1).sp
                        ),
                        color = colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(spacing.small))

                    Text(
                        text = "Compiled Research as an Educational Application\nfor Students and Teachers",
                        style = MaterialTheme.typography.titleMedium, // Bumped from bodyMedium
                        textAlign = TextAlign.Center,
                        color = colorScheme.onBackground.copy(alpha = 0.9f), // Increased alpha for readability
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        item {
            Column(
                modifier = Modifier
                    .offset(y = (-20).dp) // Reduced overlap for more breathing room
                    .padding(horizontal = spacing.medium),
                verticalArrangement = Arrangement.spacedBy(spacing.large) 
            ) {
                // --- Mission Statement ---
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(spacing.large)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.School,
                                        contentDescription = null, // Decorative
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(spacing.medium))
                            Text(
                                text = "Our Mission",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = colorScheme.onSurface
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(spacing.medium))
                        
                        Text(
                            text = "CREST empowers educators and students with centralized access to compiled research. We streamline organization, retrieval, and sharing of valuable academic resources.",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                lineHeight = 26.sp
                            ),
                            color = colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                    }
                }

                // --- School Dedication ---
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorScheme.surfaceContainerHigh 
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .padding(spacing.large)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(spacing.medium)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.secondary,
                            modifier = Modifier.size(56.dp) 
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "BDB",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = colorScheme.onSecondary
                                )
                            }
                        }
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Dedicated to",
                                style = MaterialTheme.typography.labelLarge.copy(color = colorScheme.secondary),
                                modifier = Modifier.padding(bottom = 2.dp)
                            )
                            Text(
                                text = "Bonifacio D. Borebor\nSr. High School",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = colorScheme.onSurface,
                                lineHeight = 24.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Built with a sincere heart for the advancement of our school's educational system.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // --- Team Section ---
                Column(modifier = Modifier.padding(top = spacing.small)) {
                    Text(
                        text = "The Team",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), 
                        color = colorScheme.onBackground,
                        modifier = Modifier.padding(bottom = spacing.medium, start = spacing.small)
                    )

                    // Lead Dev
                    DeveloperProfileCard()
                    
                    Spacer(modifier = Modifier.height(spacing.medium))

                    // Co-Researchers
                    Text(
                         text = "Co-Researchers",
                         style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                         color = colorScheme.onBackground.copy(alpha = 0.8f),
                         modifier = Modifier.padding(bottom = spacing.small, start = spacing.small)
                    )
                    CoResearchersGrid()
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(spacing.extraLarge))
            
            // Footer text
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.padding(bottom = spacing.large)
            )
        }
    }
}

@Composable
private fun DeveloperProfileCard() {
    val spacing = LocalSpacing.current
    val colorScheme = MaterialTheme.colorScheme
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.primaryContainer 
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        // Gradient overlay
        Box(
            modifier = Modifier.background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primaryContainer,
                        colorScheme.primaryContainer.copy(alpha = 0.8f) 
                    )
                )
            )
        ) {
            Row(
                modifier = Modifier.padding(spacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = colorScheme.primary, // Pop against container
                    modifier = Modifier.size(72.dp),
                    shadowElevation = 6.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = colorScheme.onPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(spacing.medium))
                
                Column {
                    Text(
                        text = "James Ryan S. Gallego",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Lead Developer & Proponent",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Surface(
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Text(
                            text = "Full Stack Developer",
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CoResearchersGrid() {
    val spacing = LocalSpacing.current
    val colorScheme = MaterialTheme.colorScheme
    val researchers = listOf(
        "Ralph Windel Azana",
        "Grant Gabriel Versoza",
        "Herald Ian Lejarde",
        "Uriel Sapian"
    )

    Column(verticalArrangement = Arrangement.spacedBy(spacing.small)) {
        researchers.chunked(2).forEach { rowResearchers ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(spacing.small)
            ) {
                rowResearchers.forEach { name ->
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(
                            containerColor = colorScheme.surfaceVariant // slightly lighter in dark mode?
                        ),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp), // More padding
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.tertiaryContainer,
                                modifier = Modifier.size(40.dp) // Larger avatar
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = name.take(1),
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.width(spacing.small))
                            
                            Column {
                                Text(
                                    text = name.split(" ").first(),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                                Text(
                                     text = "Researcher", // Add role text
                                     style = MaterialTheme.typography.labelSmall,
                                     color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                if (rowResearchers.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Preview(showBackground = true, heightDp = 1000)
@Composable
fun PreviewAboutUsScreen() {
    CRESTTheme {
        AboutUsScreen()
    }
}