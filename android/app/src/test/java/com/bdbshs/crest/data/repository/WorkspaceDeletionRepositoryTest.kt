package com.bdbshs.crest.data.repository

import com.bdbshs.crest.data.FirestorePaths
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.Source
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkspaceDeletionRepositoryTest {
    @Test fun `membership cleanup finds modern and unindexed legacy links using server records`() = runTest {
        val firestore = mockk<FirebaseFirestore>()
        val students = mockk<CollectionReference>()
        val snapshot = mockk<QuerySnapshot>()
        val data = listOf(
            Triple("modern", "team", ""), Triple("legacy", null, "team"),
            Triple("mixed", "", "team"), Triple("other", "different", "different"),
            Triple("unassigned", "", "")
        )
        every { firestore.collection(FirestorePaths.STUDENTS) } returns students
        every { students.get(Source.SERVER) } returns Tasks.forResult(snapshot)
        every { snapshot.documents } returns data.map { (uid, modern, legacy) ->
            mockk<DocumentSnapshot> {
                every { id } returns uid
                every { getString("groupId") } returns modern
                every { getString("group_id") } returns legacy
            }
        }
        val repository = WorkspaceDeletionRepository(firestore, mockk<FirebaseAuth>())
        assertEquals(setOf("modern", "legacy", "mixed"), repository.findLinkedStudentIds("team").toSet())
        verify(exactly = 1) { students.get(Source.SERVER) }
        // An offline cache is not a complete enough membership list for deletion.
        verify(exactly = 0) { students.get(Source.CACHE) }
    }
}
