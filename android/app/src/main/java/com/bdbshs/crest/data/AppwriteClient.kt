package com.bdbshs.crest.data

import android.content.Context
import com.bdbshs.crest.BuildConfig
import io.appwrite.Client
import io.appwrite.services.Storage

object AppwriteClient {
    private val endpoint: String
        get() = BuildConfig.APPWRITE_ENDPOINT

    private val projectId: String
        get() = BuildConfig.APPWRITE_PROJECT_ID

    private lateinit var client: Client

    lateinit var storage: Storage
        private set

    fun initialize(context: Context) {
        if (::client.isInitialized) return // Prevent re-initialization

        client = Client(context)
            .setEndpoint(endpoint)
            .setProject(projectId)

        storage = Storage(client)
    }

    fun getDownloadUrl(bucketId: String, fileId: String): String {
        return "$endpoint/storage/buckets/$bucketId/files/$fileId/download?project=$projectId"
    }
}