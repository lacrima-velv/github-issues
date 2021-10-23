package com.example.githubissues.data

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import androidx.room.withTransaction
import com.example.githubissues.api.GithubApiService
import com.example.githubissues.db.IssuesDatabase
import com.example.githubissues.db.RemoteKeys
import com.example.githubissues.model.Issue
import com.example.githubissues.model.IssueState
import retrofit2.HttpException
import java.io.IOException

// GitHub page API is 1 based: https://developer.github.com/v3/#pagination
private const val GITHUB_STARTING_PAGE_INDEX = 1

@OptIn(ExperimentalPagingApi::class)
class GithubRemoteMediator(
    private val service: GithubApiService,
    private val issuesDatabase: IssuesDatabase,
    private val issueState: IssueState
) : RemoteMediator<Int, Issue>() {
    override suspend fun load(loadType: LoadType, state: PagingState<Int, Issue>): MediatorResult {
        val page = when (loadType) {
            // LoadType.REFRESH gets called when it's the first time we're loading data,
            // or when PagingDataAdapter.refresh() is called; so now the point of reference
            // for loading our data is the state.anchorPosition
            LoadType.REFRESH -> {
                val remoteKeys = getRemoteKeyClosestToCurrentPosition(state)
                remoteKeys?.nextKey?.minus(1) ?: GITHUB_STARTING_PAGE_INDEX
            }
            // When we need to load data at the beginning of the currently loaded data set,
            // the load parameter is LoadType.PREPEND
            LoadType.PREPEND -> {
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with `endOfPaginationReached = false` because Paging
                // will call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its prevKey is null, that means we've reached
                // the end of pagination for prepend.
                val remoteKeys = getRemoteKeyForFirstItem(state)
                val prevKey = remoteKeys?.prevKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                prevKey

            }
            // When we need to load data at the end of the currently loaded data set,
            // the load parameter is LoadType.APPEND
            LoadType.APPEND -> {
                val remoteKeys = getRemoteKeyForLastItem(state)
                // If remoteKeys is null, that means the refresh result is not in the database yet.
                // We can return Success with endOfPaginationReached = false because Paging
                // will call this method again if RemoteKeys becomes non-null.
                // If remoteKeys is NOT NULL but its nextKey is null, that means we've reached
                // the end of pagination for append.
                val nextKey = remoteKeys?.nextKey
                    ?: return MediatorResult.Success(endOfPaginationReached = remoteKeys != null)
                nextKey
            }
        }

        try {
            val apiResponse = service.getIssues(issueState.state, page, state.config.pageSize)

            val issues = apiResponse
            val endOfPaginationReached = issues.isEmpty()
            issuesDatabase.withTransaction {
                // Clear all tables in the database
                if (loadType == LoadType.REFRESH) {
                    issuesDatabase.remoteKeysDao().clearRemoteKeys()
                    issuesDatabase.issuesDao().clearIssues()
                }
                val prevKey = if (page == GITHUB_STARTING_PAGE_INDEX) null else page - 1
                val nextKey = if (endOfPaginationReached) null else page + 1
                val keys = issues.map {
                    RemoteKeys(issueId = it.id, prevKey = prevKey, nextKey = nextKey)
                }
                issuesDatabase.remoteKeysDao().insertAll(keys)
                issuesDatabase.issuesDao().insertAll(issues)
            }
            return MediatorResult.Success(endOfPaginationReached = endOfPaginationReached)
        } catch (exception: IOException) {
            return MediatorResult.Error(exception)
        } catch (exception: HttpException) {
            return MediatorResult.Error(exception)
        }
    }

    // We need to get the remote key of the last Repo item loaded from the database
    private suspend fun getRemoteKeyForLastItem(state: PagingState<Int, Issue>): RemoteKeys? {
        // Get the last page that was retrieved, that contained items.
        // From that last page, get the last item
        return state.pages.lastOrNull { it.data.isNotEmpty() }?.data?.lastOrNull()
            ?.let { issue ->
                // Get the remote keys of the last item retrieved
                issuesDatabase.remoteKeysDao().getRemoteKeysByIssueId(issue.id)
            }
    }

    // We need to get the remote key of the first Issue item loaded from the database
    private suspend fun getRemoteKeyForFirstItem(state: PagingState<Int, Issue>): RemoteKeys? {
        // Get the first page that was retrieved, that contained items.
        // From that first page, get the first item
        return state.pages.firstOrNull { it.data.isNotEmpty() }?.data?.firstOrNull()
            ?.let { issue ->
                // Get the remote keys of the first items retrieved
                issuesDatabase.remoteKeysDao().getRemoteKeysByIssueId(issue.id)
            }
    }

    private suspend fun getRemoteKeyClosestToCurrentPosition(state: PagingState<Int, Issue>):
            RemoteKeys? {
        // The paging library is trying to load data after the anchor position
        // Get the item closest to the anchor position
        return state.anchorPosition?.let { position ->
            state.closestItemToPosition(position)?.id?.let { issueId ->
                issuesDatabase.remoteKeysDao().getRemoteKeysByIssueId(issueId)
            }
        }
    }

}