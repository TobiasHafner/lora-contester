package com.example.lorarangelogger.utils

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object TimeHelper {
    private val dateFormatters = mapOf<Int, DateFormat>(
        DateFormat.SHORT to DateFormat.getDateInstance(DateFormat.SHORT),
        DateFormat.MEDIUM to DateFormat.getDateInstance(DateFormat.MEDIUM),
        DateFormat.LONG to DateFormat.getDateInstance(DateFormat.LONG))

    private val timeFormatter = SimpleDateFormat.getTimeInstance()
    private val dateTimeFormatter = SimpleDateFormat.getDateTimeInstance()

    fun getDateTime(time: Long): String {
        return dateTimeFormatter.format(time)
    }

    fun getTime(time: Long): String {
        return timeFormatter.format(time)
    }

    /**
     * Format is DateFormat.SHORT (default), DateFormat.MEDIUM, or DateFormat.LONG
     */
    fun getDate(time: Long, format: Int = DateFormat.SHORT): String {
        return dateFormatters[format]?.format(Date(time)) ?: time.toString()
    }

    /**
     * Returns for example 3 h
     */
    fun getDuration(duration: Long): String {
        return when {
            duration < 1000 -> "${duration}ms"
            duration < 60_000 -> "${duration / 1000}s"
            duration < 100 * 60_000 -> "${duration / 60_000}min"
            duration < 100 * 60 *60_000 -> "${duration / (60 * 60_000)}h"
            else -> "${duration / (24 * 60 * 60_000)}d"
        }
    }

    /**
     * Returns for example 3 h 15 min
     */
    fun getPreciseDuration(duration: Long): String {
        return when {
            duration < 1000 -> "${duration}ms"
            duration < 60_000 -> "${duration / 1000}s ${(duration % 1000)}ms"
            duration < 60 * 60_000 -> "${duration / 60_000}min ${(duration % 60_000) / 1000}s"
            duration < 24 * 60 *60_000 -> "${duration / (60 * 60_000)}h ${(duration % (60 * 60_000)) / 60_000}min"
            else -> "${duration / (24 * 60 * 60_000)}d ${(duration % (24 * 60 * 60_000)) / (60 * 60_000)}h"
        }
    }
}