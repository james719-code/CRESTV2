package com.bdbshs.crest.data

import com.bdbshs.crest.BuildConfig
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CrestStorage @Inject constructor(private val legacy: Storage, private val edge: EdgeApi) {
    private val supabaseReference = Regex("^sb_[0-9a-f-]{36}$")
    private fun requireEnabled() {
        check(BuildConfig.STORAGE_PROVIDER in listOf("appwrite", "supabase")) { "School file storage is not enabled yet." }
    }

    suspend fun upload(file: File, kind: String, groupId: String? = null): String {
        requireEnabled()
        withContext(Dispatchers.IO) {
            check(file.length() in 1..20L * 1024 * 1024) { "Choose a PDF of up to 20 MB." }
            val header = ByteArray(5)
            check(file.inputStream().use { it.read(header) } == 5 && header.toString(Charsets.US_ASCII) == "%PDF-") { "Choose a valid PDF." }
        }
        if (BuildConfig.STORAGE_PROVIDER == "appwrite") {
            return legacy.createFile(StorageConfig.BUCKET_ID, ID.unique(), InputFile.fromFile(file = file)).id
        }
        val signed = edge.request("crest-storage", JSONObject().put("action", "create-upload")
            .put("kind", kind).put("groupId", groupId).put("size", file.length()))
        withContext(Dispatchers.IO) {
            val connection = URL(signed.getString("url")).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "PUT"
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(file.length())
                connection.setRequestProperty("Content-Type", "application/pdf")
                file.inputStream().use { input -> connection.outputStream.use { input.copyTo(it) } }
                check(connection.responseCode in 200..299) { "Your PDF could not be uploaded. Try again." }
            } finally { connection.disconnect() }
        }
        return signed.getString("fileId")
    }

    suspend fun downloadUrl(fileId: String): String {
        requireEnabled()
        if (BuildConfig.STORAGE_PROVIDER == "supabase" || supabaseReference.matches(fileId)) {
            return edge.request("crest-storage", JSONObject().put("action", "read-file").put("fileId", fileId)).getString("url")
        }
        return AppwriteClient.getDownloadUrl(StorageConfig.BUCKET_ID, fileId)
    }

    suspend fun download(fileId: String): ByteArray {
        requireEnabled()
        if (BuildConfig.STORAGE_PROVIDER == "appwrite" && !supabaseReference.matches(fileId)) {
            return legacy.getFileDownload(StorageConfig.BUCKET_ID, fileId)
        }
        val url = downloadUrl(fileId)
        return withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15000
                connection.readTimeout = 60000
                check(connection.responseCode in 200..299) { "The PDF could not be downloaded." }
                connection.inputStream.use { it.readBytes() }
            } finally { connection.disconnect() }
        }
    }

    suspend fun delete(fileId: String) {
        requireEnabled()
        if (BuildConfig.STORAGE_PROVIDER == "supabase" || supabaseReference.matches(fileId)) {
            edge.request("crest-storage", JSONObject().put("action", "delete-file").put("fileId", fileId))
        }
        // Appwrite originals are retained throughout migration, including when
        // existing repository flows request removal of a file reference.
    }
}
