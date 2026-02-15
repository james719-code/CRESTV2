package com.bdbshs.crest.data.repository

import android.content.Context
import android.net.Uri
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.StorageConfig
import com.google.firebase.firestore.FirebaseFirestore
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ResearchUploadInput(
    val title: String,
    val members: List<String>,
    val strandName: String,
    val researchType: String,
    val selectedFileUri: Uri
)

data class GroupResearchUploadInput(
    val groupId: String,
    val title: String,
    val researchType: String,
    val selectedFileUri: Uri
)

@Singleton
class UploadRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: Storage
) {

    suspend fun uploadResearch(context: Context, input: ResearchUploadInput) {
        val tempFile = createTempFileFromUri(context, input.selectedFileUri)
            ?: throw IllegalStateException("Failed to read the selected file.")

        val uploadedFileId = try {
            val inputFile = InputFile.fromFile(file = tempFile)
            storage.createFile(StorageConfig.BUCKET_ID, ID.unique(), inputFile).id
        } finally {
            tempFile.delete()
        }

        val researchData = hashMapOf(
            "title" to input.title,
            "members" to input.members,
            "strand" to input.strandName,
            "type" to input.researchType.uppercase(),
            "unfinished" to false,
            "views" to 0,
            "file_link" to uploadedFileId,
            "createdAt" to System.currentTimeMillis()
        )

        val collectionPath = "${FirestorePaths.RESEARCHES_BASE}/${input.researchType.lowercase()}"
        firestore.collection(collectionPath).add(researchData).await()
    }

    suspend fun submitGroupResearchForReview(context: Context, input: GroupResearchUploadInput) {
        val tempFile = createTempFileFromUri(context, input.selectedFileUri)
            ?: throw IllegalStateException("Failed to prepare file for upload.")

        val uploadedFileId = try {
            val inputFile = InputFile.fromFile(file = tempFile)
            storage.createFile(StorageConfig.BUCKET_ID, ID.unique(), inputFile).id
        } finally {
            tempFile.delete()
        }

        val groupUpdates = mapOf(
            "file_link" to uploadedFileId,
            "research_title" to input.title,
            "research_type" to input.researchType,
            "uploaded" to true
        )

        firestore.collection(FirestorePaths.GROUPS).document(input.groupId).update(groupUpdates).await()
    }

    suspend fun getStudentGroupId(uid: String): String? {
        val studentDoc = firestore.collection(FirestorePaths.STUDENTS).document(uid).get().await()
        return studentDoc.getString("group_id") ?: studentDoc.getString("groupId")
    }

    private fun createTempFileFromUri(context: Context, uri: Uri): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("upload_", ".pdf", context.cacheDir)
            val fileOutputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                fileOutputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (_: Exception) {
            null
        }
    }
}
