package com.bdbshs.crest.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bdbshs.crest.ui.screens.common.ActionBottomSheet
import com.bdbshs.crest.ui.screens.common.SheetAction
import com.bdbshs.crest.ui.viewmodels.DashboardCardItem
import com.bdbshs.crest.ui.viewmodels.SimpleResearch // Import the correct data type
import com.bdbshs.crest.ui.viewmodels.TeacherHomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherHomeScreen(
    modifier: Modifier = Modifier,
    onNavigateToUploadResearch: () -> Unit,
    onNavigateToResearchDetails: (String) -> Unit, // Add callback for details navigation
    viewModel: TeacherHomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSheet by remember { mutableStateOf(false) }

    val teacherActions = listOf(
        SheetAction("Upload a Research", Icons.Outlined.Description) {
            onNavigateToUploadResearch()
            showSheet = false
        },
        SheetAction("Upload a Document", Icons.Outlined.UploadFile) {
            // TODO: Implement this navigation
            showSheet = false
        }
    )

    if (showSheet) {
        ActionBottomSheet(onDismiss = { showSheet = false }, actions = teacherActions)
    }

    Scaffold(
        modifier = modifier,
        floatingActionButton = {
            FloatingActionButton(onClick = { showSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Greeter Card now uses the real name from the ViewModel
                item { GreeterCard(teacherName = uiState.teacherName) }

                // Dashboard cards
                if (uiState.dashboardItems.isNotEmpty()) {
                    item { DashboardGrid(items = uiState.dashboardItems) }
                }

                // Recent Researches list
                if (uiState.recentResearches.isNotEmpty()) {
                    item {
                        Text(
                            text = "Recent Researches",
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                    // Use the new, specific composable for SimpleResearch
                    items(uiState.recentResearches, key = { it.id }) { research ->
                        SimpleResearchListItem(
                            research = research,
                            onClick = { onNavigateToResearchDetails(research.id) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                    }
                }
            }
        }
    }
}

// --- NEW COMPOSABLE: Specifically for SimpleResearch data ---
@Composable
private fun SimpleResearchListItem(research: SimpleResearch, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Article,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = research.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "View Details",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


// GreeterCard now takes a name parameter to display the teacher's name
@Composable
private fun GreeterCard(teacherName: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text("Welcome back,", style = MaterialTheme.typography.titleMedium)
            if (teacherName.isNotBlank()) {
                Text(teacherName, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
        }
    }
}


@Composable
private fun DashboardGrid(items: List<DashboardCardItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (items.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[0]) }
            }
            if (items.size >= 2) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[1]) }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (items.size >= 3) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[2]) }
            }
            if (items.size >= 4) {
                Box(modifier = Modifier.weight(1f)) { DashboardCard(item = items[3]) }
            }
        }
    }
}

@Composable
private fun DashboardCard(item: DashboardCardItem) {
    Card(
        modifier = Modifier.aspectRatio(1f), // Keeps the card square
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column {
                Text(
                    text = item.value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}