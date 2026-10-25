package com.bdbshs.crest.ui.viewmodels

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bdbshs.crest.data.repository.AccountRecord
import com.bdbshs.crest.data.repository.AccountRepository
import com.bdbshs.crest.data.repository.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Immutable
sealed class AccountItem {
    abstract val uid: String
    abstract val name: String

    data class Student(
        override val uid: String = "",
        override val name: String = "",
        val lrn: Long = 0,
        val strand: String = "",
        val gender: String = "",
        val accepted: Boolean = false,
        val research_accepted: Boolean = false // Using snake_case to match Firestore
    ) : AccountItem()

    data class Teacher(
        override val uid: String = "",
        override val name: String = "",
        val email: String = "",
        val access: Boolean = false,
        val upload_count: Int = 0
    ) : AccountItem()
}

// Enums and State for the Accounts screen
enum class AccountType { STUDENT, TEACHER }
enum class AccountStatus(val displayName: String) {
    Pending("Pending Approval"),
    Accepted("Accepted")
}
enum class AccountSortOption(val displayName: String) {
    NameAZ("Name (A-Z)"),
    NameZA("Name (Z-A)")
}

data class AccountsUiState(
    val searchQuery: String = "",
    val isFilterDialogVisible: Boolean = false,
    val allAccounts: List<AccountItem> = emptyList(),
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val selectedAccountType: AccountType? = null,
    val selectedStatus: AccountStatus = AccountStatus.Pending, // Default to Pending
    val selectedSortOption: AccountSortOption = AccountSortOption.NameAZ,
    val error: String? = null,
    val isActionDialogVisible: Boolean = false,
    val selectedAccountForAction: AccountItem? = null,
    val isUpdatingAccount: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class AccountsViewModel @Inject constructor(
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(AccountsUiState())
    val uiState = _uiState.asStateFlow()

    val filteredAndSortedAccounts: StateFlow<List<AccountItem>> = combine(
        _uiState,
        _searchQuery.debounce(300L)
    ) { state, query ->
        val filteredList = state.allAccounts.filter { account ->
            val queryMatch = if (query.isBlank()) true else {
                account.name.contains(query, ignoreCase = true) ||
                        (account is AccountItem.Teacher && account.email.contains(query, ignoreCase = true))
            }
            val typeMatch = state.selectedAccountType == null ||
                    (state.selectedAccountType == AccountType.STUDENT && account is AccountItem.Student) ||
                    (state.selectedAccountType == AccountType.TEACHER && account is AccountItem.Teacher)

            val statusMatch = when (state.selectedStatus) {
                AccountStatus.Pending -> (account is AccountItem.Student && !account.accepted) || (account is AccountItem.Teacher && !account.access)
                AccountStatus.Accepted -> (account is AccountItem.Student && account.accepted) || (account is AccountItem.Teacher && account.access)
            }
            queryMatch && typeMatch && statusMatch
        }
        when (state.selectedSortOption) {
            AccountSortOption.NameAZ -> filteredList.sortedBy { it.name }
            AccountSortOption.NameZA -> filteredList.sortedByDescending { it.name }
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchAccounts(isInitialLoad = true)
    }

    private fun fetchAccounts(isInitialLoad: Boolean = false) {
        if (isInitialLoad) _uiState.update { it.copy(isLoading = true, error = null) }
        else _uiState.update { it.copy(isRefreshing = true, error = null) }

        viewModelScope.launch {
            try {
                val combinedList = withContext(Dispatchers.IO) {
                    accountRepository.fetchAllAccounts().map { it.toUiItem() }
                }
                _uiState.update { it.copy(allAccounts = combinedList, isLoading = false, isRefreshing = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, isRefreshing = false, error = "Failed to load accounts: ${e.message}") }
            }
        }
    }

    fun onRefresh() = fetchAccounts()
    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    // --- Filter Dialog Functions ---
    fun showFilterDialog() { _uiState.update { it.copy(isFilterDialogVisible = true) } }
    fun dismissFilterDialog() { _uiState.update { it.copy(isFilterDialogVisible = false) } }
    fun onAccountTypeSelected(type: AccountType) {
        _uiState.update {
            val newType = if (it.selectedAccountType == type) null else type
            it.copy(selectedAccountType = newType)
        }
    }
    fun onStatusSelected(status: AccountStatus) { _uiState.update { it.copy(selectedStatus = status) } }
    fun onSortOptionSelected(option: AccountSortOption) { _uiState.update { it.copy(selectedSortOption = option) } }
    fun applyFilters() = dismissFilterDialog()

    fun onAccountClicked(account: AccountItem) {
        _uiState.update { it.copy(isActionDialogVisible = true, selectedAccountForAction = account) }
    }

    fun dismissActionDialog() {
        _uiState.update { it.copy(isActionDialogVisible = false, isUpdatingAccount = false) }
    }

    fun approveSelectedAccount() {
        val accountToUpdate = _uiState.value.selectedAccountForAction ?: return
        _uiState.update { it.copy(isUpdatingAccount = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                accountRepository.approveAccount(accountToUpdate.uid, accountToUpdate.toRepositoryRole())

                // Refresh the list locally for an instant UI update
                updateLocalAccountState(accountToUpdate.uid, isApproved = true)
                dismissActionDialog()

            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdatingAccount = false, error = "Failed to approve: ${e.message}") }
            }
        }
    }

    fun denySelectedAccount() {
        val accountToDelete = _uiState.value.selectedAccountForAction ?: return
        _uiState.update { it.copy(isUpdatingAccount = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                accountRepository.denyAccount(accountToDelete.uid, accountToDelete.toRepositoryRole())

                // Remove from local list for instant UI update
                _uiState.update { currentState ->
                    currentState.copy(
                        allAccounts = currentState.allAccounts.filterNot { it.uid == accountToDelete.uid },
                        isActionDialogVisible = false,
                        isUpdatingAccount = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isUpdatingAccount = false, error = "Failed to deny: ${e.message}") }
            }
        }
    }

    private fun updateLocalAccountState(uid: String, isApproved: Boolean) {
        _uiState.update { currentState ->
            val updatedList = currentState.allAccounts.map { account ->
                if (account.uid == uid) {
                    when (account) {
                        is AccountItem.Student -> account.copy(accepted = isApproved)
                        is AccountItem.Teacher -> account.copy(access = isApproved)
                    }
                } else {
                    account
                }
            }
            currentState.copy(allAccounts = updatedList)
        }
    }
}

private fun AccountRecord.toUiItem(): AccountItem {
    return when (this) {
        is AccountRecord.Student -> AccountItem.Student(
            uid = uid,
            name = name,
            lrn = lrn,
            strand = strand,
            gender = gender,
            accepted = accepted,
            research_accepted = researchAccepted
        )

        is AccountRecord.Teacher -> AccountItem.Teacher(
            uid = uid,
            name = name,
            email = email,
            access = access,
            upload_count = uploadCount
        )
    }
}

private fun AccountItem.toRepositoryRole(): UserRole {
    return when (this) {
        is AccountItem.Student -> UserRole.STUDENT
        is AccountItem.Teacher -> UserRole.TEACHER
    }
}