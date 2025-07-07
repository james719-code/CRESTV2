package com.bdbshs.crest.ui.screens.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Data class to define an action in the bottom sheet
data class SheetAction(
    val title: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionBottomSheet(
    onDismiss: () -> Unit,
    actions: List<SheetAction>
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(bottom = 32.dp) // Add padding for gesture navigation bar
        ) {
            actions.forEach { action ->
                ListItem(
                    modifier = Modifier.clickable(onClick = action.onClick),
                    headlineContent = { Text(action.title) },
                    leadingContent = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = action.title
                        )
                    }
                )
            }
        }
    }
}