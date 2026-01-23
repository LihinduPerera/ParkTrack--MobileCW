package com.example.parktrack.utils

import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.QRCodeData
import com.google.firebase.Timestamp

/**
 * Validation logic for parking entry/exit operations
 */
object ParkingValidation {
    
    /**
     * Validate entry operation
     * @return Pair<isValid, errorMessage>
     */
    fun validateEntry(
        driverId: String,
        qrData: QRCodeData,
        activeSession: ParkingSession?
    ): Pair<Boolean, String> {
        // Check if driver already has active session
        if (activeSession != null) {
            return Pair(false, "Driver already has an active parking session. Exit first.")
        }
        
        // Validate QR data matches driver
        if (qrData.userId != driverId) {
            return Pair(false, "QR code doesn't match driver ID")
        }
        
        // Validate vehicle number
        if (qrData.vehicleNumber.isEmpty()) {
            return Pair(false, "Invalid vehicle number in QR code")
        }
        
        return Pair(true, "")
    }
    
    /**
     * Validate exit operation
     * @return Pair<isValid, errorMessage>
     */
    fun validateExit(
        driverId: String,
        qrData: QRCodeData,
        activeSession: ParkingSession?
    ): Pair<Boolean, String> {
        // Check if driver has active session
        if (activeSession == null) {
            return Pair(false, "No active parking session found for this driver")
        }
        
        // Check if QR data matches active session
        if (qrData.userId != activeSession.driverId) {
            return Pair(false, "QR code doesn't match active session")
        }
        
        if (activeSession.status != "ACTIVE") {
            return Pair(false, "Session is not active")
        }
        
        if (activeSession.entryTime == null) {
            return Pair(false, "Invalid session: missing entry time")
        }
        
        return Pair(true, "")
    }
    
    /**
     * Validate QR code
     * @return Pair<isValid, errorMessage>
     */
    fun validateQRCode(qrData: QRCodeData): Pair<Boolean, String> {
        // Check expiration
        val validation = QRCodeValidator.validateQRCode(qrData)
        
        return when (validation) {
            is ValidationResult.Valid -> Pair(true, "")
            is ValidationResult.Expired -> Pair(false, "QR code has expired. Generate a new one.")
            is ValidationResult.InvalidHash -> Pair(false, "QR code security verification failed")
            is ValidationResult.InvalidFormat -> Pair(false, "Invalid QR code format")
        }
    }
    
    /**
     * Check if parking duration is reasonable
     */
    fun isReasonableDuration(entryTime: Timestamp, exitTime: Timestamp): Boolean {
        val durationMs = exitTime.toDate().time - entryTime.toDate().time
        
        // Duration should be at least 1 minute and at most 24 hours
        val minDuration = 60 * 1000 // 1 minute
        val maxDuration = 24 * 60 * 60 * 1000 // 24 hours
        
        return durationMs in minDuration..maxDuration
    }
    
    /**
     * Format validation error for user display
     */
    fun formatErrorMessage(error: String): String {
        return when {
            error.contains("expired", ignoreCase = true) -> {
                "QR code has expired. Please ask the driver to generate a new QR code."
            }
            error.contains("security", ignoreCase = true) -> {
                "Invalid QR code. Security check failed. Please try again."
            }
            error.contains("active session", ignoreCase = true) -> {
                "Driver cannot enter. They already have an active parking session."
            }
            error.contains("No active", ignoreCase = true) -> {
                "Driver is not currently parked. Cannot record exit."
            }
            else -> error
        }
    }
}
