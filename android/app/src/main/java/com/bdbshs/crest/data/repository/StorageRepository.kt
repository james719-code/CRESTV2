package com.bdbshs.crest.data.repository

import android.content.Context
import com.bdbshs.crest.data.CachedFileInfo
import com.bdbshs.crest.data.FileCache
import com.bdbshs.crest.data.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

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

@Singleton
class StorageRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun loadStorageSnapshot(context: Context): StorageSnapshot {
        val pdfSize = FileCache.getTotalCacheSize(context)
        val totalSize = FileCache.getTotalAppCacheSize(context)
        val cachedFiles = FileCache.getAllCachedFiles(context)
        val cachedResearches = mapCachedFilesToResearches(cachedFiles)

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
        cachedFiles: List<CachedFileInfo>
    ): List<CachedResearchRecord> {
        if (cachedFiles.isEmpty()) return emptyList()

        val fileIds = cachedFiles.map { it.fileId }.filter { it.isNotBlank() }.distinct()
        val researchMap = mutableMapOf<String, Pair<String, String>>()

        try {
            coroutineScope {
                fileIds.chunked(FIRESTORE_IN_QUERY_LIMIT).flatMap { chunk ->
                    listOf(
                        async {
                            firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
                                .whereIn("file_link", chunk)
                                .get()
                                .await()
                        },
                        async {
                            firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES)
                                .whereIn("file_link", chunk)
                                .get()
                                .await()
                        }
                    )
                }.awaitAll()
            }.flatMap { it.documents }.forEach { doc ->
                val fileLink = doc.getString("file_link").orEmpty()
                if (fileLink.isNotBlank()) {
                    researchMap[fileLink] =
                        Pair(
                            doc.getString("title") ?: "Unknown Research",
                            doc.getString("strand") ?: ""
                        )
                }
            }
        } catch (_: Exception) {
        }

        return cachedFiles.map { cachedFile ->
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

    private companion object {
        const val FIRESTORE_IN_QUERY_LIMIT = 10
    }
}
