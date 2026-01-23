package com.example.parktrack.utils

import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.Timestamp

/**
 * Handles session conflict scenarios (e.g., driver already parked)
 */
object SessionConflictHandler {
    
    /**
     * Check if driver already has an active session
     */
    fun hasActiveSession(activeSession: ParkingSession?): Boolean {
        return activeSession != null && activeSession.status == "ACTIVE"
    }
    
    /**
     * Format conflict message for display
     */
    fun formatConflictMessage(
        driverName: String,
        gateLocation: String,
        entryTime: Timestamp
    ): String {
        val timeString = DateTimeFormatter.formatRelativeDateTime(entryTime)
        return "$driverName is already parked at Gate $gateLocation since $timeString"
    }
    
    /**
     * Calculate how long driver has been parked
     */
    fun calculateParkedDuration(entryTime: Timestamp): String {
        val now = Timestamp.now()
        val durationMinutes = (now.toDate().time - entryTime.toDate().time) / 60000
        return DateTimeFormatter.formatDuration(durationMinutes)
    }
    
    /**
     * Determine conflict action
     */
    enum class ConflictAction {
        VIEW_SESSION,
        FORCE_NEW_ENTRY,
        CANCEL
    }
    
    /**
     * Result of conflict detection
     */
    data class ConflictResult(
        val hasConflict: Boolean,
        val conflictMessage: String = "",
        val existingSession: ParkingSession? = null,
        val suggestedAction: ConflictAction? = null
    )
    
    /**
     * Detect and format session conflict
     */
    fun detectConflict(
        activeSession: ParkingSession?,
        driverId: String,
        vehicleNumber: String
    ): ConflictResult {
        if (activeSession == null || activeSession.status != "ACTIVE") {
            return ConflictResult(hasConflict = false)
        }
        
        if (activeSession.driverId != driverId) {
            return ConflictResult(hasConflict = false)
        }
        
        if (activeSession.vehicleNumber != vehicleNumber) {
            return ConflictResult(hasConflict = false)
        }
        
        return ConflictResult(
            hasConflict = true,
            conflictMessage = formatConflictMessage(
                activeSession.driverName,
                activeSession.gateLocation,
                activeSession.entryTime!!
            ),
            existingSession = activeSession,
            suggestedAction = ConflictAction.FORCE_NEW_ENTRY
        )
    }
    
    /**
     * Get conflict resolution options
     */
    fun getResolutionOptions(conflict: ConflictResult): List<Pair<String, ConflictAction>> {
        if (!conflict.hasConflict) return emptyList()
        
        return listOf(
            "View Active Session" to ConflictAction.VIEW_SESSION,
            "Create New Entry" to ConflictAction.FORCE_NEW_ENTRY,
            "Cancel" to ConflictAction.CANCEL
        )
    }
}
