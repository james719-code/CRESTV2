package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.data.StorageConfig
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import io.appwrite.services.Storage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResearchRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: Storage
) {

    fun observeQualitative(
        onEvent: (QuerySnapshot?, Exception?) -> Unit
    ): ListenerRegistration {
        return firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES)
            .addSnapshotListener { snapshot, error -> onEvent(snapshot, error) }
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
        storage.deleteFile(StorageConfig.BUCKET_ID, fileId)
    }

    suspend fun incrementDownloadCount(researchId: String) {
        val qualitativeRef = firestore.collection(FirestorePaths.QUALITATIVE_RESEARCHES).document(researchId)
        val refToUpdate = if (qualitativeRef.get().await().exists()) {
            qualitativeRef
        } else {
            firestore.collection(FirestorePaths.QUANTITATIVE_RESEARCHES).document(researchId)
        }

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(refToUpdate)
            val newCount = (snapshot.getLong("downloads") ?: 0L) + 1
            transaction.update(refToUpdate, "downloads", newCount)
            null
        }.await()
    }
}
