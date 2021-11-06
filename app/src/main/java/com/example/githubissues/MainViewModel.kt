package com.example.githubissues

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.githubissues.data.GithubRepository
import com.example.githubissues.data.UploadIssuesWorker
import com.example.githubissues.model.Issue
import com.example.githubissues.model.IssueState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

private val DEFAULT_ISSUE_STATE = IssueState.ALL.state
private val LAST_CHOSEN_ISSUE_STATE = IssueState.ALL.state

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
): AndroidViewModel(application), KoinComponent {

    private val repo: GithubRepository by inject()

    /**
     * Stream of immutable states representative of the issues list with certain states.
     */
    val currentIssueState: StateFlow<LatestIssuesUiState>

    /**
     * Processor of side effects from the UI, when user changes Issue state,
     * which in turn feedback into [currentIssueState]
     */
    val acceptChangingIssueState: (LatestIssuesUiAction) -> Unit

    /**
     * Represents user's actions of changing Issue State.
     */
    sealed class LatestIssuesUiAction {
        data class ChooseLatestIssues(val issueState: String) : LatestIssuesUiAction()
    }
    /**
     * Data which is necessary for displaying issues List with certain state
     */
    data class LatestIssuesUiState(
        val issueState: String = DEFAULT_ISSUE_STATE,
        val pagingData: PagingData<Issue> = PagingData.empty()
    )

    // Used to request latest issues from DB
    private fun changeIssueState(issueState: String): Flow<PagingData<Issue>> {
        return repo.getIssues(issueState)
            .cachedIn(viewModelScope)
    }

    // When issue is selected, it will be highlighted in IssuesPagingAdapter
    fun setIssueSelected(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("repository.setIssueSelected(id) returned ${repo.setIssueSelected(id)}")
            repo.setIssueSelected(id)
        }
    }

    // Used when we need to clean issue's selection. Its highlighted state will be cleared
    // in IssuesPagingAdapter
    fun deselectLastSelectedIssue() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deselectLastSelectedIssue()
        }
    }

    init {
        // When app has just started we must represent its clear state with no previous highlighting
        deselectLastSelectedIssue()
        // Enqueue work of uploading issues
        UploadIssuesWorker.enqueueWork(getApplication())
        // Used for creation of Issues' flow
        val initialIssueState: String =
            savedStateHandle.get(LAST_CHOSEN_ISSUE_STATE) ?: DEFAULT_ISSUE_STATE

        val actionStateFlow = MutableSharedFlow<LatestIssuesUiAction>()

        currentIssueState = actionStateFlow
            .filterIsInstance<LatestIssuesUiAction.ChooseLatestIssues>()
            .distinctUntilChanged()
            .onStart { emit(LatestIssuesUiAction.ChooseLatestIssues(initialIssueState)) }
            .flatMapLatest { issueStateChange ->
                changeIssueState(issueState = issueStateChange.issueState)
                    .distinctUntilChangedBy { it }
                    .map { pagingData ->
                        LatestIssuesUiState(
                            issueState = issueStateChange.issueState,
                            pagingData = pagingData
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = LatestIssuesUiState()
            )

        acceptChangingIssueState = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }

    }

    // Used to avoid scrolling to the top of the List if the user has already scrolled it
    private val _isScrolled = MutableStateFlow<ScrollUiAction>(ScrollUiAction.NotScrolled)
    val isScrolled: StateFlow<ScrollUiAction>
        get() = _isScrolled
    // Used when user has done scrolling so we can't scroll to the top
    fun doneScroll() {
        _isScrolled.value = ScrollUiAction.Scrolled
    }
    // Used when we must scroll to the top
    fun resetScroll() {
        _isScrolled.value = ScrollUiAction.NotScrolled
    }

    sealed class ScrollUiAction {
        object Scrolled : ScrollUiAction()
        object NotScrolled : ScrollUiAction()
    }

    // Used to get current issue's details in IssueDetailFragment
    private val _currentIssueDetails = MutableStateFlow<Issue?>(null)
    val currentIssueDetails: StateFlow<Issue?>
        get() = _currentIssueDetails

    // Get issue details from DB to represent it in IssueDetailFragment
    fun getIssueDetails(issueId: Long) = viewModelScope.launch(Dispatchers.IO) {
        _currentIssueDetails.value = repo.getIssueDetails(issueId)
    }

    override fun onCleared() {
        savedStateHandle[DEFAULT_ISSUE_STATE] = currentIssueState.value.issueState
        super.onCleared()
    }
}

