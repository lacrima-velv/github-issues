package com.example.githubissues

import androidx.lifecycle.*
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.githubissues.data.GithubRepository
import com.example.githubissues.model.Issue
import com.example.githubissues.model.IssueState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(
    private val repository: GithubRepository,
    //private val savedStateHandle: SavedStateHandle
): ViewModel() {

    /**
     * Stream of immutable states representative of the UI.
     */
    val state: StateFlow<UiState>

    /**
     * Processor of side effects from the UI which in turn feedback into [state]
     */
    //All requests to the ViewModel go through a single entry point - the accept field
    val accept: (UiAction) -> Unit
   // val accept: (UiAction) -> Issue
    private val _currentIssueDetails = MutableLiveData<Issue>()
    val currentIssueDetails: LiveData<Issue>
        get() = _currentIssueDetails

    private val _chosenIssueState = MutableLiveData<IssueState>()
    val chosenIssueState: LiveData<IssueState>
        get() = _chosenIssueState

    fun changeIssueState(issueState: IssueState) {
        _chosenIssueState.value = issueState
    }

    init {
        // Default value
        _chosenIssueState.value = IssueState.ALL

        val actionStateFlow = MutableSharedFlow<UiAction>()

        state = actionStateFlow
            .filterIsInstance<UiAction.OpenList>()
            .distinctUntilChanged()
            /*
            Converts a cold Flow into a hot StateFlow that is started in the given coroutine scope,
            sharing the most recently emitted value from a single running instance of the upstream
            flow with multiple downstream subscribers
            */
            .shareIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                replay = 1
            )
            // Returns a flow that invokes the given action before this flow starts to be collected
            /*
             Also used for caching. If the app was killed, but the user had already
             scrolled through a state, we don't want to scroll the list to the top
             causing them to lose their place again.
            */
            .onStart { emit(UiAction.OpenList(chosenIssueState.value ?: IssueState.ALL))
                Timber.d("chosenIssueState inside view model is ${chosenIssueState.value}")
            }
            /*
            Use flatmapLatest operator, because each new issue state requires a new Pager to be
            created and therefore gets a new Flow<PagingData> as well
            */
            .flatMapLatest { openList ->
                openList(openList.issueState)
                    .map { pagingData ->
                        UiState(
                            issueState = openList.issueState,
                            pagingData = pagingData
                        )
                    }
            }
//            .flatMapLatest {
//                openList(chosenIssueState.value ?: IssueState.OPEN)
//                    .map { pagingData ->
//                        UiState(
//                            issueState = chosenIssueState.value ?: IssueState.OPEN,
//                            pagingData = pagingData
//                        )
//                    }
//            }
            /*
            Converts a cold Flow into a hot StateFlow that is started in the given coroutine scope,
            sharing the most recently emitted value from a single running instance of the upstream
            flow with multiple downstream subscribers
            */
            .stateIn(
                scope = viewModelScope,
                // This start policy keeps the upstream producer active while there are active subscribers
                started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5000),
                initialValue = UiState(chosenIssueState.value ?: IssueState.ALL)
            )

        accept = { action ->
            viewModelScope.launch { actionStateFlow.emit(action) }
        }

    }

    private fun openList(issueState: IssueState): Flow<PagingData<Issue>> =
        repository.getIssues(issueState)
            .cachedIn(viewModelScope)

    fun getIssueDetails(issueId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentIssueDetails.postValue(repository.getIssueDetails(issueId))
        }
    }
}
// Represents user's actions.
sealed class UiAction {
    //object OpenList : UiAction()
    data class OpenList(val issueState: IssueState) : UiAction() //TODO: Probably should delete
    // This let us to associate a scroll action with a particular state
    data class Scroll(val currentIssueState: IssueState) : UiAction()
    data class ChooseIssueState(val issueState: IssueState) : UiAction()
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
    //val hasNotScrolled: Boolean = false,
    val issueState: IssueState = DEFAULT_ISSUE_STATE,
    val lastStateChosen: IssueState = LAST_CHOSEN_ISSUE_STATE,
    val hasNotScrolledForCurrentState: Boolean = false,
    val pagingData: PagingData<Issue> = PagingData.empty()
)

private val DEFAULT_ISSUE_STATE = IssueState.ALL
private val LAST_CHOSEN_ISSUE_STATE = IssueState.ALL