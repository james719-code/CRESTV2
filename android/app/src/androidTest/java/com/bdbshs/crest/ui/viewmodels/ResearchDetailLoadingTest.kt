package com.bdbshs.crest.ui.viewmodels

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import com.bdbshs.crest.data.repository.PdfRepository
import com.bdbshs.crest.data.repository.ResearchRepository
import com.bdbshs.crest.navigation.AppDestination
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ResearchDetailLoadingTest {

    private fun create(
        qualitative: CapturingSlot<(DocumentSnapshot?, FirebaseFirestoreException?) -> Unit>,
        quantitative: CapturingSlot<(DocumentSnapshot?, FirebaseFirestoreException?) -> Unit>
    ): ResearchDetailViewModel {
        val repository = mockk<ResearchRepository>()
        every { repository.observeQualitativeById("paper", capture(qualitative)) } returns mockk<ListenerRegistration>(relaxed = true)
        every { repository.observeQuantitativeById("paper", capture(quantitative)) } returns mockk<ListenerRegistration>(relaxed = true)
        return ResearchDetailViewModel(
            SavedStateHandle(mapOf(AppDestination.ResearchDetails.researchIdArg to "paper")),
            mockk<Context>(relaxed = true), repository, mockk<PdfRepository>()
        )
    }

    @Test fun missingPaperStopsLoadingAfterBothCollectionsResolve() {
        val qualitative = slot<(DocumentSnapshot?, FirebaseFirestoreException?) -> Unit>()
        val quantitative = slot<(DocumentSnapshot?, FirebaseFirestoreException?) -> Unit>()
        val viewModel = create(qualitative, quantitative)
        val missing = mockk<DocumentSnapshot>()
        every { missing.exists() } returns false
        qualitative.captured(missing, null)
        assertTrue(viewModel.uiState.value.isDetailsLoading)
        quantitative.captured(missing, null)
        assertFalse(viewModel.uiState.value.isDetailsLoading)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test fun failedLookupExposesAnErrorInsteadOfAnInfiniteSpinner() {
        val qualitative = slot<(DocumentSnapshot?, FirebaseFirestoreException?) -> Unit>()
        val quantitative = slot<(DocumentSnapshot?, FirebaseFirestoreException?) -> Unit>()
        val viewModel = create(qualitative, quantitative)
        qualitative.captured(null, FirebaseFirestoreException("offline", FirebaseFirestoreException.Code.UNAVAILABLE))
        assertFalse(viewModel.uiState.value.isDetailsLoading)
        assertNotNull(viewModel.uiState.value.error)
    }
}
