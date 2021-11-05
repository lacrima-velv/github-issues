package com.example.githubissues.db

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.githubissues.model.Issue

@Dao
interface IssuesDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(issues: List<Issue>)
    /*
    Instead of returning a List<Issue>, return PagingSource<Int, Issue>.
    That way, the issues table becomes the source of data for Paging
     */
    @Query("SELECT id, userLogin, number, title, state, updatedAt, closedAt, isSelected " +
            "FROM issues WHERE state = :state ORDER BY updatedAt DESC")
    fun getAllIssuesNoDetails(state: String): PagingSource<Int, Issue>

    @Query("SELECT id, userLogin, number, title, state, updatedAt, closedAt, isSelected " +
            "FROM issues ORDER BY updatedAt DESC")
    fun getAllIssuesNoDetailsAllStates(): PagingSource<Int, Issue>

//    @Query("SELECT id, userLogin, number, title, state, updatedAt, closedAt, isSelected " +
//            "FROM issues WHERE state = 'open' OR state = 'closed' ORDER BY updatedAt DESC")
//    fun getAllIssuesNoDetailsAllStates(): PagingSource<Int, Issue>

    @Query("SELECT * FROM issues WHERE id = :id")
    suspend fun getIssueDetailsById(id: Long): Issue
    // Don't remove selected issues until they became deselected
    @Query("DELETE FROM issues WHERE state = :state AND isSelected = 0")
    suspend fun clearIssuesWithState(state: String)

    @Query("DELETE FROM issues WHERE isSelected = 0")
    suspend fun clearIssues()

    @Query("UPDATE issues SET isSelected = 1 WHERE id = :id")
    suspend fun setIssueSelected(id: Long)

    @Query("UPDATE issues SET isSelected = 0 WHERE isSelected = 1")
    suspend fun deselectLastSelectedIssue()

//    @Query("SELECT isSelected FROM issues WHERE id = :id")
//    suspend fun checkIsIssueSelected(id: Long): Int
}