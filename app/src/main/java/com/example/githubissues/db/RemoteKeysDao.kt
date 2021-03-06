package com.example.githubissues.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE issueId = :issueId")
    suspend fun getRemoteKeysByIssueId(issueId: Long): RemoteKeys?
    // Delete remote keys only for unselected issues
    @Query("DELETE FROM remote_keys WHERE issueId = (" +
            "SELECT id FROM issues INNER JOIN remote_keys ON id = issueId " +
            "WHERE isSelected = 0 AND state = :state)")
    suspend fun clearRemoteKeysWithIssueState(state: String)

    @Query("DELETE FROM remote_keys WHERE issueId = (" +
            "SELECT id FROM issues INNER JOIN remote_keys ON id = issueId WHERE isSelected = 0)")
    suspend fun clearRemoteKeys()

}