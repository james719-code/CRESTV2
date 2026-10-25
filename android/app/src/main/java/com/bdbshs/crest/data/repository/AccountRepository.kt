package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

sealed class AccountRecord {
    abstract val uid: String
    abstract val name: String

    data class Student(
        override val uid: String = "",
        override val name: String = "",
        val lrn: Long = 0,
        val strand: String = "",
        val gender: String = "",
        val accepted: Boolean = false,
        val researchAccepted: Boolean = false
    ) : AccountRecord()

    data class Teacher(
        override val uid: String = "",
        override val name: String = "",
        val email: String = "",
        val access: Boolean = false,
        val uploadCount: Int = 0
    ) : AccountRecord()
}

@Singleton
class AccountRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    suspend fun fetchAllAccounts(): List<AccountRecord> = coroutineScope {
        val studentsTask = async { firestore.collection(FirestorePaths.STUDENTS).get().await() }
        val teachersTask = async { firestore.collection(FirestorePaths.TEACHERS).get().await() }

        val students = studentsTask.await().documents.mapNotNull { doc ->
            AccountRecord.Student(
                uid = doc.id,
                name = doc.getString("name").orEmpty(),
                lrn = doc.getLong("lrn") ?: 0L,
                strand = doc.getString("strand").orEmpty(),
                gender = doc.getString("gender").orEmpty(),
                accepted = doc.getBoolean("accepted") ?: false,
                researchAccepted = doc.getBoolean("research_accepted") ?: false
            )
        }

        val teachers = teachersTask.await().documents.mapNotNull { doc ->
            AccountRecord.Teacher(
                uid = doc.id,
                name = doc.getString("name").orEmpty(),
                email = doc.getString("email").orEmpty(),
                access = doc.getBoolean("access") ?: false,
                uploadCount = (doc.getLong("upload_count") ?: 0L).toInt()
            )
        }

        students + teachers
    }

    suspend fun approveAccount(uid: String, role: UserRole) {
        val (collectionPath, fieldToUpdate) = when (role) {
            UserRole.STUDENT -> FirestorePaths.STUDENTS to "accepted"
            UserRole.TEACHER -> FirestorePaths.TEACHERS to "access"
        }

        firestore.collection(collectionPath)
            .document(uid)
            .update(fieldToUpdate, true)
            .await()
    }

    suspend fun denyAccount(uid: String, role: UserRole) {
        val collectionPath = when (role) {
            UserRole.STUDENT -> FirestorePaths.STUDENTS
            UserRole.TEACHER -> FirestorePaths.TEACHERS
        }

        firestore.collection(collectionPath)
            .document(uid)
            .delete()
            .await()
    }
}
