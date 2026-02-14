package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

enum class UserRole {
    STUDENT,
    TEACHER
}

data class UserStatus(
    val role: UserRole,
    val hasPermission: Boolean
)

data class UserRoleLocation(
    val role: UserRole,
    val collectionPath: String,
    val permissionField: String
)

object UserRepository {

    private val firestore = FirebaseClient.firestore

    suspend fun getUserStatus(uid: String): UserStatus? {
        val studentDoc = firestore.collection(FirestorePaths.STUDENTS).document(uid).get().await()
        if (studentDoc.exists()) {
            return UserStatus(UserRole.STUDENT, studentDoc.getBoolean("accepted") ?: false)
        }

        val teacherDoc = firestore.collection(FirestorePaths.TEACHERS).document(uid).get().await()
        if (teacherDoc.exists()) {
            return UserStatus(UserRole.TEACHER, teacherDoc.getBoolean("access") ?: false)
        }

        return null
    }

    suspend fun detectUserRoleLocation(uid: String): UserRoleLocation? {
        val studentDoc = try {
            firestore.collection(FirestorePaths.STUDENTS).document(uid).get().await()
        } catch (_: FirebaseFirestoreException) {
            null
        }

        if (studentDoc?.exists() == true) {
            return UserRoleLocation(UserRole.STUDENT, FirestorePaths.STUDENTS, "accepted")
        }

        val teacherDoc = try {
            firestore.collection(FirestorePaths.TEACHERS).document(uid).get().await()
        } catch (_: FirebaseFirestoreException) {
            null
        }

        if (teacherDoc?.exists() == true) {
            return UserRoleLocation(UserRole.TEACHER, FirestorePaths.TEACHERS, "access")
        }

        return null
    }

    fun observeUserStatus(
        uid: String,
        roleLocation: UserRoleLocation,
        onEvent: (Boolean?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(roleLocation.collectionPath)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onEvent(null, error)
                    return@addSnapshotListener
                }
                onEvent(snapshot?.getBoolean(roleLocation.permissionField) ?: false, null)
            }
    }
}
