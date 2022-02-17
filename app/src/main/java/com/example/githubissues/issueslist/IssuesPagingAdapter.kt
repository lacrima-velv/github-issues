package com.example.githubissues.issueslist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.githubissues.R
import com.example.githubissues.Utils.getRelativeTime
import com.example.githubissues.databinding.IssueItemBinding
import com.example.githubissues.model.Issue
import timber.log.Timber
import java.util.*

/*
The PagingDataAdapter listens to internal PagingData loading events as pages are loaded and uses
DiffUtil on a background thread to compute fine-grained updates as updated content is received
in the form of new PagingData objects
 */
class IssuesPagingAdapter(
    private val onClick: (Long) -> Boolean
    //private val onFirstDetailsOpened: (Issue, View) -> Unit,
) :
    PagingDataAdapter<Issue, IssuesPagingAdapter.IssueItemViewHolder>(Issue.DiffCallback) {

    inner class IssueItemViewHolder(private var binding: IssueItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context
        private val issueTitle = binding.issueTitle
        private val issueNumber = binding.issueNumber
        private val issueAuthor = binding.issueAuthor
        private val issueUpdatedDate = binding.issueUpdatedDate
        private val issueState = binding.issueState

        fun bind(issue: Issue) {
            Timber.d("bind() is called")
            issueTitle.text = issue.title
            issueNumber.text = context.getString(R.string.issue_number,issue.number)
            issueAuthor.text = issue.user?.userLogin ?: context.getString(R.string.unknown)
            issueUpdatedDate.text = issue.updatedAt?.let { getRelativeTime(it) }
            issueState.text = context.getString(R.string.issue_state, issue.state?.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(
                    Locale.getDefault()
                ) else it.toString()
            })

            val issueId = issue.id

            // If issue is selected in DB, then highlight it. Else - clean highlighting.
            binding.root.isActivated = issue.isSelected == 1
            Timber.d("issue.isSelected returned ${issue.isSelected}")

            binding.root.setOnClickListener {
                // Highlight issue
                binding.root.isActivated = true
                // Then open its details, deselect last selected issue and add this issue
                // as selected in DB
                onClick(issueId)
            }
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
        Timber.d("Position is $position")
        val issueInList = getItem(position)
        if (issueInList != null) {
            holder.bind(issueInList)
        }
    }

}