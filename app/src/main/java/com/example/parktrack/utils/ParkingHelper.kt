package com.example.parktrack.utils

import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ParkingHelper {
    
    /**
     * Format timestamp to readable date and time string
     * @param timestamp The Firebase timestamp
     * @param format The date format (default: "dd/MM/yyyy HH:mm")
     * @return Formatted date string
     */
    fun formatTimestamp(timestamp: Timestamp?, format: String = "dd/MM/yyyy HH:mm"): String {
        return if (timestamp != null) {
            val dateFormat = SimpleDateFormat(format, Locale.getDefault())
            dateFormat.format(timestamp.toDate())
        } else {
            "N/A"
        }
    }
    
    /**
     * Calculate parking duration in user-friendly format
     * @param entryTime Entry timestamp
     * @param exitTime Exit timestamp
     * @return Duration string (e.g., "1h 30m")
     */
    fun formatDuration(entryTime: Timestamp?, exitTime: Timestamp?): String {
        return if (entryTime != null && exitTime != null) {
            val entryMillis = entryTime.toDate().time
            val exitMillis = exitTime.toDate().time
            val totalMinutes = (exitMillis - entryMillis) / (1000 * 60)
            
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            
            when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        } else {
            "N/A"
        }
    }
    
    /**
     * Get session status color for UI display
     * @param status The session status (ACTIVE or COMPLETED)
     * @return Color resource ID
     */
    fun getStatusColor(status: String): Long {
        return when (status) {
            "ACTIVE" -> 0xFF4CAF50 // Green
            "COMPLETED" -> 0xFF2196F3 // Blue
            else -> 0xFF9E9E9E // Gray
        }
    }
    
    /**
     * Validate vehicle number format
     * @param vehicleNumber The vehicle number to validate
     * @return True if valid, false otherwise
     */
    fun isValidVehicleNumber(vehicleNumber: String): Boolean {
        // Basic validation: alphanumeric, 3-10 characters
        return vehicleNumber.isNotEmpty() && 
               vehicleNumber.length in 3..10 && 
               vehicleNumber.matches(Regex("^[A-Z0-9]+$"))
    }
    
    /**
     * Check if a parking session is active (currently ongoing)
     * @param session The parking session
     * @return True if session is active, false otherwise
     */
    fun isSessionActive(session: ParkingSession): Boolean {
        return session.status == "ACTIVE" && session.exitTime == null
    }
    
    /**
     * Get session summary string for display
     * @param session The parking session
     * @return Summary string
     */
    fun getSessionSummary(session: ParkingSession): String {
        val durationStr = if (session.durationMinutes > 0) {
            "${session.durationMinutes} min"
        } else {
            "Ongoing"
        }
        
        return "Vehicle: ${session.vehicleNumber} | Duration: $durationStr | Gate: ${session.gateLocation}"
    }
}
