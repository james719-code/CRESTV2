package com.bdbshs.crest.data.repository

import android.content.Context
import android.net.Uri
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.StorageConfig
import com.bdbshs.crest.data.countWithCacheFallback
import com.bdbshs.crest.data.timestampMillis
import com.bdbshs.crest.data.nonNegativeInt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import io.appwrite.ID
import io.appwrite.models.InputFile
import io.appwrite.services.Storage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class TeacherDashboardData(
    val teacherName: String,
    val totalResearches: Long,
    val totalAccounts: Long,
    val pendingResearches: Long,
    val pendingAccounts: Long,
    val recentResearches: List<TeacherRecentResearch>
)

data class TeacherRecentResearch(
    val id: String,
    val title: String,
    val date: Long = 0L,
    val strand: String = "",
    val members: List<String> = emptyList(),
    val type: String = "qualitative",
    val downloads: Int = 0,
    val unfinished: Boolean = false
)

data class TeacherDocumentUploadInput(
    val name: String,
    val description: String,
    val fileUri: Uri
)

@Singleton
class TeacherHomeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: Storage
) {

    suspend fun fetchDashboardData(uid: String): TeacherDashboardData = coroutineScope {
        val teacherNameDeferred = async {
            firestore.collection(FirestorePaths.TEACHERS).document(uid).get().await()
                .getString("name") ?: "Teacher"
        }

        val totalResearchesDeferred = async {
            countCollection(FirestorePaths.QUALITATIVE_RESEARCHES) +
                countCollection(FirestorePaths.QUANTITATIVE_RESEARCHES)
        }

        val totalAccountsDeferred = async {
            countCollection(FirestorePaths.STUDENTS) +
                countCollection(FirestorePaths.TEACHERS)
        }

        val pendingResearchesDeferred = async {
            firestore.collection(FirestorePaths.GROUPS)
                .whereEqualTo("uploaded", true)
                .whereEqualTo("accepted_research", false)
                .countWithCacheFallback()
        }

        val pendingAccountsDeferred = async {
            val pendingStudents = firestore.collection(FirestorePaths.STUDENTS)
                .whereEqualTo("accepted", false)
                .countWithCacheFallback()
            val pendingTeachers = firestore.collection(FirestorePaths.TEACHERS)
                .whereEqualTo("access", false)
                .countWithCacheFallback()
            pendingStudents + pendingTeachers
        }

        val recentResearchesDeferred = async {
            val qualitativeDeferred = async {
                firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()
            }
            val quantitativeDeferred = async {
                firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES)
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(3)
                    .get()
                    .await()
            }

            val qualitative = qualitativeDeferred.await()
            val quantitative = quantitativeDeferred.await()

            (qualitative.documents.map { it to "qualitative" } + quantitative.documents.map { it to "quantitative" })
                .mapNotNull { (doc, type) ->
                    val title = doc.getString("title")
                    val date = doc.timestampMillis("createdAt")
                    if (title != null) TeacherRecentResearch(
                        doc.id, title, date,
                        strand = doc.getString("strand").orEmpty(),
                        members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty(),
                        type = type,
                        downloads = doc.nonNegativeInt("downloads"),
                        unfinished = doc.getBoolean("unfinished") ?: false
                    ) else null
                }
                .sortedByDescending { it.date }
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

    private suspend fun countCollection(path: String): Long {
        return firestore.collection(path)
            .countWithCacheFallback()
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
