package com.bdbshs.crest.ui.certificates

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Render and encode off the UI thread; publish only a completely written image. */
internal suspend fun exportResearchCertificate(context: Context, data: ResearchCertificateData): Boolean =
    withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var pendingUri: Uri? = null
        var legacyFile: File? = null
        val resolver = context.contentResolver
        try {
            bitmap = ResearchCertificateRenderer.render(context, data)
            val safeName = data.groupName.replace(Regex("[^\\p{L}\\p{N}_-]+"), "_").trim('_').take(72).ifEmpty { "Research" }
            val filename = "CREST_Certificate_${safeName}_${System.currentTimeMillis()}.png"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/CREST")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Could not create the certificate download")
                pendingUri = uri
                val output = resolver.openOutputStream(uri) ?: throw IOException("Could not open the certificate download")
                output.use { if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("PNG encoding failed") }
                val published = resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
                if (published != 1) throw IOException("Could not publish the certificate download")
                pendingUri = null
            } else {
                val directory = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CREST")
                if (!directory.isDirectory && !directory.mkdirs()) throw IOException("Could not create the downloads folder")
                val file = File(directory, filename)
                legacyFile = file
                file.outputStream().use { if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) throw IOException("PNG encoding failed") }
                MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf("image/png"), null)
                legacyFile = null
            }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.e("CertificateExport", "Could not save the research certificate", error)
            false
        } finally {
            bitmap?.recycle()
            pendingUri?.let { uri -> runCatching { resolver.delete(uri, null, null) } }
            legacyFile?.delete()
        }
    }
