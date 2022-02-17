package com.example.githubissues.issueslist

import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import timber.log.Timber

/**
 * Adds custom back navigation when pressing system Back in opened slidable pane.
 * Add the callback to the OnBackPressedDispatcher using addCallback().
 */
class TwoPaneOnBackPressedCallback(
    private val slidingPaneLayout: SlidingPaneLayout,
    private val issuesListToolbarTitle: () -> Unit,
    private val issueDetailsToolbarTitle: () -> Unit
) :
    OnBackPressedCallback(
        /*
        Set the default 'enabled' state to true only if it is slidable (i.e., the panes
        are overlapping) and open (i.e., the detail pane is visible).
         */
        slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
    ), SlidingPaneLayout.PanelSlideListener {

    init {
        slidingPaneLayout.addPanelSlideListener(this)
    }

    override fun handleOnBackPressed() {
        // Return to the list pane when the system back button is pressed
        slidingPaneLayout.closePane()
        issuesListToolbarTitle()
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        Timber.d("onPanelSlide is called")
    }

    override fun onPanelOpened(panel: View) {
        Timber.d("onPanelOpened is called")
        // Intercept the system back button when the detail pane becomes visible
        isEnabled = true
        issueDetailsToolbarTitle()

    }

    override fun onPanelClosed(panel: View) {
        Timber.d("onPanelClosed is called")
        // Disable intercepting the system back button when the user returns to the list pane
        isEnabled = false
        issuesListToolbarTitle()
    }

}