package com.example.parktrack.utils

import android.util.Log

/**
 * Centralized logging utility for parking management system
 */
object ParkingLogger {
    private const val TAG_PREFIX = "ParkTrack"
    
    object QRCode {
        private const val TAG = "$TAG_PREFIX-QR"
        
        fun onGenerated(userId: String, vehicleNumber: String) {
            Log.d(TAG, "QR Code generated for user: $userId, vehicle: $vehicleNumber")
        }
        
        fun onValidationSuccess(userId: String) {
            Log.d(TAG, "QR Code validated successfully for user: $userId")
        }
        
        fun onValidationFailed(reason: String) {
            Log.w(TAG, "QR Code validation failed: $reason")
        }
        
        fun onExpired(userId: String) {
            Log.i(TAG, "QR Code expired for user: $userId")
        }
        
        fun onError(error: Exception) {
            Log.e(TAG, "QR Code error: ${error.message}", error)
        }
    }
    
    object ParkingSession {
        private const val TAG = "$TAG_PREFIX-Session"
        
        fun onSessionCreated(sessionId: String, driverId: String) {
            Log.d(TAG, "Parking session created: $sessionId for driver: $driverId")
        }
        
        fun onSessionCompleted(sessionId: String, durationMinutes: Long) {
            Log.d(TAG, "Parking session completed: $sessionId, duration: ${durationMinutes}m")
        }
        
        fun onSessionFetched(count: Int) {
            Log.d(TAG, "Fetched $count parking sessions")
        }
        
        fun onListenerError(error: Exception) {
            Log.e(TAG, "Session listener error: ${error.message}", error)
        }
        
        fun onError(error: Exception) {
            Log.e(TAG, "Parking session error: ${error.message}", error)
        }
    }
    
    object ViewModel {
        private const val TAG = "$TAG_PREFIX-ViewModel"
        
        fun onUserDataLoaded(userId: String, userName: String) {
            Log.d(TAG, "User data loaded: $userId - $userName")
        }
        
        fun onSessionListenerStarted(driverId: String) {
            Log.d(TAG, "Session listener started for driver: $driverId")
        }
        
        fun onError(message: String, error: Exception) {
            Log.e(TAG, message, error)
        }
    }
}
