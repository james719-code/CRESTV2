package com.bdbshs.crest.data

import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

suspend fun Query.countWithCacheFallback(): Long {
    return try {
        count().get(AggregateSource.SERVER).await().count
    } catch (error: FirebaseFirestoreException) {
        // Offline counts may be incomplete. Never replace a failed aggregation
        // with an unbounded server read, or hide permission/index failures.
        if (error.code != FirebaseFirestoreException.Code.UNAVAILABLE) throw error
        get(Source.CACHE).await().size().toLong()
    }
}

