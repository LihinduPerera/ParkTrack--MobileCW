package com.example.parktrack.utils

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Centralized snackbar message management
 */
object SnackbarUtil {
    
    /**
     * Show success message
     */
    fun showSuccess(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                actionLabel = null
            )
        }
    }
    
    /**
     * Show error message
     */
    fun showError(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                actionLabel = null
            )
        }
    }
    
    /**
     * Show info message
     */
    fun showInfo(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        message: String,
        duration: SnackbarDuration = SnackbarDuration.Short
    ) {
        scope.launch {
            snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                actionLabel = null
            )
        }
    }
    
    /**
     * Show message with action button
     */
    fun showWithAction(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        message: String,
        actionLabel: String,
        onActionClick: () -> Unit,
        duration: SnackbarDuration = SnackbarDuration.Long
    ) {
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = message,
                duration = duration,
                actionLabel = actionLabel
            )
            if (result.name == "ActionPerformed") {
                onActionClick()
            }
        }
    }
    
    /**
     * Common success messages
     */
    object Messages {
        const val ENTRY_RECORDED = "Entry recorded successfully"
        const val EXIT_RECORDED = "Exit recorded"
        const val QR_GENERATED = "QR code generated"
        const val QR_COPIED = "QR code copied"
        const val SESSION_CREATED = "Session created"
        const val SESSION_COMPLETED = "Session completed"
        const val MANUAL_EXIT_RECORDED = "Manual exit recorded"
        const val REFRESH_COMPLETE = "Refreshed"
        
        fun entryRecordedForDriver(driverName: String) = "Entry recorded for $driverName"
        fun exitRecordedWithDuration(duration: String) = "Exit recorded - Duration: $duration"
        fun manualExitFor(driverName: String) = "Manual exit recorded for $driverName"
    }
}
