package com.bdbshs.crest.data.repository

import android.content.Context
import android.net.Uri
import com.bdbshs.crest.data.StorageConfig
import com.bdbshs.crest.data.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class DocumentRecord(
    val id: String,
    val name: String,
    val description: String,
    val fileLink: String,
    val createdAt: Long
)

data class DocumentUpdateInput(
    val documentId: String,
    val name: String,
    val description: String,
    val currentFileLink: String,
    val newFileUri: Uri?
)

@Singleton
class DocumentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: Storage
) {

    fun observeDocuments(
        onEvent: (List<DocumentRecord>?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.DOCUMENTS)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onEvent(null, error)
                    return@addSnapshotListener
                }

                val documents = snapshot?.documents?.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val description = doc.getString("description") ?: ""
                    val fileLink = doc.getString("file_link") ?: ""
                    val createdAt = doc.getLong("createdAt") ?: 0L
                    DocumentRecord(
                        id = doc.id,
                        name = name,
                        description = description,
                        fileLink = fileLink,
                        createdAt = createdAt
                    )
                } ?: emptyList()

                onEvent(documents, null)
            }
    }

    suspend fun updateDocument(context: Context, input: DocumentUpdateInput) {
        var updatedFileLink = input.currentFileLink

        if (input.newFileUri != null) {
            val tempFile = createTempFileFromUri(context, input.newFileUri)
                ?: throw IllegalStateException("Failed to prepare new file for upload.")

            updatedFileLink = try {
                val inputFile = InputFile.fromFile(file = tempFile)
                storage.createFile(StorageConfig.BUCKET_ID, ID.unique(), inputFile).id
            } finally {
                tempFile.delete()
            }

            if (input.currentFileLink.isNotBlank()) {
                storage.deleteFile(StorageConfig.BUCKET_ID, input.currentFileLink)
            }
        }

        val updates = mapOf(
            "name" to input.name,
            "description" to input.description,
            "file_link" to updatedFileLink
        )
        firestore.collection(FirestorePaths.DOCUMENTS)
            .document(input.documentId)
            .update(updates)
            .await()
    }

    suspend fun deleteDocument(documentId: String, fileLink: String) {
        firestore.collection(FirestorePaths.DOCUMENTS)
            .document(documentId)
            .delete()
            .await()

        if (fileLink.isNotBlank()) {
            storage.deleteFile(StorageConfig.BUCKET_ID, fileLink)
        }
    }

    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_doc_", "", context.cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }
}
