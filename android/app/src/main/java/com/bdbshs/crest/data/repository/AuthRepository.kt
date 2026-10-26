package com.bdbshs.crest.data.repository

import com.bdbshs.crest.notifications.PushNotifications

import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val pushNotifications: PushNotifications
) {

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserUid(): String? = auth.currentUser?.uid

    suspend fun signInWithCredential(credential: AuthCredential): AuthResult {
        return auth.signInWithCredential(credential).await()
    }

    suspend fun signOut() {
        pushNotifications.disable()
        auth.signOut()
    }

    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
