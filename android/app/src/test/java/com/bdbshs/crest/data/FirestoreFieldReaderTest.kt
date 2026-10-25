package com.bdbshs.crest.data

import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class FirestoreFieldReaderTest {

    @Test
    fun `timestampMillis supports Firestore Timestamp`() {
        val expectedMillis = 1_700_000_000_000L

        val result = FirestoreFieldReader.timestampMillis(Timestamp(Date(expectedMillis)))

        assertEquals(expectedMillis, result)
    }

    @Test
    fun `timestampMillis supports numeric and string millis`() {
        assertEquals(1234L, FirestoreFieldReader.timestampMillis(1234L))
        assertEquals(1234L, FirestoreFieldReader.timestampMillis(1234))
        assertEquals(1234L, FirestoreFieldReader.timestampMillis("1234"))
    }

    @Test
    fun `timestampMillis clamps invalid or negative values to zero`() {
        assertEquals(0L, FirestoreFieldReader.timestampMillis("-1"))
        assertEquals(0L, FirestoreFieldReader.timestampMillis("not-a-number"))
        assertEquals(0L, FirestoreFieldReader.timestampMillis(null))
    }

    @Test
    fun `nonNegativeInt clamps negatives and overflow`() {
        assertEquals(0, FirestoreFieldReader.nonNegativeInt(-5L))
        assertEquals(42, FirestoreFieldReader.nonNegativeInt("42"))
        assertEquals(Int.MAX_VALUE, FirestoreFieldReader.nonNegativeInt(Long.MAX_VALUE))
    }
}

