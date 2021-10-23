package com.example.githubissues.issueslist

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.githubissues.DateTimeFormatter.getRelativeTime
import com.example.githubissues.databinding.IssueItemBinding
import com.example.githubissues.model.Issue
import timber.log.Timber

/*
The PagingDataAdapter listens to internal PagingData loading events as pages are loaded and uses
DiffUtil on a background thread to compute fine-grained updates as updated content is received
in the form of new PagingData objects
 */
class IssuesPagingAdapter(private val onClick: (Long) -> Unit) :
    PagingDataAdapter<Issue, IssuesPagingAdapter.IssueItemViewHolder>(Issue.DiffCallback) {

    inner class IssueItemViewHolder(private var binding: IssueItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val issueTitle = binding.issueTitle
        private val issueNumber = binding.issueNumber
        private val issueAuthor = binding.issueAuthor
        private val issueUpdatedDate = binding.issueUpdatedDate
        private val issueState = binding.issueState

        fun bind(issue: Issue) {
            Timber.d("Bind is called")
            issueTitle.text = issue.title
            issueNumber.text = issue.number.toString()
            // TODO use Unknown in strings. Add by to strings
            issueAuthor.text = issue.user?.userLogin ?: "Unknown"
            // TODO convert date to "N days ago"
            //issueUpdatedDate.text = issue.updatedAt
            issueUpdatedDate.text = issue.updatedAt?.let { getRelativeTime(it) }
            // TODO add to strings "State: ..."
            issueState.text = issue.state
            Timber.d("Bind: issue.state is ${issue.state}")

            val issueId = issue.id

            binding.root.setOnClickListener {
                onClick(issueId)
            }

            //issueData.text = issue.number.toString() + issue.updatedAt
//            binding.root.setOnClickListener {
//                issue.htmlUrl?.let { it1 -> onClick(this.binding.root, it1) }
//            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IssueItemViewHolder {
        return IssueItemViewHolder(
            IssueItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: IssueItemViewHolder, position: Int) {
        val issueInList = getItem(position)
        if (issueInList != null) {
            holder.bind(issueInList)
        }
    }
}