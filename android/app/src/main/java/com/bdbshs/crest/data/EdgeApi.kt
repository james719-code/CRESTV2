package com.bdbshs.crest.data

import com.bdbshs.crest.BuildConfig
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EdgeApi @Inject constructor(private val auth: FirebaseAuth) {
    suspend fun request(function: String, body: JSONObject): JSONObject {
        val base = BuildConfig.SUPABASE_URL.trimEnd('/')
        check(base.isNotBlank() && URL(base).protocol == "https") { "The school connection is not configured." }
        val user = auth.currentUser ?: error("Sign in again to continue.")
        val token = user.getIdToken(false).await().token ?: error("Sign in again to continue.")
        return withContext(Dispatchers.IO) {
            val connection = URL("$base/functions/v1/$function").openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.doOutput = true
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                val success = connection.responseCode in 200..299
                val stream = if (success) connection.inputStream else connection.errorStream
                val result = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                val json = runCatching { JSONObject(result) }.getOrDefault(JSONObject())
                check(success) { json.optString("error", "The school service could not complete this request.") }
                json
            } finally { connection.disconnect() }
        }
    }

    suspend fun notifyEvent(event: String, recordId: String, role: String? = null) {
        if (!BuildConfig.FCM_ENABLED) return
        try {
            request("crest-notifications", JSONObject().put("action", "notify-event")
                .put("event", event).put("recordId", recordId).put("role", role))
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) { android.util.Log.w("CREST", "Change saved; notification delivery was unavailable.") }
    }
}
