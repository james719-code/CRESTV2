package com.bdbshs.crest.ui.viewmodels

import com.bdbshs.crest.data.repository.GroupRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GroupLoadingStateTest {
    private val dispatcher = StandardTestDispatcher()
    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun cleanup() { Dispatchers.resetMain() }

    @Test fun listenerFailureStopsBothLoadingIndicatorsAndExposesRecoveryState() {
        val callback = slot<(QuerySnapshot?, Exception?) -> Unit>()
        val repository = mockk<GroupRepository>()
        every { repository.observeGroups(capture(callback)) } returns mockk<ListenerRegistration>(relaxed = true)
        val viewModel = GroupsViewModel(repository)
        callback.captured(null, Exception("offline"))
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNotNull(viewModel.uiState.value.error)
    }

    @Test fun successfulRefreshClearsThePreviousError() {
        val callback = slot<(QuerySnapshot?, Exception?) -> Unit>()
        val repository = mockk<GroupRepository>()
        every { repository.observeGroups(capture(callback)) } returns mockk<ListenerRegistration>(relaxed = true)
        val viewModel = GroupsViewModel(repository)
        callback.captured(null, Exception("offline"))
        viewModel.onRefresh()
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns emptyList()
        callback.captured(snapshot, null)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isRefreshing)
        assertNull(viewModel.uiState.value.error)
    }
}
