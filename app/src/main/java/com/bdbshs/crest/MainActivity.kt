package com.bdbshs.crest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.navigation.CrestApp
import com.bdbshs.crest.ui.theme.CRESTTheme
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppwriteClient.initialize(applicationContext)
        val firestore = Firebase.firestore
        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                PersistentCacheSettings.newBuilder()
                    .setSizeBytes(100L * 1024 * 1024) // e.g. 100MB
                    .build()
            )
            .build()
        firestore.firestoreSettings = settings
        enableEdgeToEdge()
        setContent {
            CRESTTheme {
                // Call our central navigation composable.
                // It will handle showing the LoginScreen first.
                CrestApp()
            }
        }
    }
}