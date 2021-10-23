package com.example.githubissues.model

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.google.gson.annotations.SerializedName

@Entity(tableName = "issues")
data class Issue(
    @PrimaryKey val id: Long,
    //val url: String,
    @Embedded val user: User?,
    @Embedded val assignee: Assignee?,
    //@SerializedName("html_url")
    val number: Long,
    val title: String,
    val state: String,
    val body: String?,
    @SerializedName("created_at")
    val createdAt: String?,
    @SerializedName("updated_at")
    val updatedAt: String?,
    val closedAt: String?
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
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: Issue, newItem: Issue): Boolean {
                return oldItem.id == newItem.id
            }
        }
    }
}
