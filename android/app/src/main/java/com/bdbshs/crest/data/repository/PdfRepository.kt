package com.bdbshs.crest.data.repository

import android.content.Context
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.CrestStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PdfRepository @Inject constructor(
    private val storage: CrestStorage
) {

    fun getCachedPdfFile(context: Context, fileId: String): File? {
        return FileCache.getCachedFile(context, fileId)
    }

    suspend fun getOrDownloadPdfFile(
        context: Context,
        fileId: String,
        isOnline: Boolean
    ): File = withContext(Dispatchers.IO) {
        val cached = FileCache.getCachedFile(context, fileId)
        if (cached != null) return@withContext cached

        if (!isOnline) {
            throw IllegalStateException("File not cached. An internet connection is required to download it.")
        }

        val bytes = storage.download(fileId)
        FileCache.saveFile(context, fileId, bytes)

        FileCache.getCachedFile(context, fileId)
            ?: throw IllegalStateException("Could not read downloaded PDF from cache.")
    }
}
