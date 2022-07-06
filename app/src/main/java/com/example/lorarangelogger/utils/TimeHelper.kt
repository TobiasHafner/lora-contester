package com.example.lorarangelogger.utils

import java.text.SimpleDateFormat

object TimeHelper {

    private val timeFormatter = SimpleDateFormat.getTimeInstance()


    fun getTime(time: Long): String {
        return timeFormatter.format(time)
    }

    /**
     * Returns for example 3 h 15 min
     */
    fun getPreciseDuration(duration: Long): String {
        return when {
            duration < 1000 -> "${duration}ms"
            duration < 60_000 -> "${duration / 1000}s ${(duration % 1000)}ms"
            duration < 60 * 60_000 -> "${duration / 60_000}min ${(duration % 60_000) / 1000}s"
            duration < 24 * 60 * 60_000 -> "${duration / (60 * 60_000)}h ${(duration % (60 * 60_000)) / 60_000}min"
            else -> "${duration / (24 * 60 * 60_000)}d ${(duration % (24 * 60 * 60_000)) / (60 * 60_000)}h"
        }
    }
}