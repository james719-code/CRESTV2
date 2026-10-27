package com.bdbshs.crest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.WorkspaceDeletionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupDeletionOperation(val group: GroupItem, val isDeleting: Boolean = true, val error: String? = null)

data class GroupDeletionUiState(
    val operations: Map<String, GroupDeletionOperation> = emptyMap(),
    val deletedIds: Set<String> = emptySet()
) {
    fun visibleGroups(groups: List<GroupItem>): List<GroupItem> =
        (operations.values.map { it.group } + groups).distinctBy { it.id }.filterNot { it.id in deletedIds }
}

/** Owned by the app so navigation away from group details does not cancel a deletion. */
@HiltViewModel
class GroupDeletionViewModel @Inject constructor(
    private val repository: WorkspaceDeletionRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(GroupDeletionUiState())
    val uiState = _uiState.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
    private var ownerUid = auth.currentUser?.uid
    private var generation = 0
    private val authListener = FirebaseAuth.AuthStateListener { syncSession(it.currentUser?.uid) }

    init { auth.addAuthStateListener(authListener) }

    private fun syncSession(uid: String?) {
        if (uid == ownerUid) return
        ownerUid = uid
        generation++
        jobs.values.forEach { it.cancel() }
        jobs.clear()
        _uiState.value = GroupDeletionUiState()
    }

    fun deleteGroup(group: GroupItem) {
        syncSession(auth.currentUser?.uid)
        if (ownerUid == null || _uiState.value.operations[group.id]?.isDeleting == true || group.id in _uiState.value.deletedIds) return
        val operationGeneration = generation
        _uiState.update { it.copy(operations = it.operations + (group.id to GroupDeletionOperation(group))) }
        val job = viewModelScope.launch(start = CoroutineStart.LAZY) {
            try {
                repository.deleteGroup(group.id)
                if (generation == operationGeneration) _uiState.update {
                    it.copy(operations = it.operations - group.id, deletedIds = it.deletedIds + group.id)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == operationGeneration) _uiState.update {
                    it.copy(operations = it.operations + (group.id to GroupDeletionOperation(
                        group, isDeleting = false,
                        error = "Could not delete this group. ${error.message ?: "Check your connection and try again."}"
                    )))
                }
            } finally {
                if (generation == operationGeneration) jobs.remove(group.id)
            }
        }
        jobs[group.id] = job
        job.start()
    }

    fun retry(groupId: String) { _uiState.value.operations[groupId]?.group?.let(::deleteGroup) }

    override fun onCleared() {
        auth.removeAuthStateListener(authListener)
        super.onCleared()
    }
}
