package com.bdbshs.crest.notifications

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bdbshs.crest.BuildConfig
import kotlinx.coroutines.launch

@Composable
fun NotificationOptIn(uid: String?, notifications: PushNotifications) {
    if (!BuildConfig.FCM_ENABLED || uid.isNullOrBlank()) return
    var dismissed by remember(uid) { mutableStateOf(notifications.optedIn) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val enable: () -> Unit = {
        scope.launch {
            try { notifications.enable(); dismissed = true }
            catch (_: Exception) { Toast.makeText(context, "Notifications could not be enabled. Try again later.", Toast.LENGTH_LONG).show() }
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else dismissed = true
    }
    if (!dismissed) AlertDialog(
        onDismissRequest = { dismissed = true },
        title = { Text("School notifications") },
        text = { com.bdbshs.crest.ui.theme.CrestDialogSystemBars(); Text("Receive account approvals and research review updates on this device?") },
        confirmButton = { TextButton(onClick = {
            if (Build.VERSION.SDK_INT >= 33 && !notifications.permissionGranted()) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            else enable()
        }) { Text("Enable notifications") } },
        dismissButton = { TextButton(onClick = { dismissed = true }) { Text("Later") } }
    )
}
