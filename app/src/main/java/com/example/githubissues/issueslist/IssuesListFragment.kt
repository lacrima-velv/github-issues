package com.example.githubissues.issueslist

import android.app.Application
import android.content.res.Configuration
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import androidx.paging.LoadState
import androidx.recyclerview.selection.*
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.example.githubissues.*
import com.example.githubissues.R
import com.example.githubissues.Utils.toDp
import com.example.githubissues.databinding.ErrorRetryViewBinding
import com.example.githubissues.databinding.FragmentIssuesListBinding
import com.example.githubissues.databinding.ProgressViewBinding
import com.example.githubissues.model.IssueState
import com.google.android.material.tabs.TabLayout
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

// Used to check screen width to be sure what type of layout is displayed
const val WIDTH_OF_TWO_PANE_SCREEN = 600
// Used in ScrollListener
const val SCROLL_STATE_DRAGGING = 1

class IssuesListFragment : Fragment() {

    private lateinit var viewModelFactory: MainViewModelFactory
    private lateinit var viewModel: MainViewModel
    private lateinit var binding: FragmentIssuesListBinding
    private lateinit var issuesPagingAdapter: IssuesPagingAdapter
    private lateinit var errorRetryViewBinding: ErrorRetryViewBinding
    private lateinit var progressViewBinding: ProgressViewBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        viewModelFactory =  MainViewModelFactory(Application(), this)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(MainViewModel::class.java)

        // Just observe isScrolled value. It will be used later in ScrollListener
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.isScrolled.collectLatest { }
        }

        binding = FragmentIssuesListBinding.inflate(inflater, container, false)

        // We need to bind the root layout with our binder for external layout
        errorRetryViewBinding = ErrorRetryViewBinding.bind(binding.root)
        progressViewBinding = ProgressViewBinding.bind(binding.root)

        // Get previously selected tab
        if (savedInstanceState != null) {
            val selectedTab = savedInstanceState.getInt("tabState")
            binding.tabBar.selectTab(binding.tabBar.getTabAt(selectedTab))
        }

        // Provide methods for changing toolbar title when sliding two pane layout
        val issuesListToolbarTitle = {
            (activity as MainActivity).changeToolbarTitle(getString(R.string.issues_list_toolbar))
        }
        val issueDetailsToolbarTitle = {
            (activity as MainActivity).changeToolbarTitle(getString(R.string.issue_details_toolbar))
        }

        // Connect the SlidingPaneLayout to the system back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            TwoPaneOnBackPressedCallback(
                binding.slidingPaneLayout,
                issuesListToolbarTitle,
                issueDetailsToolbarTitle
            )
        )

        // Get navHostFragment to navigate to IssueDetails screen by clicking an item in the list
        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_fragment)
                as NavHostFragment
        val navController = navHostFragment.navController


        val onIssueClick = { issueId: Long ->
            viewModel.deselectLastSelectedIssue()
            viewModel.setIssueSelected(issueId)

            navController.navigate(
                R.id.issueDetailFragment,
                bundleOf("issueId" to issueId),
                NavOptions.Builder()
                    .setPopUpTo(navController.graph.startDestination, true)
                    .apply {
                        if (binding.slidingPaneLayout.isOpen) {
                            setEnterAnim(androidx.navigation.ui.ktx.R.animator.nav_default_enter_anim)
                            setExitAnim(androidx.navigation.ui.ktx.R.animator.nav_default_exit_anim)
                        }
                    }
                    .build()
            )
            binding.slidingPaneLayout.openPane()
        }

        issuesPagingAdapter = IssuesPagingAdapter(onIssueClick)

        // AdapterDataObserver is used to control when we should scroll to the top of the list
        issuesPagingAdapter.registerAdapterDataObserver(observeIssuesList())

        // Listen to Swipe to refresh gesture
        binding.addRefreshListener(issuesPagingAdapter)

        // Add dividers between RecyclerView's row items
        binding.issuesList.addItemDecoration(
            DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        )

        // Bind the state
        binding.bindState(
            issueStateUiState = viewModel.stateIssueState,
            uiActions = viewModel.acceptChangingIssueState
        )

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        // We must close pane when screen changes from wide to narrow by rotation when issue details
        // were displayed, because there will be an incorrect state of ui
        val width = resources.displayMetrics.widthPixels.toDp
        val configuration = (activity as MainActivity).resources.configuration.orientation
        if (configuration == Configuration.ORIENTATION_PORTRAIT &&
            width < WIDTH_OF_TWO_PANE_SCREEN) {
            binding.slidingPaneLayout.closePane()
        }
    }

    /**
     * Binds the [IssueStateUiState] provided  by the [ViewModel] to the UI,
     * and allows the UI to feed back user actions to it.
     */
    private fun FragmentIssuesListBinding.bindState(
        issueStateUiState: StateFlow<IssueStateUiState>,
        uiActions: (IssueStateUiAction) -> Unit
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
            issueStateUiState = issueStateUiState,
            onIssueStateChosen = uiActions
        )
        bindList(
            issuesPagingAdapter = issuesPagingAdapter,
            issueStateUiState = issueStateUiState
        )
    }

    private fun bindChooseState(
        issueStateUiState: StateFlow<IssueStateUiState>,
        onIssueStateChosen: (IssueStateUiAction.ChooseIssueState) -> Unit
    ) {
        viewLifecycleOwner.lifecycleScope.launch {
            issueStateUiState
                .map { it.issueState }
                .distinctUntilChanged()
                .collect()
        }

        binding.tabBar.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                Timber.d("onTabSelected is called")
                val chosenState = when (tab?.text) {
                    getString(R.string.tab_all) -> IssueState.ALL.state
                    getString(R.string.tab_closed) -> IssueState.CLOSED.state
                    getString(R.string.tab_open) -> IssueState.OPEN.state
                    else -> IssueState.ALL.state
                }

                updateIssuesListByChoosingState(chosenState, onIssueStateChosen)
                viewModel.fresetScroll()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) { }

            override fun onTabReselected(tab: TabLayout.Tab?) { }
        })
    }

    private fun updateIssuesListByChoosingState(
        chosenState: String,
        onIssueStateChosen: (IssueStateUiAction.ChooseIssueState) -> Unit
    ) {
        onIssueStateChosen(IssueStateUiAction.ChooseIssueState(chosenState))
    }

    // Save selected tab position to restore it when configuration changes
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tabState", binding.tabBar.selectedTabPosition)
    }

    private fun FragmentIssuesListBinding.bindList(
        issuesPagingAdapter: IssuesPagingAdapter,
        issueStateUiState: StateFlow<IssueStateUiState>
    ) {
        errorRetryViewBinding.retryButton.setOnClickListener { issuesPagingAdapter.retry() }

        issuesList.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (!swiperefresh.isRefreshing && recyclerView.scrollState == SCROLL_STATE_DRAGGING)
                    viewModel.fdoneScroll()
                Timber.d("onScrollStateChanged: isScrolled.value is " +
                        "${viewModel.isScrolled.value}")
            }
        })

        val pagingData = issueStateUiState
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

                // Show toast when an Error occurs
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
            pagingData
                .distinctUntilChanged()
                .collectLatest { pagingData ->
                    issuesPagingAdapter.submitData(pagingData)
                }
        }
    }

    private fun FragmentIssuesListBinding
            .addRefreshListener(issuesPagingAdapter: IssuesPagingAdapter) {
        swiperefresh.setOnRefreshListener {
            viewModel.fresetScroll()
            Timber.d("swiperefresh: isScrolled.value is ${viewModel.isScrolled.value}")
            issuesPagingAdapter.refresh()
        }
    }

    private fun observeIssuesList() = object : RecyclerView.AdapterDataObserver() {
        override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
            Timber.d("observeIssuesList() is called. " +
                    "isScrolled.value is ${viewModel.isScrolled.value}")
            if (viewModel.isScrolled.value == MainViewModel.ScrollUiAction.FNotScrolled) {
                binding.issuesList.layoutManager?.scrollToPosition(0)
            }
        }

        override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
            super.onItemRangeRemoved(positionStart, itemCount)
            if (viewModel.isScrolled.value == MainViewModel.ScrollUiAction.FNotScrolled) {
                binding.issuesList.layoutManager?.scrollToPosition(0)
            }
        }
    }


}