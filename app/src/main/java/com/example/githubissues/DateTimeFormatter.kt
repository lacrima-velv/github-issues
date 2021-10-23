package com.example.githubissues

import android.text.format.DateUtils.*
import java.text.SimpleDateFormat
import java.util.*

object DateTimeFormatter {

    private const val INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    private const val MILLIS_IN_ONE_DAY = 86400000L

    private fun getTimeMillisFromInputDate(dateValue: String): Long {
        val parser = SimpleDateFormat(INPUT_DATE_FORMAT, Locale.getDefault())
        parser.timeZone = TimeZone.getTimeZone("UTC")
        return parser.parse(dateValue)?.time ?: 0
    }

//    fun diffDates(dateValue: String): String {
//        val inputDateMillis = getTimeMillisFromInputDate(dateValue)
//
//        val currentMillis = System.currentTimeMillis() // Current time in millis since midnight, January 1, 1970, UTC
//        val diffMillis = currentMillis.minus(inputDateMillis)
//
//        return if (diffMillis < MILLIS_IN_ONE_DAY) {
//            "today"
//        } else if (diffMillis > MILLIS_IN_ONE_DAY && diffMillis < MILLIS_IN_ONE_DAY * 2) {
//            "yesterday"
//        } else {
//            kotlin.math.floor((diffMillis / MILLIS_IN_ONE_DAY).toDouble()).toInt().toString() + " days ago"
//        }
//
//    }

    fun getRelativeTime(dateValue: String): String {
        val time = getTimeMillisFromInputDate(dateValue)
        val now = System.currentTimeMillis()
        val minResolution = MINUTE_IN_MILLIS
        return getRelativeTimeSpanString(time, now, minResolution).toString()
    }

    }

//    fun convertDateToPeriod(date: String) {
//        val defaultCalendar = Calendar.getInstance() // Represents current time
//
//        val year = date.substring(0, 4).toInt()
//        val month = date.substring(5, 7).toInt()
//        val day = date.substring(8, 10).toInt()
//        val hour = date.substring(11, 13).toInt()
//        val minute = date.substring(14, 16).toInt()
//        val second = date.substring(17, 19).toInt()
//
//        val calendarWithDate = Calendar.getInstance().apply {
//            // TODO add cases when the first num is 0
//            add(Calendar.DAY_OF_MONTH, day)
//            add(Calendar.MONTH, month)
//            add(Calendar.YEAR, year)
//            set(Calendar.HOUR_OF_DAY, hour)
//            set(Calendar.MINUTE, minute)
//            set(Calendar.SECOND, second)
//        }
//        val millisCurrent = defaultCalendar.timeInMillis
//        val millisWhenUpdated = calendarWithDate.timeInMillis
//        val diffMillis = millisCurrent - millisWhenUpdated
//
//        //TODO Convert millis to days

   // }
//}