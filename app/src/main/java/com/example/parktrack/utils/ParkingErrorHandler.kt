package com.example.parktrack.utils

import android.util.Log

/**
 * Error handling and logging for parking operations
 */
object ParkingErrorHandler {
    
    private const val TAG = "ParkTrack-Error"
    
    /**
     * Handle and log Firestore errors
     */
    fun handleFirestoreError(error: Exception, operation: String): String {
        Log.e(TAG, "$operation failed: ${error.message}", error)
        
        return when {
            error.message?.contains("permission", ignoreCase = true) == true -> {
                "Permission denied. Please check Firestore rules."
            }
            error.message?.contains("not found", ignoreCase = true) == true -> {
                "Resource not found."
            }
            error.message?.contains("network", ignoreCase = true) == true -> {
                "Network error. Please check your connection."
            }
            error.message?.contains("quota", ignoreCase = true) == true -> {
                "Operation quota exceeded. Please try again later."
            }
            error.message?.contains("already exists", ignoreCase = true) == true -> {
                "This resource already exists."
            }
            else -> "An error occurred: ${error.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Handle QR scanning errors
     */
    fun handleScanError(error: Exception): String {
        Log.e(TAG, "QR scanning error: ${error.message}", error)
        
        return when {
            error.message?.contains("permission", ignoreCase = true) == true -> {
                "Camera permission required for scanning."
            }
            error.message?.contains("camera", ignoreCase = true) == true -> {
                "Camera error. Please check your device camera."
            }
            else -> "Scanning error: ${error.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Handle session creation errors
     */
    fun handleSessionError(error: Exception, sessionType: String): String {
        Log.e(TAG, "Session $sessionType error: ${error.message}", error)
        
        return when {
            error.message?.contains("already has", ignoreCase = true) == true -> {
                "Driver already has an active session."
            }
            error.message?.contains("no active", ignoreCase = true) == true -> {
                "No active session found for this driver."
            }
            error.message?.contains("not found", ignoreCase = true) == true -> {
                "Session not found."
            }
            else -> "Failed to record $sessionType: ${error.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Log validation errors
     */
    fun logValidationError(error: String, context: String) {
        Log.w(TAG, "Validation error in $context: $error")
    }
    
    /**
     * Log information
     */
    fun logInfo(message: String, tag: String = "Info") {
        Log.i("$TAG-$tag", message)
    }
    
    /**
     * Log debug information
     */
    fun logDebug(message: String, tag: String = "Debug") {
        Log.d("$TAG-$tag", message)
    }
}
