package com.example.githubissues.issuedetail

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.transform.CircleCropTransformation
import com.example.githubissues.*
import com.example.githubissues.Utils.getRelativeTime
import com.example.githubissues.api.GithubApiService
import com.example.githubissues.data.GithubRepository
import com.example.githubissues.databinding.FragmentIssueDetailBinding
import com.example.githubissues.db.IssuesDatabase
import com.example.githubissues.model.Issue
import timber.log.Timber
import java.util.*

class IssueDetailFragment : Fragment() {

    private lateinit var binding: FragmentIssueDetailBinding
    //private lateinit var appContainer: AppContainer
    private lateinit var viewModelFactory: MainViewModelFactory
    private lateinit var viewModel: MainViewModel
    //private lateinit var args: IssueDetailFragmentArgs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val service = GithubApiService.create()
        val issuesDatabase = IssuesDatabase.getInstance(requireContext())

        val repository = GithubRepository(service, issuesDatabase)

        binding = FragmentIssueDetailBinding.inflate(layoutInflater)
        //val issueId = if (IssueDetailFragmentArgs.fromBundle(requireArguments()))

        //val args: IssueDetailFragmentArgs? = arguments?.let { IssueDetailFragmentArgs.fromBundle(it) }

        val id = arguments?.getLong("issueId")
        Timber.d("issue id is $id")

        //val args = IssueDetailFragmentArgs.fromBundle(requireArguments())
        //Timber.d("Got an issue id ${args.issueId}")

//        val navHostFragment = parentFragmentManager.findFragmentById(R.id.nav_host_fragment)
//                as NavHostFragment
//        val navController = navHostFragment.navController

       // appContainer = (Application() as MyApplication).appContainer

        viewModelFactory =  MainViewModelFactory(Application(), this)

        viewModel = ViewModelProvider(this, viewModelFactory).get(MainViewModel::class.java)

//        viewModel = ViewModelProvider(
//            this, Injection.provideViewModelFactory(
//            application = Application(),
//            context = requireActivity(),
//            owner = this
//            )
//        ).get(MainViewModel::class.java)

//        args?.issueId?.let {
//            viewModel.getIssueDetails(it)
//            binding.showIssueDetails(viewModel.currentIssueDetails.value)
//        } ?: binding.showPlaceholder()
        id?.let {
            viewModel.getIssueDetails(it)
            binding.showIssueDetails(viewModel.currentIssueDetails.value)

//            if (viewModel.currentIssueDetails.value?.title == null) {
//                binding.showPlaceholder()
//            }
        } ?: binding.showPlaceholder()

        viewModel.currentIssueDetails.observe(viewLifecycleOwner) {
            Timber.d("Current issue title: ${viewModel.currentIssueDetails.value?.title}")
            binding.showIssueDetails(viewModel.currentIssueDetails.value)
        }

        return binding.root
    }

    private fun FragmentIssueDetailBinding.showPlaceholder() {
        /*
            If couldn't get arguments, probably, there are no issues in issues list.
            Then show placeholder for empty state.
            */
        emptyIssueDetailsPlaceholderImage.isVisible = true
        emptyIssueDetailsPlaceholderText.isVisible = true

        issueDetailsCreated.isVisible = false
        issueDetailsUpdated.isVisible = false
        issueDetailsAssignedTo.isVisible = false
        issueDetailsPostedBy.isVisible = false
        issueDetailsTitle.isVisible = false
        issueDetailsNumber.isVisible = false
        issueDetailsState.isVisible = false
        issueDetailsBody.isVisible = false
        issueDetailsAuthor.isVisible = false
        issueDetailsAssignee.isVisible = false
        issueDetailsCreatedDate.isVisible = false
        issueDetailsUpdatedDate.isVisible = false
        issueDetailsAssigneeAvatar.isVisible = false
        issueDetailsAuthorAvatar.isVisible = false
    }

    private fun FragmentIssueDetailBinding.showIssueDetails(currentIssueDetails: Issue?) {
        Timber.d("currentIssueDetails is $currentIssueDetails")
        Timber.d("currentIssueDetails?.state is ${currentIssueDetails?.state}")

        if (currentIssueDetails?.state == null) {
            showPlaceholder()
        } else {
            // Remove placeholder if it was visible
            emptyIssueDetailsPlaceholderImage.isVisible = false
            emptyIssueDetailsPlaceholderText.isVisible = false

            issueDetailsCreated.isVisible = true
            issueDetailsUpdated.isVisible = true
            issueDetailsAssignedTo.isVisible = true
            issueDetailsPostedBy.isVisible = true
            issueDetailsTitle.isVisible = true
            issueDetailsNumber.isVisible = true
            issueDetailsState.isVisible = true
            issueDetailsBody.isVisible = true
            issueDetailsAuthor.isVisible = true
            issueDetailsAssignee.isVisible = true
            issueDetailsCreatedDate.isVisible = true
            issueDetailsUpdatedDate.isVisible = true
            issueDetailsAssigneeAvatar.isVisible = true
            issueDetailsAuthorAvatar.isVisible = true

            // If we got arguments, display issue's details
            issueDetailsState.text = getString(
                R.string.issue_state, currentIssueDetails?.state?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            )
            issueDetailsNumber.text = getString(R.string.issue_number, currentIssueDetails?.number)
            issueDetailsAuthor.text = currentIssueDetails?.user?.userLogin
            issueDetailsTitle.text = currentIssueDetails?.title

            issueDetailsBody.text = currentIssueDetails?.body ?: getString(R.string.no_description)
            issueDetailsAssignee.text = currentIssueDetails?.assignee?.assigneeLogin ?: getString(R.string.no_one_assignee)

            issueDetailsUpdatedDate.text = currentIssueDetails?.updatedAt?.
            let { getRelativeTime(it) } ?: getString(R.string.unknown)
            issueDetailsCreatedDate.text = currentIssueDetails?.createdAt?.
            let { getRelativeTime(it) } ?: getString(R.string.unknown)

            // Get Author's avatar
            getAvatar(issueDetailsAuthorAvatar, currentIssueDetails?.user?.userAvatarUrl)

            // Get Assignee's avatar
            getAvatar(issueDetailsAssigneeAvatar, currentIssueDetails?.assignee?.assigneeAvatarUrl)
        }

    }

    private fun getAvatar(imageView: ImageView, avatarUrl: String?) {
        Timber.d("avatarUrl is $avatarUrl")
        val viewSize = ViewSizeResolver(imageView)
        val imageLoader = requireActivity().imageLoader
        val request = ImageRequest.Builder(requireActivity())
            .target(imageView)
            .data(avatarUrl)
            .placeholder(R.drawable.ic_user_placeholder)
            .error(R.drawable.ic_broken_image)
            .scale(Scale.FIT)
            .size(viewSize)
            .precision(Precision.EXACT)
            .transformations(CircleCropTransformation())
            .build()

        if (avatarUrl != null) {
            imageLoader.enqueue(request)
        } else {
            imageView.setImageResource(R.drawable.ic_user_placeholder)
        }
    }

    fun getIssueId() {
        val args : IssueDetailFragmentArgs? = arguments?.let { IssueDetailFragmentArgs.fromBundle(it) }
        val issueId = args?.issueId ?: null
    }

    companion object {
        const val LONG_ARG = 0
    }
}