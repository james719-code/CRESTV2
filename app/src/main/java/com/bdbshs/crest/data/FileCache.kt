package com.bdbshs.crest.data

import android.content.Context
import java.io.File
import java.io.IOException

/**
 * A simple file caching utility to store and retrieve downloaded files
 * in the app's internal cache directory.
 */
object FileCache {

    private const val PDF_CACHE_DIR = "pdf_cache"

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
        val file = File(getCacheDir(context), fileId)
        return if (file.exists()) {
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
     * Checks if a file exists in the cache.
     * @param context The application context.
     * @param fileId The unique identifier for the file.
     * @return True if the file is cached, false otherwise.
     */
    fun isFileCached(context: Context, fileId: String): Boolean {
        return File(getCacheDir(context), fileId).exists()
    }
}