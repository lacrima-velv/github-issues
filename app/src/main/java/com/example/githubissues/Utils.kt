package com.example.githubissues

import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.text.format.DateUtils
import android.util.TypedValue
import androidx.lifecycle.ViewModelProvider
import androidx.savedstate.SavedStateRegistryOwner
import com.example.githubissues.api.GithubApiService
import com.example.githubissues.data.GithubRepository
import com.example.githubissues.db.IssuesDatabase
import java.text.SimpleDateFormat
import java.util.*


object Utils {
    // Used for time formatting
    private const val INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"

    private fun getTimeMillisFromInputDate(dateValue: String): Long {
        val parser = SimpleDateFormat(INPUT_DATE_FORMAT, Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        return parser.parse(dateValue)?.time ?: 0
    }

    /**
     * Converts time from [INPUT_DATE_FORMAT] to more user-friendly relative date/time like
     * "4 hour ago", "yesterday"
     */
    fun getRelativeTime(dateValue: String): String {
        val time = getTimeMillisFromInputDate(dateValue)
        val now = System.currentTimeMillis()
        val minResolution = DateUtils.MINUTE_IN_MILLIS
        return DateUtils.getRelativeTimeSpanString(time, now, minResolution).toString()
    }

    /**
     * Convert pixels to dp
     */
    val Int.toDp
        get() =  this / (Resources.getSystem().displayMetrics.density)

}