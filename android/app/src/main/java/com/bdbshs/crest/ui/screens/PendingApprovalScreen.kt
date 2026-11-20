package com.bdbshs.crest.ui.screens

import com.bdbshs.crest.ui.components.CrestOutlinedButton as OutlinedButton
import com.bdbshs.crest.ui.theme.crestScreenGutter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.bdbshs.crest.branding.CrestBrandName
import com.bdbshs.crest.branding.CrestWordmarkTrackingEm
import com.bdbshs.crest.ui.components.CrestCard
import com.bdbshs.crest.ui.components.CrestMark
import com.bdbshs.crest.ui.components.IconBadge
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

@Composable
fun PendingApprovalScreen(
    accountEmail: String = runCatching { Firebase.auth.currentUser?.email.orEmpty() }.getOrDefault(""),
    onSignOut: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            Modifier.widthIn(max = 480.dp).fillMaxWidth().padding(horizontal = crestScreenGutter(), vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            CrestMark(72.dp)
            Text(
                CrestBrandName,
                style = MaterialTheme.typography.headlineLarge.copy(letterSpacing = CrestWordmarkTrackingEm.em),
                color = MaterialTheme.colorScheme.onSurface
            )
            CrestCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(Icons.Outlined.HourglassTop, size = 56.dp)
                    Text(
                        "Waiting for approval",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "Your registration was sent. A teacher needs to approve it before you can open the school library.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Once you are approved, reopen CREST to continue.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (accountEmail.isNotBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.AccountCircle, null, tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Signed in as $accountEmail",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) {
                Text("Use a different Google account", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Preview(name = "Waiting · Light", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
fun PendingApprovalScreenPreview() {
    CRESTTheme { PendingApprovalScreen(onSignOut = {}, accountEmail = "jane.santos@school.edu.ph") }
}

@Preview(name = "Waiting · Dark", showBackground = true, widthDp = 360, heightDp = 720)
@Composable
private fun PendingApprovalDarkPreview() {
    CRESTTheme(darkTheme = true) { PendingApprovalScreen(onSignOut = {}, accountEmail = "jane.santos@school.edu.ph") }
}
