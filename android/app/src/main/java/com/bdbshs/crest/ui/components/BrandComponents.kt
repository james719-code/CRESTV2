package com.bdbshs.crest.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bdbshs.crest.branding.CrestSubmissionStatus
import com.bdbshs.crest.branding.CrestSpacing
import com.bdbshs.crest.ui.theme.crestColors
import com.bdbshs.crest.ui.theme.crestScreenGutter

/** Keep reading and editing surfaces comfortable on both phones and larger windows. */
@Composable
internal fun CrestPageList(
    modifier: Modifier = Modifier,
    spacing: Dp = 16.dp,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit
) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            Modifier.widthIn(max = 840.dp).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = crestScreenGutter(), vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(spacing),
            horizontalAlignment = horizontalAlignment,
            content = content
        )
    }
}

@Composable
internal fun CrestFormPage(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(modifier.fillMaxSize().imePadding(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.widthIn(max = 640.dp).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = crestScreenGutter(), vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content
        )
    }
}

@Composable
internal fun FormSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    CrestOutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                IconBadge(icon, size = 36.dp)
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            }
            content()
        }
    }
}

@Composable
fun PageHeading(title: String, description: String? = null, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(CrestSpacing.xs)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
        description?.let {
            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MetadataBadge(
    label: String,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(shape = RoundedCornerShape(50), color = containerColor, contentColor = contentColor) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = CrestSpacing.sm, vertical = CrestSpacing.xs)
        )
    }
}

@Composable
fun StrandBadge(strand: String) {
    val style = strandStyle(strand)
    MetadataBadge(strand, style.container, style.content)
}

/** Display labels only; persisted workflow statuses remain unchanged. */
@Composable
fun SubmissionStatusBadge(status: CrestSubmissionStatus) {
    val colors = MaterialTheme.crestColors
    val (container, content) = when (status) {
        CrestSubmissionStatus.APPROVED -> colors.success to colors.onSuccess
        CrestSubmissionStatus.PENDING -> colors.info to colors.onInfo
        CrestSubmissionStatus.CHANGES -> colors.warning to colors.onWarning
        CrestSubmissionStatus.NOT_SUBMITTED ->
            MaterialTheme.colorScheme.surfaceContainer to MaterialTheme.colorScheme.onSurfaceVariant
    }
    MetadataBadge(status.label, container, content)
}

@Composable
fun HomeGreeting(name: String, supportingText: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            supportingText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )
    }
}
