package com.bdbshs.crest.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

suspend fun Query.countWithCacheFallback(): Long {
    return try {
        count().get(AggregateSource.SERVER).await().count
    } catch (_: Exception) {
        get().await().size().toLong()
    }
}

