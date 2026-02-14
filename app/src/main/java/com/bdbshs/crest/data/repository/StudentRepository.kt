package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.FirebaseClient
import com.bdbshs.crest.data.StorageConfig
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

data class StudentProfile(
    val name: String,
    val strand: String,
    val groupId: String
)

data class StudentGroup(
    val groupName: String,
    val strand: String,
    val fileLink: String,
    val groupMembers: List<String>,
    val uploaded: Boolean,
    val researchType: String,
    val acceptedResearch: Boolean,
    val comments: List<String>
)

data class StudentResearchSummary(
    val id: String,
    val title: String,
    val strand: String,
    val createdAt: Long
)

data class StudentHomeData(
    val student: StudentProfile?,
    val group: StudentGroup?,
    val memberNames: List<String>,
    val allResearches: List<StudentResearchSummary>
)

object StudentRepository {

    private val firestore = FirebaseClient.firestore
    private val storage = AppwriteClient.storage

    suspend fun fetchHomeData(uid: String): StudentHomeData {
        val studentDoc = firestore.collection(FirestorePaths.STUDENTS).document(uid).get().await()
        val student = if (studentDoc.exists()) {
            StudentProfile(
                name = studentDoc.getString("name").orEmpty(),
                strand = studentDoc.getString("strand").orEmpty(),
                groupId = studentDoc.getString("groupId")
                    ?: studentDoc.getString("group_id")
                    ?: ""
            )
        } else {
            null
        }

        var group: StudentGroup? = null
        var memberNames: List<String> = emptyList()

        if (student != null && student.groupId.isNotBlank()) {
            val groupDoc = firestore.collection(FirestorePaths.GROUPS).document(student.groupId).get().await()
            if (groupDoc.exists()) {
                val groupMembers = groupDoc.get("group_member") as? List<*>
                val groupMemberIds = groupMembers?.mapNotNull { it as? String }.orEmpty()

                group = StudentGroup(
                    groupName = groupDoc.getString("group_name").orEmpty(),
                    strand = groupDoc.getString("strand").orEmpty(),
                    fileLink = groupDoc.getString("file_link").orEmpty(),
                    groupMembers = groupMemberIds,
                    uploaded = groupDoc.getBoolean("uploaded") ?: false,
                    researchType = groupDoc.getString("research_type").orEmpty(),
                    acceptedResearch = groupDoc.getBoolean("accepted_research") ?: false,
                    comments = (groupDoc.get("comments") as? List<*>)?.mapNotNull { it as? String }.orEmpty()
                )

                if (groupMemberIds.isNotEmpty()) {
                    memberNames = groupMemberIds.chunked(10).flatMap { chunk ->
                        val docs = firestore.collection(FirestorePaths.STUDENTS)
                            .whereIn("__name__", chunk)
                            .get()
                            .await()
                        docs.documents.mapNotNull { it.getString("name") }
                    }
                }
            }
        }

        val qualitativeDocs = firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES).get().await()
        val quantitativeDocs = firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES).get().await()

        val allResearches = (qualitativeDocs.documents + quantitativeDocs.documents).mapNotNull { doc ->
            val title = doc.getString("title")
            val strand = doc.getString("strand")
            val createdAt = when (val rawDate = doc.get("createdAt")) {
                is Timestamp -> rawDate.toDate().time
                is Long -> rawDate
                else -> 0L
            }

            if (title != null && strand != null) {
                StudentResearchSummary(
                    id = doc.id,
                    title = title,
                    strand = strand,
                    createdAt = createdAt
                )
            } else {
                null
            }
        }

        return StudentHomeData(
            student = student,
            group = group,
            memberNames = memberNames,
            allResearches = allResearches
        )
    }

    suspend fun leaveGroup(uid: String, groupId: String, memberCount: Int) {
        val groupRef = firestore.collection(FirestorePaths.GROUPS).document(groupId)
        val studentRef = firestore.collection(FirestorePaths.STUDENTS).document(uid)

        if (memberCount <= 1) {
            firestore.batch().apply {
                delete(groupRef)
                update(studentRef, mapOf("groupId" to "", "group_id" to ""))
            }.commit().await()
        } else {
            firestore.batch().apply {
                update(groupRef, "group_member", FieldValue.arrayRemove(uid))
                update(studentRef, mapOf("groupId" to "", "group_id" to ""))
            }.commit().await()
        }
    }

    suspend fun unsubmitResearch(groupId: String, fileId: String?) {
        if (!fileId.isNullOrBlank()) {
            storage.deleteFile(StorageConfig.BUCKET_ID, fileId)
        }

        val updates = mapOf(
            "uploaded" to false,
            "file_link" to "",
            "research_type" to ""
        )
        firestore.collection(FirestorePaths.GROUPS).document(groupId).update(updates).await()
    }

    suspend fun createGroup(uid: String, studentStrand: String, groupName: String) {
        val newGroupRef = firestore.collection(FirestorePaths.GROUPS).document()
        val newGroupId = newGroupRef.id

        val newGroup = mapOf(
            "group_name" to groupName,
            "strand" to studentStrand,
            "group_member" to listOf(uid),
            "file_link" to "",
            "uploaded" to false,
            "research_type" to "",
            "accepted_research" to false,
            "comments" to emptyList<String>()
        )

        val studentRef = firestore.collection(FirestorePaths.STUDENTS).document(uid)
        firestore.batch().apply {
            set(newGroupRef, newGroup)
            update(studentRef, mapOf("groupId" to newGroupId, "group_id" to newGroupId))
        }.commit().await()
    }

    suspend fun joinGroup(uid: String, studentStrand: String, groupId: String) {
        val groupRef = firestore.collection(FirestorePaths.GROUPS).document(groupId)
        val groupDoc = groupRef.get().await()

        if (!groupDoc.exists()) throw IllegalStateException("Group ID '$groupId' does not exist.")

        val groupStrand = groupDoc.getString("strand")
        if (groupStrand != studentStrand) {
            throw IllegalStateException("You can only join a group from your own strand ($studentStrand).")
        }

        val studentRef = firestore.collection(FirestorePaths.STUDENTS).document(uid)
        firestore.batch().apply {
            update(groupRef, "group_member", FieldValue.arrayUnion(uid))
            update(studentRef, mapOf("groupId" to groupId, "group_id" to groupId))
        }.commit().await()
    }
}
