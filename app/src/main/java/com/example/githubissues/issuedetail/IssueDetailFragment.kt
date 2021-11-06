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
import androidx.lifecycle.lifecycleScope
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.transform.CircleCropTransformation
import com.example.githubissues.*
import com.example.githubissues.Utils.getRelativeTime
import com.example.githubissues.databinding.FragmentIssueDetailBinding
import com.example.githubissues.model.Issue
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*

class IssueDetailFragment : Fragment() {

    private lateinit var binding: FragmentIssueDetailBinding
    private lateinit var viewModelFactory: MainViewModelFactory
    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIssueDetailBinding.inflate(layoutInflater)

        val id = arguments?.getLong("issueId")
        Timber.d("issue id is $id")

        viewModelFactory =  MainViewModelFactory(Application(), this)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory).get(MainViewModel::class.java)

        id?.let {
            viewModel.fgetIssueDetails(it)
        } ?: binding.showPlaceholder()

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.fcurrentIssueDetails.collectLatest {
                binding.showIssueDetails(it)
            }
        }

        return binding.root
    }

    private fun FragmentIssueDetailBinding.getNormalViews() = listOf(
        issueDetailsCreated,
        issueDetailsUpdated,
        issueDetailsAssignedTo,
        issueDetailsPostedBy,
        issueDetailsTitle,
        issueDetailsNumber,
        issueDetailsState,
        issueDetailsBody,
        issueDetailsAuthor,
        issueDetailsAssignee,
        issueDetailsCreatedDate,
        issueDetailsUpdatedDate,
        issueDetailsAssigneeAvatar,
        issueDetailsAuthorAvatar
    )

    private fun FragmentIssueDetailBinding.getPlaceholderViews() = listOf(
        emptyIssueDetailsPlaceholderImage,
        emptyIssueDetailsPlaceholderText
    )

    private fun FragmentIssueDetailBinding.showPlaceholder() {
        /*
        If couldn't get arguments, probably, there are no issues in issues list.
        Then show placeholder for empty state.
        */
        for (view in getPlaceholderViews()) {
            view.isVisible = true
        }

        for (view in getNormalViews()) {
            view.isVisible = false
        }
    }

    private fun FragmentIssueDetailBinding.showIssueDetails(currentIssueDetails: Issue?) {
        if (currentIssueDetails?.state == null) {
            showPlaceholder()
        } else {
            // Remove placeholder if it was visible
            for (view in getPlaceholderViews()) {
                view.isVisible = false
            }

            for (view in getNormalViews()) {
                view.isVisible = true
            }

            // If we got arguments, display issue's details
            issueDetailsState.text = getString(
                R.string.issue_state, currentIssueDetails?.state?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            )
            issueDetailsNumber.text = getString(R.string.issue_number, currentIssueDetails?.number)
            issueDetailsAuthor.text = currentIssueDetails.user?.userLogin
            issueDetailsTitle.text = currentIssueDetails.title

            issueDetailsBody.text = currentIssueDetails.body ?: getString(R.string.no_description)
            issueDetailsAssignee.text = currentIssueDetails.assignee?.assigneeLogin ?: getString(R.string.no_one_assignee)

            issueDetailsUpdatedDate.text = currentIssueDetails.updatedAt?.
            let { getRelativeTime(it) } ?: getString(R.string.unknown)
            issueDetailsCreatedDate.text = currentIssueDetails.createdAt?.
            let { getRelativeTime(it) } ?: getString(R.string.unknown)

            // Get Author's avatar
            getAvatar(issueDetailsAuthorAvatar, currentIssueDetails.user?.userAvatarUrl)

            // Get Assignee's avatar
            getAvatar(issueDetailsAssigneeAvatar, currentIssueDetails.assignee?.assigneeAvatarUrl)
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

}