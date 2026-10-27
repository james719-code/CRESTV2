package com.bdbshs.crest.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.*
import com.bdbshs.crest.data.repository.PlanningRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlanningUiState(
    val records: PlanningRecords = PlanningRecords(), val teacher: Boolean = false,
    val busy: Boolean = false, val error: String? = null, val inboxError: String? = null
)

@HiltViewModel
class PlanningViewModel @Inject constructor(private val repository: PlanningRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(PlanningUiState())
    val uiState = _uiState.asStateFlow()
    private var uid: String? = null
    private var teacher = false
    private var observation: Job? = null
    private var captureJob: Job? = null
    private var actionJob: Job? = null
    private var generation = 0

    fun bind(account: String?, role: UserType?, allowed: Boolean) {
        val next = account?.takeIf { allowed && role != null }
        val nextTeacher = role == UserType.TEACHER
        if (next == uid && nextTeacher == teacher) return
        generation++
        observation?.cancel(); captureJob?.cancel(); actionJob?.cancel()
        uid = next; teacher = nextTeacher
        _uiState.value = PlanningUiState(teacher = teacher, records = PlanningRecords(loading = next != null))
        if (next != null) observe(next, generation)
    }
    private fun observe(account: String, version: Int) {
        observation = viewModelScope.launch {
            repository.observe(account, teacher).catch { error ->
                if (version == generation) _uiState.update { it.copy(records = PlanningRecords(loading = false), busy = false,
                    error = "School updates could not be loaded. Check your connection and school access, then retry.") }
            }.collect { records ->
                if (version != generation) return@collect
                _uiState.update { it.copy(records = records, error = null) }
                if (!records.loading) capture(records, account, version)
            }
        }
    }
    private fun capture(records: PlanningRecords, account: String, version: Int) {
        captureJob?.cancel()
        val known = records.notifications.map { it.id }.toSet()
        val missing = PlanningInbox.candidates(records).filterNot { it.id in known }
        if (missing.isEmpty()) return
        captureJob = viewModelScope.launch {
            try {
                repository.capture(account, teacher, missing)
                if (version == generation) _uiState.update { it.copy(inboxError = null) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (version == generation) _uiState.update { it.copy(inboxError = "New updates could not be saved to the inbox. Retry when connected.") } }
        }
    }
    fun retry() {
        val account = uid ?: return
        generation++; observation?.cancel(); captureJob?.cancel(); actionJob?.cancel()
        _uiState.value = PlanningUiState(teacher = teacher)
        observe(account, generation)
    }
    private fun action(work: suspend (String) -> Unit, success: () -> Unit = {}) {
        val account = uid ?: return
        if (_uiState.value.busy) return
        val version = generation
        _uiState.update { it.copy(busy = true, error = null) }
        actionJob = viewModelScope.launch {
            try { work(account); if (version == generation) success() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { if (version == generation) _uiState.update { it.copy(error = error.message ?: "The change could not be saved. Try again.") } }
            finally { if (version == generation) _uiState.update { it.copy(busy = false) } }
        }
    }
    fun save(input: MilestoneInput, existing: ResearchMilestone?, saved: () -> Unit) {
        if (teacher) action({ repository.save(it, input, existing) }, saved)
    }
    fun change(item: ResearchMilestone, operation: String, saved: () -> Unit = {}) {
        if (teacher) action({ repository.change(it, item, operation) }, saved)
    }
    fun markRead(ids: List<String>, read: Boolean, saved: () -> Unit = {}) =
        action({ repository.markRead(it, teacher, ids, read) }, saved)
    fun dismissError() { _uiState.update { it.copy(error = null) } }
}
