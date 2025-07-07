package com.bdbshs.crest.data

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Storage

object AppwriteClient {
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1" // Your Appwrite API Endpoint
    private const val PROJECT_ID = "686a25c60006e47cfbea"           // Your Appwrite Project ID

    // Lazy initialization for the client
    private lateinit var client: Client

    // Lazy initialization for services
    lateinit var storage: Storage
        private set

    fun initialize(context: Context) {
        if (::client.isInitialized) return // Prevent re-initialization

        client = Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)

        storage = Storage(client)
    }
}