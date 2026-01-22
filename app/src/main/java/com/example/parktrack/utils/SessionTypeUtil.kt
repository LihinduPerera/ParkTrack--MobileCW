package com.example.parktrack.utils

import com.example.parktrack.data.model.ParkingSession

/**
 * Utility functions for determining parking session types
 */
object SessionTypeUtil {
    
    /**
     * Determine if the session is an entry scan
     */
    fun isEntrySession(session: ParkingSession?): Boolean {
        return session == null || session.status == "ACTIVE"
    }
    
    /**
     * Determine if the session is an exit scan
     */
    fun isExitSession(session: ParkingSession?): Boolean {
        return session != null && session.status == "COMPLETED" && session.exitTime != null
    }
    
    /**
     * Get session type string
     */
    fun getSessionTypeString(session: ParkingSession?): String {
        return if (isEntrySession(session)) "ENTRY" else "EXIT"
    }
    
    /**
     * Get session type display text
     */
    fun getSessionTypeDisplayText(sessionType: String): String {
        return when (sessionType) {
            "ENTRY" -> "Entry Recorded"
            "EXIT" -> "Exit Recorded"
            else -> "Scan Completed"
        }
    }
    
    /**
     * Format session info for display
     */
    fun formatSessionInfo(session: ParkingSession): String {
        val time = ParkingHelper.formatTimestamp(session.entryTime)
        val gate = session.gateLocation
        return "$gate - $time"
    }
}
