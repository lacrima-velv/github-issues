package com.example.githubissues.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE issueId = :issueId")
    suspend fun getRemoteKeysByIssueId(issueId: Long): RemoteKeys?

    @Query("DELETE FROM remote_keys WHERE issueState = :state")
    suspend fun clearRemoteKeysWithIssueState(state: String)

    @Query("DELETE FROM remote_keys")
    suspend fun clearRemoteKeys()
}