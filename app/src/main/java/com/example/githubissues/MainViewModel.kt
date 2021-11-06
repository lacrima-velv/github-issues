package com.example.githubissues

import android.app.Application
import androidx.lifecycle.*
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
     * Stream of immutable states representative of the UI.
     */
    val stateIssueState: StateFlow<IssueStateUiState>

    /**
     * Processor of side effects from the UI which in turn feedback into [stateIssueState]
     */
    //All requests to the ViewModel go through a single entry point - the accept field
    val acceptChangingIssueState: (IssueStateUiAction) -> Unit

    private val _isScrolled = MutableStateFlow<ScrollUiAction>(ScrollUiAction.FNotScrolled)
    val isScrolled: StateFlow<ScrollUiAction>
        get() = _isScrolled

    fun fdoneScroll() {
        _isScrolled.value = ScrollUiAction.FScrolled
    }

    fun fresetScroll() {
        _isScrolled.value = ScrollUiAction.FNotScrolled
    }

    sealed class ScrollUiAction {
        object FScrolled : ScrollUiAction()
        object FNotScrolled : ScrollUiAction()
    }

    private val _fcurrentIssueDetails = MutableStateFlow<Issue?>(null)
    val fcurrentIssueDetails: StateFlow<Issue?>
        get() = _fcurrentIssueDetails


    fun fgetIssueDetails(issueId: Long) = viewModelScope.launch(Dispatchers.IO) {
        _fcurrentIssueDetails.value = repo.fgetIssueDetails(issueId)
    }


    init {
        Timber.d("ViewModel init")
        deselectLastSelectedIssue()

        UploadIssuesWorker.enqueueWork(getApplication())

        //_isAnyIssueSelected.value = false
        //_isInitiallyNavigatedToDetails.value = false

        // Default value
        // _chosenIssueState.value = IssueState.ALL
        val initialIssueState: String =
            savedStateHandle.get(LAST_CHOSEN_ISSUE_STATE) ?: DEFAULT_ISSUE_STATE
        // It represents the last issue state whare the user has interacted with the list
        // val lastIssueStateScrolled: String = savedStateHandle.get(LAST_ISSUE_STATE_SCROLLED) ?: DEFAULT_ISSUE_STATE


        val actionStateFlow = MutableSharedFlow<IssueStateUiAction>()

        /*
        Split the flow into specific UiAction types:
        - UiAction.ChooseIssueState - for each time user changes issue state using some UI widgets
        (probably, tabs)
        - UiAction.Scroll - for each time the user scrolls the list with a particular issue state
         */
        val issueStatesChanges = actionStateFlow
            .filterIsInstance<IssueStateUiAction.ChooseIssueState>()
            .distinctUntilChanged()
            .onStart { emit(IssueStateUiAction.ChooseIssueState(initialIssueState)) }

//        val issueStatesScrolled = actionStateFlow
//            .filterIsInstance<UiAction.Scroll>()
//            .distinctUntilChanged()
//            /*
//            This is shared to keep the flow "hot" while caching the last state scrolled,
//            otherwise each flatMapLatest invocation (when this flow consumed) would lose
//            the last state scrolled because each time the upstream emits, flatmapLatest will cancel
//            the last Flow it was operating on, and start working based on the new flow it was given.
//            That's why we use operator replay with value 1 to cache the last value
//             */
//            .shareIn(
//                scope = viewModelScope,
//                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
//                replay = 1
//            )
//            /*
//            Also used for caching. If the app was killed, but the user had already scrolled with
//            chosen issue state, we don't want to scroll the list to the top causing them
//            to lose their place again
//            */
//            .onStart { emit(UiAction.Scroll(currentIssueState = lastIssueStateScrolled)) }

        stateIssueState = issueStatesChanges
            .flatMapLatest { issueStateChange ->
                changeIssueState(issueState = issueStateChange.issueState)
                    .distinctUntilChangedBy { it }
                    .map { pagingData ->
                        IssueStateUiState(
                            issueState = issueStateChange.issueState,
                            pagingData = pagingData
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = IssueStateUiState()
            )

        acceptChangingIssueState = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }

    }

    private fun changeIssueState(issueState: String): Flow<PagingData<Issue>> {
        return repo.getIssues(issueState)
            .cachedIn(viewModelScope)
    }

    fun deselectLastSelectedIssue() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deselectLastSelectedIssue()
        }
    }

    fun setIssueSelected(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("repository.setIssueSelected(id) returned ${repo.setIssueSelected(id)}")
            repo.setIssueSelected(id)
        }
    }

    override fun onCleared() {
        savedStateHandle[DEFAULT_ISSUE_STATE] = stateIssueState.value.issueState
        super.onCleared()
    }
}

// Represents user's actions of changing Issue State.
sealed class IssueStateUiAction {
    data class ChooseIssueState(val issueState: String) : IssueStateUiAction()
}
/*
New versions of IssueStateUiAction are produced as a result of
the user's action of changing tab with state
 */
data class IssueStateUiState(
    val issueState: String = DEFAULT_ISSUE_STATE,
    val pagingData: PagingData<Issue> = PagingData.empty()
)