package com.bdbshs.crest.data

import android.content.Context
import io.appwrite.Client
import io.appwrite.services.Storage

object AppwriteClient {
    private const val ENDPOINT = "https://fra.cloud.appwrite.io/v1"
    private const val PROJECT_ID = "686a25c60006e47cfbea"

    private lateinit var client: Client

    lateinit var storage: Storage
        private set

    fun initialize(context: Context) {
        if (::client.isInitialized) return // Prevent re-initialization

        client = Client(context)
            .setEndpoint(ENDPOINT)
            .setProject(PROJECT_ID)

        storage = Storage(client)
    }

    fun getDownloadUrl(bucketId: String, fileId: String): String {
        return "$ENDPOINT/storage/buckets/$bucketId/files/$fileId/download?project=$PROJECT_ID"
    }
}