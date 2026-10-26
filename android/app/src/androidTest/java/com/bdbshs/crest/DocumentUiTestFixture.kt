package com.bdbshs.crest

import android.content.Context
import com.bdbshs.crest.data.repository.DocumentRecord
import com.bdbshs.crest.data.repository.DocumentRepository
import com.bdbshs.crest.ui.viewmodels.DocumentsViewModel
import com.google.firebase.firestore.FirebaseFirestoreException
import io.mockk.every
import io.mockk.mockk

fun documentsViewModelForUiTest(context: Context): DocumentsViewModel {
    val repository = mockk<DocumentRepository>()
    every { repository.observeDocuments(any()) } answers {
        firstArg<(List<DocumentRecord>?, FirebaseFirestoreException?) -> Unit>()(emptyList(), null)
        mockk(relaxed = true)
    }
    return DocumentsViewModel(context, repository, mockk(relaxed = true))
}
