package com.example.githubissues.issuedetail

import android.app.Application
import android.os.Bundle
import android.text.util.Linkify
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import coil.ImageLoader
import coil.imageLoader
import coil.request.Disposable
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Scale
import coil.size.ViewSizeResolver
import coil.transform.CircleCropTransformation
import com.example.githubissues.*
import com.example.githubissues.Utils.getRelativeTime
import com.example.githubissues.databinding.FragmentIssueDetailBinding
import com.example.githubissues.model.Issue
import io.noties.markwon.Markwon
import io.noties.markwon.image.AsyncDrawable
import io.noties.markwon.image.coil.CoilImagesPlugin
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.util.*
import java.util.regex.Pattern

class IssueDetailFragment : Fragment() {

    private lateinit var binding: FragmentIssueDetailBinding
    private lateinit var viewModelFactory: MainViewModelFactory
    private lateinit var viewModel: MainViewModel
    private lateinit var markwon: Markwon
    private lateinit var imageLoader: ImageLoader
    private lateinit var coilPlugin: CoilImagesPlugin

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentIssueDetailBinding.inflate(layoutInflater)

        val id = arguments?.getLong("issueId")
        Timber.d("issue id is $id")

        viewModelFactory =  MainViewModelFactory(Application(), this)

        viewModel = ViewModelProvider(requireActivity(), viewModelFactory)[MainViewModel::class.java]

        id?.let {
            viewModel.getIssueDetails(it)
        } ?: binding.showPlaceholder()

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.currentIssueDetails.collectLatest {
                binding.showIssueDetails(it)
            }
        }

        // Will be used for avatars and images inside issue's body
        imageLoader = requireActivity().imageLoader

        createCoilPlugin()

        // Will be used to parse markdown inside issue's body
        markwon = Markwon.builder(requireActivity())
            .usePlugin(coilPlugin)
            .build()

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
                R.string.issue_state, currentIssueDetails.state.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(
                        Locale.getDefault()
                    ) else it.toString()
                }
            )
            issueDetailsNumber.text = getString(R.string.issue_number, currentIssueDetails.number)
            issueDetailsAuthor.text = currentIssueDetails.user?.userLogin
            issueDetailsTitle.text = currentIssueDetails.title

            parseIssueDetailsBody(currentIssueDetails)

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

    private fun createCoilPlugin() {
        coilPlugin = CoilImagesPlugin.create(
            object : CoilImagesPlugin.CoilStore {
                override fun load(drawable: AsyncDrawable): ImageRequest {
                    return ImageRequest.Builder(requireActivity())
                        .defaults(imageLoader.defaults)
                        .data(drawable.destination)
                        .placeholder(R.drawable.ic_broken_image_2)
                        .error(R.drawable.ic_broken_image_2)
                        .build()
                }

                override fun cancel(disposable: Disposable) {
                    disposable.dispose()
                }
            },
            imageLoader
        )
    }

    private fun FragmentIssueDetailBinding.parseIssueDetailsBody(currentIssueDetails: Issue){
        // Parse markdown
        markwon.setMarkdown(issueDetailsBody,
            currentIssueDetails.body ?: getString(R.string.no_description))
        // Parse links without markdown
        val pattern = Pattern.compile("(http|https)\\S+")
        Linkify.addLinks(issueDetailsBody, pattern, "")
    }

}