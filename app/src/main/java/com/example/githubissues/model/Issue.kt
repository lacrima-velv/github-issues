package com.example.githubissues.model

import androidx.recyclerview.widget.DiffUtil
import androidx.room.*
import com.google.gson.annotations.Expose
import com.google.gson.annotations.SerializedName

@Entity(tableName = "issues")
data class Issue(
    @PrimaryKey val id: Long,
    @Embedded val user: User?,
    @Embedded val assignee: Assignee?,
    val number: Long?,
    val title: String?,
    val state: String?,
    val body: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val closedAt: String?,
    @Expose(serialize = false, deserialize = false)
    // If true, use 1. If false, use 0.
    val isSelected: Int = 0
) {

    data class User(
        @SerializedName("id")
        @PrimaryKey val userId: Long?,
        @SerializedName("login")
        val userLogin: String?,
        @SerializedName("avatar_url")
        val userAvatarUrl: String?
    )

    data class Assignee(
        @SerializedName("id")
        @PrimaryKey val assigneeId: Long?,
        @SerializedName("login")
        val assigneeLogin: String?,
        @SerializedName("avatar_url")
        val assigneeAvatarUrl: String?
    )

    companion object {
        val DiffCallback = object : DiffUtil.ItemCallback<Issue>() {
            override fun areItemsTheSame(oldItem: Issue, newItem: Issue): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Issue, newItem: Issue): Boolean {
                return (oldItem.isSelected == newItem.isSelected &&
                        oldItem.state == newItem.state &&
                        oldItem.assignee == newItem.assignee &&
                        oldItem.title == newItem.title &&
                        oldItem.body == newItem.body &&
                        oldItem.updatedAt == newItem.updatedAt)
            }
        }
    }
}
