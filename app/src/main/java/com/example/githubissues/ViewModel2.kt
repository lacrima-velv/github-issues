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

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel2(
    private val repository: GithubRepository,
    //private val savedStateHandle: SavedStateHandle
): ViewModel() {

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



    }

    fun openList(issueState: IssueState): Flow<PagingData<Issue>> =
        repository.getIssues(issueState)
            .cachedIn(viewModelScope)

    fun getIssueDetails(issueId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            _currentIssueDetails.postValue(repository.getIssueDetails(issueId))
        }
    }
}