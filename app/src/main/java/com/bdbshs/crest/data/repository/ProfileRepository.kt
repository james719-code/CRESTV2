package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.FirebaseClient
import kotlinx.coroutines.tasks.await

data class StudentProfileInput(
    val uid: String,
    val name: String,
    val lrn: Long,
    val strand: String,
    val gender: String,
    val groupId: String = ""
)

data class TeacherProfileInput(
    val uid: String,
    val name: String,
    val email: String
)

object ProfileRepository {

    private val firestore = FirebaseClient.firestore

    suspend fun saveStudentProfile(input: StudentProfileInput) {
        val data = mapOf(
            "name" to input.name,
            "lrn" to input.lrn,
            "strand" to input.strand,
            "gender" to input.gender,
            "group_id" to input.groupId,
            "accepted" to false,
            "research_accepted" to false,
            "uid" to input.uid
        )

        firestore.collection(FirestorePaths.STUDENTS)
            .document(input.uid)
            .set(data)
            .await()
    }

    suspend fun saveTeacherProfile(input: TeacherProfileInput) {
        val data = mapOf(
            "name" to input.name,
            "email" to input.email,
            "access" to false,
            "upload_count" to 0,
            "uid" to input.uid
        )

        firestore.collection(FirestorePaths.TEACHERS)
            .document(input.uid)
            .set(data)
            .await()
    }
}
