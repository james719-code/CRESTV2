package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.CrestStorage
import com.bdbshs.crest.data.FirestorePaths
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Test

class ResearchDownloadCountTest {
    @Test fun `known research type updates its counter without an existence read`() = runTest {
        val firestore = mockk<FirebaseFirestore>()
        val repository = ResearchRepository(firestore, mockk<CrestStorage>())
        for ((type, path) in listOf("qualitative" to FirestorePaths.QUALITATIVE_RESEARCHES,
            "quantitative" to FirestorePaths.QUANTITATIVE_RESEARCHES)) {
            val collection = mockk<CollectionReference>()
            val document = mockk<DocumentReference>()
            every { firestore.collection(path) } returns collection
            every { collection.document("same-id") } returns document
            every { document.update("downloads", any<FieldValue>()) } returns Tasks.forResult(null)

            repository.incrementDownloadCount("same-id", type)

            verify(exactly = 1) { document.update("downloads", any<FieldValue>()) }
            verify(exactly = 0) { document.get() }
        }
    }
}
