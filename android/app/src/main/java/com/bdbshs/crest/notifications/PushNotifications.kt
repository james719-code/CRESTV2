package com.bdbshs.crest.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.bdbshs.crest.BuildConfig
import com.bdbshs.crest.data.EdgeApi
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PushNotifications @Inject constructor(
    @ApplicationContext private val context: Context,
    private val auth: FirebaseAuth,
    private val edge: EdgeApi
) {
    private val prefs = context.getSharedPreferences("crest_push", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val optedIn: Boolean get() = prefs.getBoolean("opted_in", false)
    fun permissionGranted(): Boolean = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun start() {
        if (!BuildConfig.FCM_ENABLED) return
        FirebaseMessaging.getInstance().isAutoInitEnabled = optedIn
        auth.addAuthStateListener { if (it.currentUser != null && optedIn) refresh() }
    }

    fun refresh() {
        if (!BuildConfig.FCM_ENABLED || !optedIn || !permissionGranted() || auth.currentUser == null) return
        scope.launch {
            try { register(FirebaseMessaging.getInstance().token.await()) }
            catch (_: Exception) { android.util.Log.w("CREST", "Notification registration will be retried on next sign-in.") }
        }
    }

    suspend fun enable() {
        check(BuildConfig.FCM_ENABLED && permissionGranted()) { "Allow notifications to receive school updates." }
        check(auth.currentUser != null) { "Sign in first." }
        prefs.edit().putBoolean("opted_in", true).apply()
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        register(FirebaseMessaging.getInstance().token.await())
    }

    suspend fun register(token: String) {
        if (!BuildConfig.FCM_ENABLED || !optedIn || auth.currentUser == null || !permissionGranted()) return
        val old = prefs.getString("token", null)
        edge.request("crest-notifications", JSONObject().put("action", "register-device").put("token", token).put("platform", "android"))
        prefs.edit().putString("token", token).apply()
        if (old != null && old != token) edge.request("crest-notifications", JSONObject().put("action", "remove-device").put("token", old))
    }

    suspend fun disable() {
        val token = prefs.getString("token", null)
        // Suppress queued messages locally before attempting network cleanup.
        prefs.edit().clear().apply()
        if (!BuildConfig.FCM_ENABLED) return
        FirebaseMessaging.getInstance().isAutoInitEnabled = false
        if (token != null && auth.currentUser != null) {
            runCatching { edge.request("crest-notifications", JSONObject().put("action", "remove-device").put("token", token)) }
        }
        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
    }
}
