package com.bdbshs.crest.di

import com.bdbshs.crest.data.AppwriteClient
import com.bdbshs.crest.data.FirebaseClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.appwrite.services.Storage
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseClient.auth

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseClient.firestore

    @Provides
    @Singleton
    fun provideAppwriteStorage(): Storage = AppwriteClient.storage
}
