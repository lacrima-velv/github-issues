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
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import com.example.githubissues.*
import com.example.githubissues.R
import com.example.githubissues.Utils.toDp
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

        //viewModel.isAnyIssueSelected.observe(viewLifecycleOwner) { }
        //viewModel.isLayoutVertical.observe(viewLifecycleOwner) { }


        binding = FragmentIssuesListBinding.inflate(inflater, container, false)

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


     //   Timber.d("slidingPaneLayout: title is ${(activity as MainActivity).getToolbarTitle()}")
//        if ((activity as MainActivity).getToolbarTitle() == "Issue Details") {
//            viewModel.setLayoutVertical(true)
//        } else if ((activity as MainActivity).getToolbarTitle() == "Issues List") {
//            viewModel.setLayoutVertical(false)
//        }

        // Connect the SlidingPaneLayout to the system back button
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            TwoPaneOnBackPressedCallback(
                binding.slidingPaneLayout,
                issuesListToolbarTitle,
                issueDetailsToolbarTitle
                //viewModel.isLayoutVertical.value
            )
        )


        // We need to bind the root layout with our binder for external layout
        errorRetryViewBinding = ErrorRetryViewBinding.bind(binding.root)
        progressViewBinding = ProgressViewBinding.bind(binding.root)

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
            //viewModel.setIsAnyIssueSelectedToTrue()
            binding.slidingPaneLayout.openPane()

        }
//                val onFirstDetailsOpened = { issue: Issue, view: View ->
//            val width = resources.displayMetrics.widthPixels.toDp
//            view.isActivated = issue.isSelected == 1
//            Timber.d("onFirstDetailsOpened is called. width is $width and viewModel.isAnyIssueSelected.value is ${viewModel.isAnyIssueSelected.value}")
//        }

//        val onFirstDetailsOpened = { issue: Issue, view: View ->
//            val width = resources.displayMetrics.widthPixels.toDp
//            //view.isActivated = issue.isSelected == 1
//
//            if (issue.isSelected == 1) {
//                Timber.d("issue.isSelected == 1 returns ${issue.isSelected}")
//                view.isActivated = true
//            } else {
//                view.isActivated = false
//            }
//
//
//            Timber.d("onFirstDetailsOpened is called. width is $width and viewModel.isAnyIssueSelected.value is ${viewModel.isAnyIssueSelected.value}")
//        }

        issuesPagingAdapter = IssuesPagingAdapter(
            onIssueClick
            //onFirstDetailsOpened
        )

//        // Save scrolling position when fragment is recreated
//        issuesPagingAdapter.stateRestorationPolicy =
//            RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY

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

    override fun onStart() {
        super.onStart()

        val configuration = (activity as MainActivity).resources.configuration.orientation
        if (configuration == Configuration.ORIENTATION_PORTRAIT) {
            binding.slidingPaneLayout.closePane()
        }
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
        viewLifecycleOwner.lifecycleScope.launch {
            Timber.d("Collecting uiState started")
            uiState
                .map { it.issueState }
                .distinctUntilChanged()
                .collect()
            Timber.d("Collecting uiState ended")
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
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                Timber.d("onTabUnselected is called")
                return
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                Timber.d("onTabReselected is called")
//                val chosenState = when (tab?.text) {
//                    getString(R.string.tab_all) -> IssueState.ALL.state
//                    getString(R.string.tab_closed) -> IssueState.CLOSED.state
//                    getString(R.string.tab_open) -> IssueState.OPEN.state
//                    else -> IssueState.ALL.state
//                }
//
//                updateIssuesListByChoosingState(chosenState, onIssueStateChosen)
            }

        })


    }

    // When another tab is selected then scroll to top
    private fun FragmentIssuesListBinding.updateIssuesListByChoosingState(
        chosenState: String,
        onIssueStateChosen: (UiAction.ChooseIssueState) -> Unit
    ) {
        onIssueStateChosen(UiAction.ChooseIssueState(chosenState))
        issuesList.scrollToPosition(0)
        Timber.d("position: ${issuesList.verticalScrollbarPosition}")
        //issuesList.scrollTo(0, 0)
    }

    // Save selected tab position to restore it when configuration changes
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("tabState", binding.tabBar.selectedTabPosition)
    }

    private fun FragmentIssuesListBinding.bindList(
        issuesPagingAdapter: IssuesPagingAdapter,
        uiState: StateFlow<UiState>,
        onScrollChanged: (UiAction.Scroll) -> Unit
    ) {
        errorRetryViewBinding.retryButton.setOnClickListener { issuesPagingAdapter.retry() }

        issuesList.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                if (dy != 0)
                    onScrollChanged(UiAction.Scroll(currentIssueState = uiState.value.issueState))
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

//    override fun onConfigurationChanged(newConfig: Configuration) {
//        Timber.d("onConfigurationChanged is called")
//        super.onConfigurationChanged(newConfig)
//
//        val configuration = (activity as MainActivity).resources.configuration.orientation
//        if (configuration == Configuration.ORIENTATION_PORTRAIT) {
//            binding.slidingPaneLayout.closePane()
//        }
//    }





    private fun FragmentIssuesListBinding
            .addRefreshListener(issuesPagingAdapter: IssuesPagingAdapter) {
        swiperefresh.setOnRefreshListener {
            issuesPagingAdapter.refresh()
        }
    }


}