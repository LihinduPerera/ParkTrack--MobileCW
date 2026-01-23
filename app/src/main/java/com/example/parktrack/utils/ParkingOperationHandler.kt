package com.example.parktrack.utils

import android.content.Context
import com.example.parktrack.data.model.QRCodeData
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.repository.ParkingSessionRepository
import com.google.firebase.Timestamp

/**
 * Complete parking operation handler with validation and error handling
 */
class ParkingOperationHandler(
    private val repository: ParkingSessionRepository,
    private val context: Context
) {
    
    /**
     * Handle entry operation with validation
     */
    suspend fun handleEntry(
        driverId: String,
        qrData: QRCodeData,
        adminId: String,
        adminName: String,
        gateLocation: String
    ): Result<String> = runCatching {
        // Validate QR code
        val qrValidation = ParkingValidation.validateQRCode(qrData)
        if (!qrValidation.first) {
            ParkingErrorHandler.logValidationError(qrValidation.second, "QR Validation")
            throw IllegalArgumentException(qrValidation.second)
        }
        
        // Check for existing active session
        val existingSession = repository.getActiveSessionForDriver(driverId).getOrNull()
        val entryValidation = ParkingValidation.validateEntry(driverId, qrData, existingSession)
        if (!entryValidation.first) {
            ParkingErrorHandler.logValidationError(entryValidation.second, "Entry Validation")
            throw IllegalArgumentException(entryValidation.second)
        }
        
        // Create new session
        val session = ParkingSession(
            id = java.util.UUID.randomUUID().toString(),
            driverId = driverId,
            driverName = qrData.userId, // Will be updated with actual name
            vehicleNumber = qrData.vehicleNumber,
            entryTime = Timestamp.now(),
            gateLocation = gateLocation,
            scannedByAdminId = adminId,
            adminName = adminName,
            status = "ACTIVE",
            qrCodeUsed = qrData.toQRString()
        )
        
        val result = repository.createSession(session)
        if (result.isFailure) {
            val error = result.exceptionOrNull() as? Exception ?: Exception("Unknown")
            val errorMsg = ParkingErrorHandler.handleSessionError(error, "entry")
            throw Exception(errorMsg)
        }
        
        // Vibrate success
        VibrationHelper.vibrateSuccess(context)
        ParkingErrorHandler.logInfo("Entry recorded for driver: $driverId", "Entry")
        
        result.getOrThrow()
    }
    
    /**
     * Handle exit operation with validation
     */
    suspend fun handleExit(
        driverId: String,
        qrData: QRCodeData,
        adminId: String,
        gateLocation: String
    ): Result<String> = runCatching {
        // Validate QR code
        val qrValidation = ParkingValidation.validateQRCode(qrData)
        if (!qrValidation.first) {
            ParkingErrorHandler.logValidationError(qrValidation.second, "QR Validation")
            throw IllegalArgumentException(qrValidation.second)
        }
        
        // Get active session
        val activeSession = repository.getActiveSessionForDriver(driverId).getOrNull()
        val exitValidation = ParkingValidation.validateExit(driverId, qrData, activeSession)
        if (!exitValidation.first) {
            ParkingErrorHandler.logValidationError(exitValidation.second, "Exit Validation")
            throw IllegalArgumentException(exitValidation.second)
        }
        
        if (activeSession == null) {
            throw Exception("No active session found")
        }
        
        // Check reasonable duration
        val exitTime = Timestamp.now()
        val durationValid = ParkingValidation.isReasonableDuration(
            activeSession.entryTime!!,
            exitTime
        )
        
        if (!durationValid) {
            throw Exception("Invalid parking duration. Please check entry/exit times.")
        }
        
        // Complete session
        val result = repository.completeSession(activeSession.id, exitTime)
        if (result.isFailure) {
            val error = result.exceptionOrNull() as? Exception ?: Exception("Unknown")
            val errorMsg = ParkingErrorHandler.handleSessionError(error, "exit")
            throw Exception(errorMsg)
        }
        
        // Vibrate success
        VibrationHelper.vibrateSuccess(context)
        ParkingErrorHandler.logInfo("Exit recorded for driver: $driverId", "Exit")
        
        activeSession.id
    }
    
    /**
     * Handle manual exit with validation
     */
    suspend fun handleManualExit(
        sessionId: String,
        adminId: String
    ): Result<String> = runCatching {
        // Get session
        val session = repository.getSessionById(sessionId).getOrNull()
            ?: throw Exception("Session not found")
        
        if (session.status != "ACTIVE") {
            throw Exception("Session is not active")
        }
        
        // Check reasonable duration
        val exitTime = Timestamp.now()
        val durationValid = ParkingValidation.isReasonableDuration(
            session.entryTime!!,
            exitTime
        )
        
        if (!durationValid) {
            throw Exception("Invalid parking duration.")
        }
        
        // Complete session
        val result = repository.completeSession(sessionId, exitTime)
        if (result.isFailure) {
            throw result.exceptionOrNull() ?: Exception("Failed to complete session")
        }
        
        // Vibrate success
        VibrationHelper.vibrateSuccess(context)
        ParkingErrorHandler.logInfo("Manual exit recorded by admin: $adminId", "ManualExit")
        
        sessionId
    }
}
