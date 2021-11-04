package com.example.githubissues.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RemoteKeysDao {

    //@Query("INSERT INTO remote_keys VALUES ")


    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(remoteKey: List<RemoteKeys>)

    @Query("SELECT * FROM remote_keys WHERE issueId = :issueId")
    suspend fun getRemoteKeysByIssueId(issueId: Long): RemoteKeys?

    // @Query("DELETE FROM remote_keys WHERE issueState = :state")
//    @Query("DELETE FROM remote_keys WHERE issueState = :state AND NOT issueId = (SELECT id FROM issues WHERE isSelected = 1)")
//    suspend fun clearRemoteKeysWithIssueState(state: String)

    @Query("DELETE FROM remote_keys WHERE issueId = (" +
            "SELECT id FROM issues INNER JOIN remote_keys ON id = issueId " +
            "WHERE isSelected = 0 AND state = :state)")
    suspend fun clearRemoteKeysWithIssueState(state: String)

//    @Query("DELETE FROM remote_keys WHERE NOT issueId = (SELECT id FROM issues WHERE isSelected = 1)")
//    suspend fun clearRemoteKeys()
    @Query("DELETE FROM remote_keys WHERE issueId = (" +
            "SELECT id FROM issues INNER JOIN remote_keys ON id = issueId WHERE isSelected = 0)")
    suspend fun clearRemoteKeys()

//    @Query("DELETE FROM remote_keys WHERE (SELECT isSelected FROM issues INNER JOIN remote_keys ON id = issueId WHERE isSelected = 0)")
//    suspend fun clearRemoteKeysOfNoneSelectedIssues()
//
//    @Query("DELETE FROM remote_keys WHERE issueState = :state AND NOT issueId = (SELECT id FROM issues WHERE isSelected = 1)")
//    suspend fun clearRemoteKeysOfNoneSelectedIssues2(state: String)
}