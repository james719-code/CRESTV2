package com.bdbshs.crest.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.bdbshs.crest.BuildConfig
import com.bdbshs.crest.MainActivity
import com.bdbshs.crest.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class CrestMessagingService : FirebaseMessagingService() {
    @Inject lateinit var notifications: PushNotifications
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        if (!BuildConfig.FCM_ENABLED) return
        scope.launch { runCatching { notifications.register(token) } }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (!BuildConfig.FCM_ENABLED || !notifications.optedIn || !notifications.permissionGranted()) return
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (message.data["recipientUid"] != currentUid) return
        val title = message.data["title"] ?: return
        val body = message.data["body"] ?: return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "crest_school_updates"
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(NotificationChannel(channelId, "School updates", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val intent = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify((message.data["eventId"] ?: message.messageId.orEmpty()).hashCode(),
            NotificationCompat.Builder(this, channelId).setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setContentIntent(intent).setAutoCancel(true).build())
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
