package com.example.englishword.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/**
 * Utility object for date-related operations.
 */
object DateUtils {

    private const val DATE_FORMAT = "yyyy-MM-dd"

    /**
     * Gets today's date as a formatted string (yyyy-MM-dd).
     *
     * @return Today's date string
     */
    fun getTodayString(): String {
        return LocalDate.now().format(DateTimeFormatter.ofPattern(DATE_FORMAT))
    }

    /**
     * Calculates the study streak based on the last study date.
     *
     * @param lastDate The last study date string (yyyy-MM-dd), or null if never studied
     * @param today Today's date string (yyyy-MM-dd)
     * @return The streak count (0 if streak is broken, unchanged if same day, +1 if consecutive)
     */
    fun calculateStreak(lastDate: String?, today: String): Int {
        if (lastDate == null) {
            return 1 // First day of study
        }

        return try {
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
            val lastLocalDate = LocalDate.parse(lastDate, formatter)
            val todayLocalDate = LocalDate.parse(today, formatter)

            val daysDifference = ChronoUnit.DAYS.between(lastLocalDate, todayLocalDate)

            when {
                daysDifference == 0L -> 0 // Same day, don't increment (return 0 to indicate no change needed)
                daysDifference == 1L -> 1 // Consecutive day, increment by 1
                else -> -1 // Streak broken (more than 1 day gap), return -1 to indicate reset
            }
        } catch (e: Exception) {
            1 // On parsing error, treat as first day
        }
    }

    /**
     * Updates the streak count based on the last study date.
     *
     * @param currentStreak The current streak count
     * @param lastDate The last study date string (yyyy-MM-dd), or null if never studied
     * @param today Today's date string (yyyy-MM-dd)
     * @return The updated streak count
     */
    fun updateStreak(currentStreak: Int, lastDate: String?, today: String): Int {
        val change = calculateStreak(lastDate, today)
        return when (change) {
            0 -> currentStreak // Same day, keep current streak
            1 -> currentStreak + 1 // Consecutive day, increment
            else -> 1 // Streak broken or first day, reset to 1
        }
    }

    /**
     * Formats a timestamp to a date string.
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted date string (yyyy-MM-dd)
     */
    fun formatTimestamp(timestamp: Long): String {
        val sdf = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    /**
     * Checks if a given date string is today.
     *
     * @param dateString The date string to check (yyyy-MM-dd)
     * @return True if the date is today
     */
    fun isToday(dateString: String): Boolean {
        return dateString == getTodayString()
    }

    /**
     * Checks if a given date string is yesterday.
     *
     * @param dateString The date string to check (yyyy-MM-dd)
     * @return True if the date is yesterday
     */
    fun isYesterday(dateString: String): Boolean {
        return try {
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
            val date = LocalDate.parse(dateString, formatter)
            val yesterday = LocalDate.now().minusDays(1)
            date == yesterday
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the number of days since a given date.
     *
     * @param dateString The date string (yyyy-MM-dd)
     * @return Number of days since the date, or -1 if parsing fails
     */
    fun daysSince(dateString: String): Long {
        return try {
            val formatter = DateTimeFormatter.ofPattern(DATE_FORMAT)
            val date = LocalDate.parse(dateString, formatter)
            ChronoUnit.DAYS.between(date, LocalDate.now())
        } catch (e: Exception) {
            -1L
        }
    }
}
