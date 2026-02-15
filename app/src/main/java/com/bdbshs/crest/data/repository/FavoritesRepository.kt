package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.bdbshs.crest.ui.viewmodels.ResearchType
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {

    fun observeFavoriteIds(
        uid: String,
        onEvent: (Set<String>, Exception?) -> Unit
    ): ListenerRegistration {
        return firestore
            .collection(FirestorePaths.USER_FAVORITES)
            .document(uid)
            .collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onEvent(emptySet(), error)
                } else {
                    val ids = snapshot?.documents
                        ?.mapNotNull { it.getString("researchId") ?: it.id }
                        ?.toSet()
                        ?: emptySet()
                    onEvent(ids, null)
                }
            }
    }

    suspend fun setFavorite(
        uid: String,
        researchId: String,
        researchType: ResearchType,
        isFavorite: Boolean
    ) {
        val docRef = firestore
            .collection(FirestorePaths.USER_FAVORITES)
            .document(uid)
            .collection("items")
            .document(researchId)

        if (isFavorite) {
            docRef.set(
                mapOf(
                    "researchId" to researchId,
                    "type" to researchType.name,
                    "updatedAt" to System.currentTimeMillis()
                )
            ).await()
        } else {
            docRef.delete().await()
        }
    }
}
