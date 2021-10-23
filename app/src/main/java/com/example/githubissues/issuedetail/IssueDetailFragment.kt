package com.example.githubissues.issuedetail

import android.opengl.Visibility
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModelProvider
import coil.imageLoader
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.transform.CircleCropTransformation
import coil.transform.Transformation
import com.example.githubissues.DateTimeFormatter.getRelativeTime
import com.example.githubissues.Injection
import com.example.githubissues.MainViewModel
import com.example.githubissues.R
import com.example.githubissues.databinding.FragmentIssueDetailBinding
import com.example.githubissues.model.Issue
import timber.log.Timber

class IssueDetailFragment : Fragment() {

    private lateinit var binding: FragmentIssueDetailBinding
    private lateinit var viewModel: MainViewModel
    private lateinit var args: IssueDetailFragmentArgs

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        args = IssueDetailFragmentArgs.fromBundle(requireArguments())
        Timber.d("Got an issue id ${args.issueId}")

        viewModel = ViewModelProvider(this, Injection.provideViewModelFactory(
            context = requireActivity()
        )).get(MainViewModel::class.java)

        viewModel.getIssueDetails(args.issueId)

        binding = FragmentIssueDetailBinding.inflate(layoutInflater)

        viewModel.currentIssueDetails.observe(viewLifecycleOwner) {
            Timber.d("Current issue title: ${viewModel.currentIssueDetails.value?.title}")
            binding.bindUi(viewModel.currentIssueDetails.value)
        }

        return binding.root
    }

    private fun FragmentIssueDetailBinding.bindUi(currentIssueDetails: Issue?) {
        issueDetailsState.text = getString(R.string.issue_state, currentIssueDetails?.state ?: R.string.unknown_lower_case)
        issueDetailsNumber.text = getString(R.string.issue_number, currentIssueDetails?.number)
        issueDetailsTitle.text = currentIssueDetails?.title ?: getString(R.string.unknown_upper_case)
        issueDetailsAuthor.text = currentIssueDetails?.user?.userLogin ?: getString(R.string.unknown_upper_case)

        // Show Author's avatar only if we got his name
//        if (currentIssueDetails?.user?.userLogin == null) {
//            issueDetailsAuthorAvatar.visibility = View.INVISIBLE
//        }
        //issueDetailsAuthorAvatar.isVisible = currentIssueDetails?.user?.userLogin != null

        issueDetailsBody.text = currentIssueDetails?.body ?: getString(R.string.no_description)
        issueDetailsAssignee.text = currentIssueDetails?.assignee?.assigneeLogin ?: getString(R.string.no_one_assignee)

        // Show Assignee's avatar only if we got his name
//        if (currentIssueDetails?.assignee?.assigneeLogin == null) {
//            issueDetailsAssigneeAvatar.visibility = View.INVISIBLE
//        }

        //issueDetailsAssigneeAvatar.isVisible = currentIssueDetails?.assignee?.assigneeLogin != null

        issueDetailsUpdatedDate.text = currentIssueDetails?.updatedAt?.
            let { getRelativeTime(it) } ?: getString(R.string.unknown_lower_case)
        issueDetailsCreatedDate.text = currentIssueDetails?.createdAt?.
            let { getRelativeTime(it) } ?: getString(R.string.unknown_lower_case)

        // Get Author's avatar
        getAvatar(issueDetailsAuthorAvatar, currentIssueDetails?.user?.userAvatarUrl)

        // Get Assignee's avatar
        getAvatar(issueDetailsAssigneeAvatar, currentIssueDetails?.assignee?.assigneeAvatarUrl)
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