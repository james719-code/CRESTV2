package com.bdbshs.crest.data.repository

import android.content.Context
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.FirebaseClient
import kotlinx.coroutines.tasks.await

data class CachedResearchRecord(
    val fileId: String,
    val title: String,
    val strand: String,
    val sizeBytes: Long,
    val lastModified: Long
)

data class StorageSnapshot(
    val pdfCacheSize: Long,
    val totalAppCacheSize: Long,
    val cachedResearches: List<CachedResearchRecord>
)

object StorageRepository {

    private val firestore = FirebaseClient.firestore

    suspend fun loadStorageSnapshot(context: Context): StorageSnapshot {
        val pdfSize = FileCache.getTotalCacheSize(context)
        val totalSize = FileCache.getTotalAppCacheSize(context)
        val cachedFiles = FileCache.getAllCachedFiles(context)
        val cachedResearches = mapCachedFilesToResearches(cachedFiles.map { it.fileId }, context)

        return StorageSnapshot(
            pdfCacheSize = pdfSize,
            totalAppCacheSize = totalSize,
            cachedResearches = cachedResearches
        )
    }

    suspend fun deleteSelectedFiles(context: Context, fileIds: List<String>): Int {
        return FileCache.deleteFiles(context, fileIds)
    }

    suspend fun clearPdfCache(context: Context): Int {
        return FileCache.clearAllCache(context)
    }

    suspend fun clearAllCache(context: Context): Boolean {
        return FileCache.clearAllAppCache(context)
    }

    private suspend fun mapCachedFilesToResearches(
        fileIds: List<String>,
        context: Context
    ): List<CachedResearchRecord> {
        if (fileIds.isEmpty()) return emptyList()

        val fileSet = fileIds.toSet()
        val researchMap = mutableMapOf<String, Pair<String, String>>()

        try {
            val qualDocs = firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES).get().await()
            val quantDocs = firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES).get().await()

            (qualDocs.documents + quantDocs.documents).forEach { doc ->
                val fileLink = doc.getString("file_link") ?: ""
                if (fileLink in fileSet) {
                    researchMap[fileLink] = Pair(
                        doc.getString("title") ?: "Unknown Research",
                        doc.getString("strand") ?: ""
                    )
                }
            }
        } catch (_: Exception) {
        }

        return FileCache.getAllCachedFiles(context).map { cachedFile ->
            val (title, strand) = researchMap[cachedFile.fileId] ?: Pair("Cached File", "")
            CachedResearchRecord(
                fileId = cachedFile.fileId,
                title = title,
                strand = strand,
                sizeBytes = cachedFile.sizeBytes,
                lastModified = cachedFile.lastModified
            )
        }
    }
}
