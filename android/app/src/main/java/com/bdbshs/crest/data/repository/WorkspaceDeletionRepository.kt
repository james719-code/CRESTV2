package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkspaceDeletionRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) {
    suspend fun deleteDeclinedStudent(uid: String) {
        val teacherId = auth.currentUser?.uid ?: error("Sign in again before making changes.")
        // group_member is deliberately unindexed; use the teacher's collection access.
        val memberships = firestore.collection(FirestorePaths.GROUPS).get(Source.SERVER).await()
            .documents.filter { (it.get("group_member") as? List<*>)?.contains(uid) == true }
        firestore.runTransaction { transaction ->
            val teacher = transaction.get(firestore.collection(FirestorePaths.TEACHERS).document(teacherId))
            check(teacher.getBoolean("access") == true) { "An approved teacher account is required." }
            val accountRef = firestore.collection(FirestorePaths.STUDENTS).document(uid)
            val account = transaction.get(accountRef)
            check(account.exists()) { "This student account no longer exists." }
            check(account.getBoolean("accepted") != true && account.getBoolean("declined") == true) {
                "Only declined student accounts can be deleted. Check the current status."
            }
            val groupIds = (memberships.map { it.id } +
                listOfNotNull(account.getString("groupId"), account.getString("group_id")))
                .filter { it.isNotBlank() }.distinct()
            val groups = groupIds.map { transaction.get(firestore.collection(FirestorePaths.GROUPS).document(it)) }
            groups.filter { it.exists() }.forEach { group ->
                val members = (group.get("group_member") as? List<*>)?.filterIsInstance<String>().orEmpty()
                if (uid in members) transaction.update(group.reference, "group_member", members.filterNot { it == uid })
            }
            transaction.delete(accountRef)
        }.await()
    }

    suspend fun deleteGroup(groupId: String) {
        val teacherId = auth.currentUser?.uid ?: error("Sign in again before making changes.")
        val linkedIds = findLinkedStudentIds(groupId)
        firestore.runTransaction { transaction ->
            val teacher = transaction.get(firestore.collection(FirestorePaths.TEACHERS).document(teacherId))
            check(teacher.getBoolean("access") == true) { "An approved teacher account is required." }
            val groupRef = firestore.collection(FirestorePaths.GROUPS).document(groupId)
            val group = transaction.get(groupRef)
            check(group.exists()) { "This group no longer exists." }
            val memberIds = (group.get("group_member") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val members = (memberIds + linkedIds).distinct().map {
                transaction.get(firestore.collection(FirestorePaths.STUDENTS).document(it))
            }
            members.filter { it.exists() }.forEach { member ->
                val updates = listOf("groupId", "group_id")
                    .filter { member.getString(it) == groupId }.associateWith { "" }
                if (updates.isNotEmpty()) transaction.update(member.reference, updates)
            }
            transaction.delete(groupRef)
            // Published research and shared PDFs remain in the school library.
        }.await()
    }

    internal suspend fun findLinkedStudentIds(groupId: String): List<String> {
        // Legacy group_id is deliberately unindexed. Read the teacher-accessible
        // collection from the server so cleanup includes both field spellings.
        return firestore.collection(FirestorePaths.STUDENTS).get(Source.SERVER).await()
            .documents.filter {
                it.getString("groupId") == groupId || it.getString("group_id") == groupId
            }.map { it.id }
    }
}
