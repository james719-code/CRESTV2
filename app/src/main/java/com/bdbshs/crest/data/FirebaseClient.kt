package com.bdbshs.crest.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
object FirebaseClient {

    val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    val firestore: FirebaseFirestore by lazy {
        // Initialize Firestore
        val instance = Firebase.firestore

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024 * 1024)
                    .build()
            )
            .build()
        instance.firestoreSettings = settings
        instance
    }

}
