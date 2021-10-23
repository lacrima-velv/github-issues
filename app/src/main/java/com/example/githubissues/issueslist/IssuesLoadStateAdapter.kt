package com.example.githubissues.issueslist

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.LoadState
import androidx.paging.LoadStateAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.githubissues.databinding.ErrorRetryViewBinding
import com.example.githubissues.databinding.IssuesLoadStateFooterHeaderViewItemBinding
import com.example.githubissues.databinding.ProgressViewBinding

class IssuesLoadStateAdapter(private val retry: () -> Unit) :
    LoadStateAdapter<IssuesLoadStateAdapter.IssuesLoadStateViewHolder>(){
    private lateinit var errorRetryViewBinding: ErrorRetryViewBinding
    private lateinit var progressViewBinding: ProgressViewBinding

    inner class IssuesLoadStateViewHolder(
        binding: IssuesLoadStateFooterHeaderViewItemBinding,
        retry: () -> Unit) : RecyclerView.ViewHolder(binding.root) {
            init {
                // We need to bind the root layout with our binder for external layout
                errorRetryViewBinding = ErrorRetryViewBinding.bind(binding.root)
                progressViewBinding = ProgressViewBinding.bind(binding.root)

                errorRetryViewBinding.retryButton.setOnClickListener { retry() }
            }
        fun bind(loadState: LoadState) {
//            if (loadState is LoadState.Error) {
//                errorRetryViewBinding.errorMsg.text = loadState.error.localizedMessage
//            }
            progressViewBinding.progressBar.isVisible = loadState is LoadState.Loading
            errorRetryViewBinding.retryButton.isVisible = loadState is LoadState.Error
            errorRetryViewBinding.errorMsg.isVisible = loadState is LoadState.Error
        }

    }

    override fun onBindViewHolder(holder: IssuesLoadStateViewHolder, loadState: LoadState) {
        holder.bind(loadState)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        loadState: LoadState
    ): IssuesLoadStateViewHolder {
        return IssuesLoadStateViewHolder(
            IssuesLoadStateFooterHeaderViewItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ), retry
        )
    }
}