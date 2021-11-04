package com.example.githubissues.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_keys")
data class RemoteKeys(
    @PrimaryKey val issueId: Long,
    val prevKey: Int?,
    val nextKey: Int?,
    // TODO: Try to delete this field and use it from IssuesDao
    //val issueState: String
)
