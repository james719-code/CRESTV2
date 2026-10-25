package com.bdbshs.crest.data

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Unit tests for FileCache utility.
 * Tests file caching operations including save, retrieve, and existence checks.
 */
class FileCacheTest {

    private lateinit var mockContext: Context
    private lateinit var tempCacheDir: File

    @Before
    fun setup() {
        // Create a real temporary directory for testing
        tempCacheDir = File(System.getProperty("java.io.tmpdir"), "test_cache_${System.currentTimeMillis()}")
        tempCacheDir.mkdirs()
        
        val pdfCacheDir = File(tempCacheDir, "pdf_cache")
        pdfCacheDir.mkdirs()

        mockContext = mockk {
            every { cacheDir } returns tempCacheDir
        }
    }

    @After
    fun teardown() {
        // Clean up temporary directory
        tempCacheDir.deleteRecursively()
    }

    @Test
    fun `saveFile stores data correctly`() {
        // Given
        val fileId = "test-file-id"
        val testData = "Hello, World!".toByteArray()

        // When
        FileCache.saveFile(mockContext, fileId, testData)

        // Then
        val savedFile = File(File(tempCacheDir, "pdf_cache"), fileId)
        assertTrue("File should exist after saving", savedFile.exists())
        assertArrayEquals("File content should match", testData, savedFile.readBytes())
    }

    @Test
    fun `getFile retrieves stored data correctly`() {
        // Given
        val fileId = "test-file-id"
        val testData = "Test Content".toByteArray()
        val pdfCacheDir = File(tempCacheDir, "pdf_cache")
        pdfCacheDir.mkdirs()
        File(pdfCacheDir, fileId).writeBytes(testData)

        // When
        val retrievedData = FileCache.getFile(mockContext, fileId)

        // Then
        assertArrayEquals("Retrieved data should match original", testData, retrievedData)
    }

    @Test
    fun `getFile returns null for non-existent file`() {
        // Given
        val fileId = "non-existent-file"

        // When
        val result = FileCache.getFile(mockContext, fileId)

        // Then
        assertNull("Should return null for non-existent file", result)
    }

    @Test
    fun `isFileCached returns true for cached file`() {
        // Given
        val fileId = "cached-file"
        val pdfCacheDir = File(tempCacheDir, "pdf_cache")
        pdfCacheDir.mkdirs()
        File(pdfCacheDir, fileId).writeBytes("data".toByteArray())

        // When
        val isCached = FileCache.isFileCached(mockContext, fileId)

        // Then
        assertTrue("Should return true for cached file", isCached)
    }

    @Test
    fun `isFileCached returns false for non-cached file`() {
        // Given
        val fileId = "not-cached-file"

        // When
        val isCached = FileCache.isFileCached(mockContext, fileId)

        // Then
        assertFalse("Should return false for non-cached file", isCached)
    }

    @Test
    fun `saveFile handles empty data`() {
        // Given
        val fileId = "empty-file"
        val emptyData = ByteArray(0)

        // When
        FileCache.saveFile(mockContext, fileId, emptyData)

        // Then
        val savedFile = File(File(tempCacheDir, "pdf_cache"), fileId)
        assertTrue("Empty file should be created", savedFile.exists())
        assertTrue("File should be empty", savedFile.length() == 0L)
    }

    @Test
    fun `saveFile overwrites existing file`() {
        // Given
        val fileId = "overwrite-file"
        val originalData = "Original Content".toByteArray()
        val newData = "New Content".toByteArray()
        
        // Create initial file
        FileCache.saveFile(mockContext, fileId, originalData)

        // When
        FileCache.saveFile(mockContext, fileId, newData)

        // Then
        val retrievedData = FileCache.getFile(mockContext, fileId)
        assertArrayEquals("File should contain new data", newData, retrievedData)
    }

    @Test
    fun `saveFile handles special characters in fileId`() {
        // Given
        val fileId = "file_with-special.chars123"
        val testData = "Test Data".toByteArray()

        // When
        FileCache.saveFile(mockContext, fileId, testData)

        // Then
        assertTrue(FileCache.isFileCached(mockContext, fileId))
    }
}
