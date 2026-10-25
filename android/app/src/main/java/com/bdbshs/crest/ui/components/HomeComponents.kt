package com.bdbshs.crest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.ui.viewmodels.ResearchItem
import com.bdbshs.crest.ui.viewmodels.ResearchType

@Composable
internal fun HomeSummary(name: String, workspace: String, metrics: List<Pair<String, String>>) {
    Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "${timeGreeting()} · $workspace",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
        if (metrics.isNotEmpty()) {
            Text(
                metrics.joinToString("  ·  ") { (value, label) -> "$value ${label.lowercase()}" },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Two items side by side; stacked once text is enlarged so labels never break mid-word. */
@Composable
internal fun AdaptivePair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    if (LocalDensity.current.fontScale > 1.3f) {
        Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            first(Modifier.weight(1f).fillMaxHeight())
            second(Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
internal fun HomeSectionHeading(title: String, count: Int? = null) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f).semantics { heading() })
        count?.let { MetadataBadge(it.toString()) }
    }
}

/** Home shows the same paper card as the library so a paper looks identical everywhere. */
@Composable
internal fun HomeResearchRow(research: ResearchItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ModernResearchCard(research, onClick, modifier = modifier)
}

@Composable
internal fun HomeEmptyResearch(title: String, message: String, actionLabel: String, onAction: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 0.dp)) { Text(actionLabel) }
        }
    }
}
