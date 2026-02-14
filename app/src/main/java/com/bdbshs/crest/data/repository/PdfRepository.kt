package com.bdbshs.crest.data.repository

import android.content.Context
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.StorageConfig
import java.io.File

object PdfRepository {

    fun getCachedPdfFile(context: Context, fileId: String): File? {
        return FileCache.getCachedFile(context, fileId)
    }

    suspend fun getOrDownloadPdfFile(
        context: Context,
        fileId: String,
        isOnline: Boolean
    ): File {
        val cached = FileCache.getCachedFile(context, fileId)
        if (cached != null) return cached

        if (!isOnline) {
            throw IllegalStateException("File not cached. An internet connection is required to download it.")
        }

        val bytes = AppwriteClient.storage.getFileDownload(
            bucketId = StorageConfig.BUCKET_ID,
            fileId = fileId
        )
        FileCache.saveFile(context, fileId, bytes)

        return FileCache.getCachedFile(context, fileId)
            ?: throw IllegalStateException("Could not read downloaded PDF from cache.")
    }
}
