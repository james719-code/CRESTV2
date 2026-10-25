package com.bdbshs.crest.data

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * Data class representing a cached file with metadata.
 */
data class CachedFileInfo(
    val fileId: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val name: String = fileId
)

/**
 * A file caching utility to store, retrieve, and manage downloaded files
 * in the app's internal cache directory.
 *
 * Enhanced with storage tracking and management capabilities.
 */
object FileCache {

    private const val PDF_CACHE_DIR = "pdf_cache"
    private const val MAX_PDF_CACHE_BYTES = 200L * 1024 * 1024 // 200 MB

    private fun getCacheDir(context: Context): File {
        return File(context.cacheDir, PDF_CACHE_DIR).apply { mkdirs() }
    }

    /**
     * Saves a byte array to a uniquely named file in the cache.
     * @param context The application context.
     * @param fileId The unique identifier for the file (e.g., Appwrite file ID).
     * @param data The file content as a ByteArray.
     */
    fun saveFile(context: Context, fileId: String, data: ByteArray) {
        try {
            val file = File(getCacheDir(context), fileId)
            file.writeBytes(data)
            file.setLastModified(System.currentTimeMillis())
            enforceCacheLimit(context)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Retrieves a file from the cache as a ByteArray.
     * @param context The application context.
     * @param fileId The unique identifier for the file.
     * @return The file content as a ByteArray, or null if it doesn't exist.
     */
    fun getFile(context: Context, fileId: String): ByteArray? {
        val file = getCachedFile(context, fileId)
        return if (file != null) {
            try {
                file.readBytes()
            } catch (e: IOException) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }

    /**
     * Retrieves the cached file handle if it exists.
     * @param context The application context.
     * @param fileId The unique identifier for the file.
     * @return The cached File, or null if it doesn't exist.
     */
    fun getCachedFile(context: Context, fileId: String): File? {
        val file = File(getCacheDir(context), fileId)
        return if (file.exists()) {
            file.setLastModified(System.currentTimeMillis())
            file
        } else {
            null
        }
    }

    /**
     * Evicts least-recently-used cached PDFs until total cache size is within the limit.
     */
    private fun enforceCacheLimit(context: Context) {
        val cacheDir = getCacheDir(context)
        val files = cacheDir.listFiles()?.toMutableList() ?: return

        var totalSize = files.sumOf { it.length() }
        if (totalSize <= MAX_PDF_CACHE_BYTES) return

        files.sortBy { it.lastModified() } // oldest first
        for (file in files) {
            if (totalSize <= MAX_PDF_CACHE_BYTES) break
            val size = file.length()
            if (file.delete()) {
                totalSize -= size
            }
        }
    }

    /**
     * Checks if a file exists in the cache.
     * @param context The application context.
     * @param fileId The unique identifier for the file.
     * @return True if the file is cached, false otherwise.
     */
    fun isFileCached(context: Context, fileId: String): Boolean {
        return File(getCacheDir(context), fileId).exists()
    }

    // ==================== STORAGE MANAGEMENT ====================

    /**
     * Gets the total size of all cached PDF files in bytes.
     */
    fun getTotalCacheSize(context: Context): Long {
        val cacheDir = getCacheDir(context)
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    /**
     * Gets the number of cached files.
     */
    fun getCachedFileCount(context: Context): Int {
        return getCacheDir(context).listFiles()?.size ?: 0
    }

    /**
     * Gets information about all cached files.
     * @return List of CachedFileInfo objects sorted by size (largest first).
     */
    fun getAllCachedFiles(context: Context): List<CachedFileInfo> {
        val cacheDir = getCacheDir(context)
        return cacheDir.listFiles()?.map { file ->
            CachedFileInfo(
                fileId = file.name,
                sizeBytes = file.length(),
                lastModified = file.lastModified()
            )
        }?.sortedByDescending { it.sizeBytes } ?: emptyList()
    }

    /**
     * Gets the size of a specific cached file.
     * @return File size in bytes, or 0 if file doesn't exist.
     */
    fun getFileSize(context: Context, fileId: String): Long {
        val file = File(getCacheDir(context), fileId)
        return if (file.exists()) file.length() else 0L
    }

    /**
     * Deletes a specific cached file.
     * @param context The application context.
     * @param fileId The unique identifier for the file to delete.
     * @return True if the file was deleted successfully, false otherwise.
     */
    fun deleteFile(context: Context, fileId: String): Boolean {
        val file = File(getCacheDir(context), fileId)
        return if (file.exists()) {
            try {
                file.delete()
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        } else {
            true // File doesn't exist, consider it deleted
        }
    }

    /**
     * Deletes multiple cached files.
     * @param context The application context.
     * @param fileIds List of file IDs to delete.
     * @return Number of files successfully deleted.
     */
    fun deleteFiles(context: Context, fileIds: List<String>): Int {
        var deletedCount = 0
        fileIds.forEach { fileId ->
            if (deleteFile(context, fileId)) {
                deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * Clears all cached PDF files.
     * @param context The application context.
     * @return Number of files deleted.
     */
    fun clearAllCache(context: Context): Int {
        val cacheDir = getCacheDir(context)
        var deletedCount = 0
        cacheDir.listFiles()?.forEach { file ->
            if (file.delete()) deletedCount++
        }
        return deletedCount
    }

    /**
     * Gets the app's total cache directory size (including system cache).
     */
    fun getTotalAppCacheSize(context: Context): Long {
        return calculateDirSize(context.cacheDir)
    }

    /**
     * Clears all app cache (including system cache).
     */
    fun clearAllAppCache(context: Context): Boolean {
        return try {
            deleteDir(context.cacheDir)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun calculateDirSize(dir: File): Long {
        var size = 0L
        dir.listFiles()?.forEach { file ->
            size += if (file.isDirectory) {
                calculateDirSize(file)
            } else {
                file.length()
            }
        }
        return size
    }

    private fun deleteDir(dir: File): Boolean {
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                deleteDir(child)
            }
        }
        return dir.delete()
    }

    // ==================== UTILITY FUNCTIONS ====================

    /**
     * Formats bytes into a human-readable string (KB, MB, GB).
     */
    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_073_741_824 -> String.format("%.2f GB", bytes / 1_073_741_824.0)
            bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
            bytes >= 1024 -> String.format("%.2f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}