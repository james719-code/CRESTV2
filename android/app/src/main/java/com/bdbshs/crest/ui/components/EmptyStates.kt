package com.bdbshs.crest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.LibraryBooks
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun ModernEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(icon, null, Modifier.padding(16.dp).size(32.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 360.dp)
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction, modifier = Modifier.heightIn(min = 48.dp), shape = MaterialTheme.shapes.small) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
fun ResearchEmptyState(
    isSearchResult: Boolean = false,
    searchQuery: String = "",
    onClearSearch: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    ModernEmptyState(
        icon = if (isSearchResult) Icons.Outlined.SearchOff else Icons.AutoMirrored.Outlined.LibraryBooks,
        title = if (isSearchResult) "No papers match your search" else "No research papers yet",
        description = if (isSearchResult) "Try another title or clear the filters." else "Papers will appear here when they are added to the library.",
        actionLabel = if (isSearchResult) "Clear search" else null,
        onAction = onClearSearch,
        modifier = modifier
    )
}

@Composable
fun GroupsEmptyState(onCreateGroup: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    ModernEmptyState(Icons.Outlined.Groups, "No groups yet",
        "Create or join a group to work on research with your classmates.",
        if (onCreateGroup != null) "Create a group" else null, onCreateGroup, modifier)
}

@Composable
fun DocumentsEmptyState(onUpload: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    ModernEmptyState(Icons.Outlined.FolderOpen, "No documents yet",
        "School resources and templates will appear here.",
        if (onUpload != null) "Upload document" else null, onUpload, modifier)
}

@Composable
fun OfflineEmptyState(onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    ModernEmptyState(Icons.Outlined.CloudOff, "You're offline",
        "Saved papers are available. Connect to the internet for the rest of the library.",
        if (onRetry != null) "Try again" else null, onRetry, modifier)
}

@Composable
fun ErrorEmptyState(message: String = "Something went wrong", onRetry: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    ModernEmptyState(Icons.Outlined.ErrorOutline, "Could not load this content",
        message, if (onRetry != null) "Try again" else null, onRetry, modifier)
}

@Composable
fun LoadingState(message: String = "Loading…", modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(32.dp))
        Spacer(Modifier.height(16.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun PendingApprovalEmptyState(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
        ModernEmptyState(Icons.Outlined.HourglassTop, "Waiting for account approval",
            "A teacher needs to approve your registration before you can access the library.")
    }
}
