package com.example.githubissues.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.githubissues.model.Issue
import com.example.githubissues.model.IssueState

@Dao
interface IssuesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(issues: List<Issue>)
    /*
    Instead of returning a List<Issue>, return PagingSource<Int, Issue>.
    That way, the issues table becomes the source of data for Paging
     */
//    @Query("SELECT id, userLogin, number, title, state, updatedAt, closedAt " +
//            "FROM issues WHERE state = :issueState ORDER BY updatedAt DESC")
//    fun getAllIssuesNoDetails(issueState: String): PagingSource<Int, Issue>

    @Query("SELECT * FROM issues WHERE state = :state ORDER BY updatedAt DESC")
    fun getAllIssuesNoDetails(state: String): PagingSource<Int, Issue>

    @Query("SELECT * FROM issues ORDER BY updatedAt DESC")
    fun getAllIssuesNoDetailsAllStates(): PagingSource<Int, Issue>

    @Query("SELECT * FROM issues WHERE id = :id")
    suspend fun getIssueDetailsById(id: Long): Issue

    @Query("DELETE FROM issues")
    suspend fun clearIssues()
}