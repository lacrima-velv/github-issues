package com.example.githubissues.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.githubissues.api.GithubApiService
import com.example.githubissues.db.IssuesDatabase
import com.example.githubissues.model.Issue
import com.example.githubissues.model.IssueState
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Repository class that works with local and remote data sources.
 */
class GithubRepository(
    private val service: GithubApiService,
    private val database: IssuesDatabase) {
    /**
     * Get issues, and expose them as a stream of
     * data that will emit every time we get more data from the network
     */
    /*
    PagingData is a container for paginated data.
    Each refresh of data will have a separate corresponding PagingData
     */
    @OptIn(ExperimentalPagingApi::class)
    fun getIssues(issueState: IssueState): Flow<PagingData<Issue>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                prefetchDistance = PREFETCH_DISTANCE,
                // By default PagingConfig.maxSize is unbounded, so pages are never dropped
                maxSize = NETWORK_PAGE_SIZE + PREFETCH_DISTANCE * 2,
                enablePlaceholders = false
            ),
            remoteMediator = GithubRemoteMediator(
                service,
                database,
                issueState
            ),
            pagingSourceFactory = {
                Timber.d("Create pagingSourceFactory. issueState.state is ${issueState.state}")
                if (issueState.state != IssueState.ALL.state) {
                    database.issuesDao().getAllIssuesNoDetails(issueState.state)
                } else {
                    database.issuesDao().getAllIssuesNoDetailsAllStates()
                }
                //database.issuesDao().getAllIssuesNoDetails()
            }
        ).flow
    }

    suspend fun getIssueDetails(issueId: Long): Issue {
       return database.issuesDao().getIssueDetailsById(issueId)
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 50
    }
}