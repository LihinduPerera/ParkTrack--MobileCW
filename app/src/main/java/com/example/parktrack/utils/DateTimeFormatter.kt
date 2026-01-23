package com.example.parktrack.utils

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

/**
 * Centralized date/time formatting utility for consistent time display across app
 */
object DateTimeFormatter {
    
    /**
     * Format time as "2:30 PM"
     */
    fun formatTime(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Format date and time as "Jan 15, 2:30 PM"
     */
    fun formatDateTime(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Format full timestamp as "Jan 15, 2026 2:30:45 PM"
     */
    fun formatFullDateTime(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("MMM d, yyyy h:mm:ss a", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Format duration in minutes as "2h 15m" or "45m"
     */
    fun formatDuration(minutes: Long): String {
        return when {
            minutes < 1 -> "< 1m"
            minutes < 60 -> "${minutes}m"
            else -> {
                val hours = minutes / 60
                val mins = minutes % 60
                if (mins > 0) "${hours}h ${mins}m" else "${hours}h"
            }
        }
    }
    
    /**
     * Format duration from two timestamps as "2h 15m"
     */
    fun formatDurationBetween(start: Timestamp, end: Timestamp): String {
        val durationMs = abs(end.toDate().time - start.toDate().time)
        val minutes = (durationMs / 60000).toInt()
        return formatDuration(minutes.toLong())
    }
    
    /**
     * Get relative time as "2 mins ago", "1 hour ago", "3 days ago"
     */
    fun getRelativeTime(timestamp: Timestamp): String {
        val now = System.currentTimeMillis()
        val then = timestamp.toDate().time
        val diffMs = abs(now - then)
        
        return when {
            diffMs < 60_000 -> "just now"
            diffMs < 120_000 -> "1 min ago"
            diffMs < 3_600_000 -> "${diffMs / 60_000} mins ago"
            diffMs < 7_200_000 -> "1 hour ago"
            diffMs < 86_400_000 -> "${diffMs / 3_600_000} hours ago"
            diffMs < 172_800_000 -> "1 day ago"
            else -> {
                val days = diffMs / 86_400_000
                if (days <= 7) "$days days ago" else formatDateTime(timestamp)
            }
        }
    }
    
    /**
     * Get countdown format as "5s", "1m 30s" etc.
     */
    fun formatCountdown(milliseconds: Long): String {
        return when {
            milliseconds <= 0 -> "0s"
            milliseconds < 1_000 -> "${milliseconds / 100}00ms"
            milliseconds < 60_000 -> "${milliseconds / 1_000}s"
            else -> {
                val minutes = milliseconds / 60_000
                val seconds = (milliseconds % 60_000) / 1_000
                "${minutes}m ${seconds}s"
            }
        }
    }
    
    /**
     * Format date only as "Jan 15, 2026"
     */
    fun formatDateOnly(timestamp: Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
        return sdf.format(date)
    }
    
    /**
     * Check if timestamp is today
     */
    fun isToday(timestamp: Timestamp): Boolean {
        val date = timestamp.toDate()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        
        calendar.time = date
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        
        return today == day
    }
    
    /**
     * Format relative date - "Today 2:30 PM", "Yesterday 2:30 PM", "Jan 15, 2:30 PM"
     */
    fun formatRelativeDateTime(timestamp: Timestamp): String {
        return when {
            isToday(timestamp) -> "Today " + formatTime(timestamp)
            isYesterday(timestamp) -> "Yesterday " + formatTime(timestamp)
            else -> formatDateTime(timestamp)
        }
    }
    
    /**
     * Check if timestamp is yesterday
     */
    private fun isYesterday(timestamp: Timestamp): Boolean {
        val date = timestamp.toDate()
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        
        calendar.time = date
        val day = calendar.get(Calendar.DAY_OF_YEAR)
        
        return today - day == 1
    }
}
