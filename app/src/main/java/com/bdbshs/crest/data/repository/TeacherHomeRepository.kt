package com.bdbshs.crest.data.repository

import android.content.Context
import android.net.Uri
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.data.StorageConfig
import com.google.firebase.firestore.Query
import io.appwrite.ID
import io.appwrite.models.InputFile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream

data class TeacherDashboardData(
    val teacherName: String,
    val totalResearches: String,
    val totalAccounts: String,
    val pendingResearches: String,
    val pendingAccounts: String,
    val recentResearches: List<TeacherRecentResearch>
)

data class TeacherRecentResearch(
    val id: String,
    val title: String,
    val date: Long = 0L
)

data class TeacherDocumentUploadInput(
    val name: String,
    val description: String,
    val fileUri: Uri
)

object TeacherHomeRepository {

    private val firestore = FirebaseClient.firestore
    private val storage = AppwriteClient.storage

    suspend fun fetchDashboardData(uid: String): TeacherDashboardData = coroutineScope {
        val teacherNameDeferred = async {
            firestore.collection(FirestorePaths.TEACHERS).document(uid).get().await()
                .getString("name") ?: "Teacher"
        }

        val totalResearchesDeferred = async {
            (firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES).get().await().size() +
                    firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES).get().await().size()).toString()
        }

        val totalAccountsDeferred = async {
            (firestore.collection(FirestorePaths.STUDENTS).get().await().size() +
                    firestore.collection(FirestorePaths.TEACHERS).get().await().size()).toString()
        }

        val pendingResearchesDeferred = async {
            firestore.collection(FirestorePaths.GROUPS)
                .whereEqualTo("uploaded", true)
                .whereEqualTo("accepted_research", false)
                .get()
                .await()
                .size()
                .toString()
        }

        val pendingAccountsDeferred = async {
            val pendingStudents = firestore.collection(FirestorePaths.STUDENTS)
                .whereEqualTo("accepted", false)
                .get()
                .await()
                .size()
            val pendingTeachers = firestore.collection(FirestorePaths.TEACHERS)
                .whereEqualTo("access", false)
                .get()
                .await()
                .size()
            (pendingStudents + pendingTeachers).toString()
        }

        val recentResearchesDeferred = async {
            val qualitative = firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .await()
            val quantitative = firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(3)
                .get()
                .await()

            (qualitative.documents + quantitative.documents)
                .mapNotNull { doc ->
                    val title = doc.getString("title")
                    val date = doc.getLong("createdAt") ?: 0L
                    if (title != null) TeacherRecentResearch(doc.id, title, date) else null
                }
                .sortedByDescending { it.id }
                .take(5)
        }

        TeacherDashboardData(
            teacherName = teacherNameDeferred.await(),
            totalResearches = totalResearchesDeferred.await(),
            totalAccounts = totalAccountsDeferred.await(),
            pendingResearches = pendingResearchesDeferred.await(),
            pendingAccounts = pendingAccountsDeferred.await(),
            recentResearches = recentResearchesDeferred.await()
        )
    }

    suspend fun uploadTeacherDocument(context: Context, input: TeacherDocumentUploadInput) {
        val tempFile = createTempFileFromUri(context, input.fileUri)
            ?: throw IllegalStateException("Failed to prepare file for upload.")

        val uploadedFileId = try {
            val inputFile = InputFile.fromFile(file = tempFile)
            storage.createFile(StorageConfig.BUCKET_ID, ID.unique(), inputFile).id
        } finally {
            tempFile.delete()
        }

        val documentData = mapOf(
            "name" to input.name,
            "description" to input.description,
            "file_link" to uploadedFileId,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection(FirestorePaths.DOCUMENTS).add(documentData).await()
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
