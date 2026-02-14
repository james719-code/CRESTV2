package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.data.StorageConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await

object GroupRepository {

    private val firestore = FirebaseClient.firestore
    private val storage = AppwriteClient.storage

    fun observeGroups(
        onEvent: (QuerySnapshot?, Exception?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.GROUPS)
            .orderBy("group_name", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
    }

    fun observeGroupById(
        groupId: String,
        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.GROUPS)
            .document(groupId)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
    }

    suspend fun fetchStudentNamesByIds(uids: List<String>): List<String> {
        if (uids.isEmpty()) return emptyList()

        return uids.chunked(10).flatMap { chunk ->
            val studentDocs = firestore.collection(FirestorePaths.STUDENTS)
                .whereIn("__name__", chunk)
                .get()
                .await()
            studentDocs.documents.mapNotNull { it.getString("name") }
        }
    }

    suspend fun approveSubmissionWithResearch(
        groupId: String,
        researchTypeLowercase: String,
        researchTitle: String,
        memberNames: List<String>,
        strand: String,
        fileLink: String
    ) {
        val newResearchData = mapOf(
            "title" to researchTitle,
            "members" to memberNames,
            "strand" to strand,
            "downloads" to 0,
            "unfinished" to false,
            "file_link" to fileLink,
            "createdAt" to System.currentTimeMillis()
        )

        val batch = firestore.batch()
        val newResearchRef = firestore.collection("${FirestorePaths.RESEARCHES_BASE}/$researchTypeLowercase").document()
        batch.set(newResearchRef, newResearchData)

        val groupRef = firestore.collection(FirestorePaths.GROUPS).document(groupId)
        batch.update(groupRef, mapOf("accepted_research" to true, "uploaded" to false))

        batch.commit().await()
    }

    suspend fun approveSubmission(groupId: String) {
        val updates = mapOf("accepted_research" to true, "uploaded" to false)
        firestore.collection(FirestorePaths.GROUPS).document(groupId).update(updates).await()
    }

    suspend fun denySubmission(groupId: String, fileLink: String?, comment: String) {
        if (!fileLink.isNullOrBlank()) {
            storage.deleteFile(StorageConfig.BUCKET_ID, fileLink)
        }

        val updates = mapOf(
            "accepted_research" to false,
            "uploaded" to false,
            "file_link" to "",
            "research_title" to "",
            "research_type" to "",
            "comments" to FieldValue.arrayUnion(comment)
        )
        firestore.collection(FirestorePaths.GROUPS).document(groupId).update(updates).await()
    }
}
