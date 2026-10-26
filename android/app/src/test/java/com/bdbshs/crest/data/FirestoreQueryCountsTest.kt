package com.bdbshs.crest.data

import android.util.SparseArray
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.AggregateQuery
import com.google.firebase.firestore.AggregateQuerySnapshot
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.junit.BeforeClass
import org.junit.AfterClass

class FirestoreQueryCountsTest {
    companion object {
        // Firebase's status enum builds an Android SparseArray at startup.
        @JvmStatic @BeforeClass fun prepareAndroidStatusLookup() {
            mockkConstructor(SparseArray::class)
            every { anyConstructed<SparseArray<Any>>().get(any()) } returns null
            every { anyConstructed<SparseArray<Any>>().put(any(), any()) } just Runs
            FirebaseFirestoreException.Code.values()
        }
        @JvmStatic @AfterClass fun releaseAndroidStatusLookup() {
            unmockkConstructor(SparseArray::class)
        }
    }
    private val query = mockk<Query>()
    private val aggregate = mockk<AggregateQuery>()

    init { every { query.count() } returns aggregate }

    @Test fun `online totals use aggregation without downloading documents`() = runTest {
        val result = mockk<AggregateQuerySnapshot>()
        every { result.count } returns 950L
        every { aggregate.get(AggregateSource.SERVER) } returns Tasks.forResult(result)

        assertEquals(950L, query.countWithCacheFallback())
        verify(exactly = 0) { query.get() }
        verify(exactly = 0) { query.get(any<Source>()) }
    }

    @Test fun `offline totals fall back only to local documents`() = runTest {
        val error = mockk<FirebaseFirestoreException>()
        every { error.code } returns FirebaseFirestoreException.Code.UNAVAILABLE
        every { aggregate.get(AggregateSource.SERVER) } returns Tasks.forException(error)
        val cache = mockk<QuerySnapshot>()
        every { cache.size() } returns 12
        every { query.get(Source.CACHE) } returns Tasks.forResult(cache)

        assertEquals(12L, query.countWithCacheFallback())
        verify(exactly = 1) { query.get(Source.CACHE) }
        verify(exactly = 0) { query.get() }
        verify(exactly = 0) { query.get(Source.SERVER) }
    }

    @Test fun `permission and index failures never trigger collection reads`() = runTest {
        for (code in listOf(FirebaseFirestoreException.Code.PERMISSION_DENIED,
            FirebaseFirestoreException.Code.FAILED_PRECONDITION)) {
            val error = mockk<FirebaseFirestoreException>()
            every { error.code } returns code
            every { aggregate.get(AggregateSource.SERVER) } returns Tasks.forException(error)
            try {
                query.countWithCacheFallback()
                fail("Expected the aggregation failure")
            } catch (actual: FirebaseFirestoreException) { assertSame(error, actual) }
        }
        verify(exactly = 0) { query.get() }
        verify(exactly = 0) { query.get(any<Source>()) }
    }

    @Test fun `cancellation is propagated without starting another read`() = runTest {
        every { aggregate.get(AggregateSource.SERVER) } throws CancellationException("Cancelled")
        try {
            query.countWithCacheFallback()
            fail("Expected cancellation")
        } catch (_: CancellationException) { /* Expected. */ }
        verify(exactly = 0) { query.get() }
        verify(exactly = 0) { query.get(any<Source>()) }
    }
}
