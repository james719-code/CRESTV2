package com.bdbshs.crest.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot

object FirestoreFieldReader {
    fun timestampMillis(rawValue: Any?): Long {
        return when (rawValue) {
            is Timestamp -> rawValue.toDate().time
            is Number -> rawValue.toLong()
            is String -> rawValue.toLongOrNull() ?: 0L
            else -> 0L
        }.coerceAtLeast(0L)
    }

    fun nonNegativeInt(rawValue: Any?): Int {
        return nonNegativeLong(rawValue)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    fun nonNegativeLong(rawValue: Any?): Long {
        return when (rawValue) {
            is Number -> rawValue.toLong()
            is String -> rawValue.toLongOrNull() ?: 0L
            else -> 0L
        }.coerceAtLeast(0L)
    }
}

fun DocumentSnapshot.timestampMillis(fieldName: String): Long {
    return FirestoreFieldReader.timestampMillis(get(fieldName))
}

fun DocumentSnapshot.nonNegativeInt(fieldName: String): Int {
    return FirestoreFieldReader.nonNegativeInt(get(fieldName))
}

fun DocumentSnapshot.nonNegativeLong(fieldName: String): Long {
    return FirestoreFieldReader.nonNegativeLong(get(fieldName))
}
