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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle
): AndroidViewModel(application), KoinComponent {

    private val repo: GithubRepository by inject()

    /**
     * Stream of immutable states representative of the UI.
     */
    val state: StateFlow<UiState>

    /**
     * Processor of side effects from the UI which in turn feedback into [state]
     */
    //All requests to the ViewModel go through a single entry point - the accept field
    val accept: (UiAction) -> Unit

    private val _currentIssueDetails = MutableLiveData<Issue>()
    val currentIssueDetails: LiveData<Issue>
        get() = _currentIssueDetails

        init {
            Timber.d("ViewModel init")
            deselectLastSelectedIssue()

            UploadIssuesWorker.enqueueWork(getApplication())

            //_isAnyIssueSelected.value = false
            //_isInitiallyNavigatedToDetails.value = false

        // Default value
       // _chosenIssueState.value = IssueState.ALL
        val initialIssueState: String = savedStateHandle.get(LAST_CHOSEN_ISSUE_STATE) ?: DEFAULT_ISSUE_STATE
        // It represents the last issue state whare the user has interacted with the list
        val lastIssueStateScrolled: String = savedStateHandle.get(LAST_ISSUE_STATE_SCROLLED) ?: DEFAULT_ISSUE_STATE


        val actionStateFlow = MutableSharedFlow<UiAction>()

        /*
        Split the flow into specific UiAction types:
        - UiAction.ChooseIssueState - for each time user changes issue state using some UI widgets
        (probably, tabs)
        - UiAction.Scroll - for each time the user scrolls the list with a particular issue state
         */
        val issueStatesChanges = actionStateFlow
            .filterIsInstance<UiAction.ChooseIssueState>()
            .distinctUntilChanged()
            .onStart { emit(UiAction.ChooseIssueState(initialIssueState)) }

        val issueStatesScrolled = actionStateFlow
            .filterIsInstance<UiAction.Scroll>()
            .distinctUntilChanged()
            /*
            This is shared to keep the flow "hot" while caching the last state scrolled,
            otherwise each flatMapLatest invocation (when this flow consumed) would lose
            the last state scrolled because each time the upstream emits, flatmapLatest will cancel
            the last Flow it was operating on, and start working based on the new flow it was given.
            That's why we use operator replay with value 1 to cache the last value
             */
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            /*
            Also used for caching. If the app was killed, but the user had already scrolled with
            chosen issue state, we don't want to scroll the list to the top causing them
            to lose their place again
            */
            .onStart { emit(UiAction.Scroll(currentIssueState = lastIssueStateScrolled)) }


        state = issueStatesChanges
            /*
            Use flatmapLatest operator, because each new issue state requires a new Pager to be
            created and therefore gets a new Flow<PagingData> as well
            */
            .flatMapLatest { issueStateChange ->
                /*
                Combine it with issueStatesScrolled, but only emitting when we have new emissions of
                PagingData
                */
                combine(
                    issueStatesScrolled,
                    changeIssueState(issueState = issueStateChange.issueState),
                    ::Pair
                )
                /*
                Each unique PagingData should be submitted once, take the latest from
                issueStatesScrolled
                 */
                    .distinctUntilChangedBy { it.second }
                    .map { (scroll, pagingData) ->
                        Timber.d("issueStateChange.issueState is ${issueStateChange.issueState} scroll.currentIssueState is ${scroll.currentIssueState}")
                        UiState(
                            issueState = issueStateChange.issueState,
                            pagingData = pagingData,
                            lastStateChosen = scroll.currentIssueState,
                            /*
                            If the issueStateChange issue state matches the scroll issue state,
                            the user has scrolled
                             */
                             hasNotScrolledForCurrentState = issueStateChange.issueState != scroll.currentIssueState
                        )
                    }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = UiState()
            )


        accept = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }

    }

    private fun changeIssueState(issueState: String): Flow<PagingData<Issue>> {
       return repo.getIssues(issueState)
            .cachedIn(viewModelScope)
    }




    fun getIssueDetails(issueId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentIssueDetails.postValue(repo.getIssueDetails(issueId))
        }
    }

    fun deselectLastSelectedIssue() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.deselectLastSelectedIssue()
        }
    }


    val setIssueSelected = { id: Long ->
       // _lastSelectedItemId.postValue(id)
        viewModelScope.launch(Dispatchers.IO) {
            Timber.d("repository.setIssueSelected(id) returned ${repo.setIssueSelected(id)}")
            repo.setIssueSelected(id)
        }
    }

    override fun onCleared() {
        savedStateHandle[DEFAULT_ISSUE_STATE] = state.value.issueState
        savedStateHandle[LAST_CHOSEN_ISSUE_STATE] = state.value.lastStateChosen
        super.onCleared()
    }
}

// Represents user's actions.
sealed class UiAction {
    // This let us to associate a scroll action with a particular state
    data class Scroll(val currentIssueState: String) : UiAction()
    data class ChooseIssueState(val issueState: String) : UiAction()
}
/*
The UiState is a representation of everything needed to render the app's UI,
with different fields corresponding to different UI components.
New versions of it are produced as a result of the user's actions:
either by changing tab with state, or by scrolling the list to fetch more.

lastStateChosen and hasNotScrolledForCurrentState flags will prevent us from scrolling to the top
of the list when we shouldn't.
 */
data class UiState(
    val issueState: String = DEFAULT_ISSUE_STATE,
    val lastStateChosen: String = LAST_CHOSEN_ISSUE_STATE,
    val hasNotScrolledForCurrentState: Boolean = false,
    val pagingData: PagingData<Issue> = PagingData.empty()
)

private val DEFAULT_ISSUE_STATE = IssueState.ALL.state
private val LAST_CHOSEN_ISSUE_STATE = IssueState.ALL.state
private val LAST_ISSUE_STATE_SCROLLED = IssueState.ALL.state