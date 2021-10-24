package com.example.githubissues.issueslist

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.githubissues.*
import com.example.githubissues.databinding.ErrorRetryViewBinding
import com.example.githubissues.databinding.FragmentIssuesListBinding
import com.example.githubissues.databinding.ProgressViewBinding
import com.example.githubissues.model.Issue
import com.example.githubissues.model.IssueState
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class IssuesListFragment : Fragment() {

    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentIssuesListBinding
    private lateinit var issuesPagingAdapter: IssuesPagingAdapter
    private lateinit var errorRetryViewBinding: ErrorRetryViewBinding
    private lateinit var progressViewBinding: ProgressViewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = ViewModelProvider(
            this, Injection.provideViewModelFactory(
            context = requireActivity(), owner = this
            )
        ).get(MainViewModel::class.java)

        binding = FragmentIssuesListBinding.inflate(inflater, container, false)

        // We need to bind the root layout with our binder for external layout
        errorRetryViewBinding = ErrorRetryViewBinding.bind(binding.root)
        progressViewBinding = ProgressViewBinding.bind(binding.root)

        val onIssueClick = { issueId: Long ->
            val action = IssuesListFragmentDirections.actionIssuesListFragmentToIssueDetailFragment(issueId)
            findNavController().navigate(action)
        }

        issuesPagingAdapter = IssuesPagingAdapter(onIssueClick)

        // Save scrolling position when fragment is recreated
        issuesPagingAdapter.stateRestorationPolicy =
            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

        // Listen to Swipe to refresh gesture
        binding.addRefreshListener(issuesPagingAdapter)


        // add dividers between RecyclerView's row items
        binding.issuesList.addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))

        // bind the state
        binding.bindState(
            uiState = viewModel.state,
            uiActions = viewModel.accept
        )

        return binding.root
    }

    /**
     * Binds the [UiState] provided  by the [ViewModel] to the UI,
     * and allows the UI to feed back user actions to it.
     */
    private fun FragmentIssuesListBinding.bindState(
        uiState: StateFlow<UiState>,
        uiActions: (UiAction) -> Unit
    ) {
        /*
        Set up header additionally because it is used when there was an error refreshing,
        but items are displayed because we got them from cache or database.
        When an error occurs during scrolling down the list, we use just footer.
         */
        val header = IssuesLoadStateAdapter { issuesPagingAdapter.retry() }
        val footer = IssuesLoadStateAdapter { issuesPagingAdapter.retry() }

        issuesList.adapter = issuesPagingAdapter.withLoadStateHeaderAndFooter(
            header = header,
            footer = footer
        )
        bindChooseState(
            uiState = uiState,
            onIssueStateChosen = uiActions
        )
        bindList(
            issuesPagingAdapter = issuesPagingAdapter,
            uiState = uiState,
            onScrollChanged = uiActions
        )
    }

    private fun FragmentIssuesListBinding.bindChooseState(
        uiState: StateFlow<UiState>,
        onIssueStateChosen: (UiAction.ChooseIssueState) -> Unit
    ) {
        binding.tabBar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val chosenState = when (tab?.text) {
                    getString(R.string.tab_all) -> IssueState.ALL.state
                    getString(R.string.tab_closed) -> IssueState.CLOSED.state
                    getString(R.string.tab_open) -> IssueState.OPEN.state
                    else -> IssueState.ALL.state
                }
                updateIssuesListByChoosingState(chosenState, onIssueStateChosen)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                return
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                return
            }

        })

        viewLifecycleOwner.lifecycleScope.launch {
            uiState
                .map { it.issueState }
                .distinctUntilChanged()
                .collect()
        }
    }

    private fun FragmentIssuesListBinding.updateIssuesListByChoosingState(
        chosenState: String,
        onIssueStateChosen: (UiAction.ChooseIssueState) -> Unit
    ) {
        issuesList.scrollToPosition(0)
        onIssueStateChosen(UiAction.ChooseIssueState(chosenState))
    }

    private fun FragmentIssuesListBinding.bindList(
        //  header: IssuesLoadStateAdapter,
        issuesPagingAdapter: IssuesPagingAdapter,
        uiState: StateFlow<UiState>,
        onScrollChanged: (UiAction.Scroll) -> Unit
        //onIssueStateChosen: (UiAction.ChooseIssueState) -> Unit
    ) {
        errorRetryViewBinding.retryButton.setOnClickListener { issuesPagingAdapter.retry() }

        issuesList.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0) onScrollChanged(UiAction.Scroll(currentIssueState = uiState.value.issueState))
            }
        })

        val notLoading = issuesPagingAdapter.loadStateFlow
            // Only emit when REFRESH LoadState for RemoteMediator changes.
            .distinctUntilChangedBy { it.refresh }
            // Only react to cases where Remote REFRESH completes i.e., NotLoading.
            .map { it.refresh is LoadState.NotLoading }

        val hasNotScrolledForCurrentState = uiState
            .map { it.hasNotScrolledForCurrentState }
            .distinctUntilChanged()

        val shouldScrollToTop = combine(
            notLoading,
            hasNotScrolledForCurrentState,
            Boolean::and
        )
            .distinctUntilChanged()

        val pagingData = uiState
            .map { it.pagingData }
            .distinctUntilChanged()


        viewLifecycleOwner.lifecycleScope.launch {
            issuesPagingAdapter.loadStateFlow.collect { loadState ->
                val isListEmpty = loadState.refresh is LoadState.NotLoading
                        && issuesPagingAdapter.itemCount == 0
                // Show empty list
                emptyListPlaceholderText.isVisible = isListEmpty
                emptyListPlaceholderImage.isVisible = isListEmpty
                // Only show the list if refresh succeeds, either from the the local db or the remote.
                issuesList.isVisible = loadState.source.refresh is LoadState.NotLoading ||
                        loadState.mediator?.refresh is LoadState.NotLoading
                // Show loading spinner during initial load or refresh
                progressViewBinding.progressBar.isVisible = loadState.mediator?.refresh is
                        LoadState.Loading && swiperefresh.isRefreshing == false
                // Show the retry state if initial load or refresh fails
                errorRetryViewBinding.errorMsg.isVisible = loadState.mediator?.refresh is
                        LoadState.Error && issuesPagingAdapter.itemCount == 0

                errorRetryViewBinding.retryButton.isVisible = loadState.mediator?.refresh is
                        LoadState.Error && issuesPagingAdapter.itemCount == 0

                if (loadState.mediator?.refresh is LoadState.NotLoading ||
                    loadState.mediator?.refresh is LoadState.Error
                ) {
                    swiperefresh.isRefreshing = false
                }

                //TODO: Maybe I should delete it later
                val errorState = loadState.mediator?.append as? LoadState.Error
                    ?: loadState.mediator?.prepend as? LoadState.Error
                    ?: loadState.append as? LoadState.Error
                    ?: loadState.prepend as? LoadState.Error
                errorState?.let {
                    Toast.makeText(
                        requireActivity(),
                        it.error.message.toString(),
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            combine(shouldScrollToTop, pagingData,::Pair)
                /*
                Each unique PagingData should be submitted once, take the latest from
                shouldScrollToTop
                */
                .distinctUntilChanged()
                .collectLatest { (shouldScroll, pagingData) ->
                    issuesPagingAdapter.submitData(pagingData)
                    /*
                    Scroll only after the data has been submitted to the adapter,
                    and is a fresh issue state
                     */
                    if (shouldScroll) issuesList.scrollToPosition(0)
                }
        }


    }





    private fun FragmentIssuesListBinding
            .addRefreshListener(issuesPagingAdapter: IssuesPagingAdapter) {
        swiperefresh.setOnRefreshListener {
            issuesPagingAdapter.refresh()
        }
    }




}