package com.example.githubissues.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.example.githubissues.api.GithubApiService
import com.example.githubissues.db.IssuesDatabase
import com.example.githubissues.db.RemoteKeys
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
    fun getIssues(issueState: String): Flow<PagingData<Issue>> {
        return Pager(
            config = PagingConfig(
                pageSize = NETWORK_PAGE_SIZE,
                initialLoadSize = INITIAL_LOAD_SIZE,
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
                if (issueState != IssueState.ALL.state) {
                    database.issuesDao().getAllIssuesNoDetails(issueState)
                } else {
                    database.issuesDao().getAllIssuesNoDetailsAllStates()
                }
            }
        ).flow
    }

    suspend fun getIssueDetails(issueId: Long): Issue {
        return database.issuesDao().getIssueDetailsById(issueId)
    }

    suspend fun deselectLastSelectedIssue() {
        return database.issuesDao().deselectLastSelectedIssue()
    }

    suspend fun setIssueSelected(id: Long) {
        Timber.d("database.issuesDao().setIssueSelected(id) returned " +
                "${database.issuesDao().setIssueSelected(id)}")
        return database.issuesDao().setIssueSelected(id)
    }

    private fun getNextPageKeyForWorker(pageNum: Int) =
        when (pageNum) {
            1 -> pageNum + 1 + (PREFETCH_DISTANCE / NETWORK_PAGE_SIZE)
            else -> pageNum + 1
        }

    private fun getPreviousKeyForWorker(pageNum: Int) =
        when (pageNum) {
            1 -> null
            else -> pageNum - 1
        }

    suspend fun getIssuesByWorkManager() {
        val numberOfPages = (INITIAL_LOAD_SIZE + PREFETCH_DISTANCE) / NETWORK_PAGE_SIZE

        for (pageNum in 1..numberOfPages) {
            val response = service.getIssues(IssueState.ALL.state, pageNum, NETWORK_PAGE_SIZE)
            if (response.isNotEmpty()) {
                database.issuesDao().insertAll(response)
                val remoteKeys = mutableListOf<RemoteKeys>()
                for (issue in response) {
                    remoteKeys.add(0, RemoteKeys(
                        issue.id,
                        getPreviousKeyForWorker(pageNum),
                        getNextPageKeyForWorker(pageNum)
                    )
                    )
                }
                database.remoteKeysDao().insertAll(remoteKeys)
            }
        }
    }

    companion object {
        const val NETWORK_PAGE_SIZE = 50
        const val PREFETCH_DISTANCE = 50
        const val INITIAL_LOAD_SIZE = NETWORK_PAGE_SIZE
    }
}