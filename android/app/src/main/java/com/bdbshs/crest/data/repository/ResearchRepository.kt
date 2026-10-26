package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.CrestStorage
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResearchRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: CrestStorage
) {

    fun observeQualitative(
        onEvent: (QuerySnapshot?, Exception?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
    }

    suspend fun fetchQualitativeResearches(
        limit: Long = 10L,
        startAfter: DocumentSnapshot? = null
    ): QuerySnapshot {
        var query = firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            
        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }
        
        return query.get().await()
    }

    fun observeQualitativeById(
        researchId: String,
        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
            .document(researchId)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
    }

    fun observeQuantitative(
        onEvent: (QuerySnapshot?, Exception?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
    }

    suspend fun fetchQuantitativeResearches(
        limit: Long = 10L,
        startAfter: DocumentSnapshot? = null
    ): QuerySnapshot {
        var query = firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            
        if (startAfter != null) {
            query = query.startAfter(startAfter)
        }
        
        return query.get().await()
    }

    fun observeQuantitativeById(
        researchId: String,
        onEvent: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES)
            .document(researchId)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
    }

    suspend fun deleteResearchDocument(typeLowercase: String, researchId: String) {
        firestore.collection("${FirestorePaths.RESEARCHES_BASE}/$typeLowercase")
            .document(researchId)
            .delete()
            .await()
    }

    suspend fun deleteResearchFile(fileId: String) {
        storage.delete(fileId) // Retention-only request; no physical removal.
    }

    suspend fun incrementDownloadCount(researchId: String, typeLowercase: String) {
        val path = when (typeLowercase) {
            "qualitative" -> FirestorePaths.QUALITATIVE_RESEARCHES
            "quantitative" -> FirestorePaths.QUANTITATIVE_RESEARCHES
            else -> throw IllegalArgumentException("Unknown research type.")
        }
        firestore.collection(path).document(researchId)
            .update("downloads", FieldValue.increment(1L)).await()
    }
}
