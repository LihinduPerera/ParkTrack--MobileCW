package com.example.parktrack.utils

/**
 * Constants and configuration for QR code operations
 */
object QRCodeConfig {
    
    // QR Code expiration time in seconds
    const val QR_EXPIRATION_SECONDS = 30L
    
    // QR Code bitmap size in pixels
    const val QR_CODE_SIZE = 512
    
    // QR Code refresh time in milliseconds
    const val QR_REFRESH_INTERVAL_MS = 1000L
    
    // Minimum time warning threshold in seconds
    const val LOW_TIME_THRESHOLD = 10
    
    // QR Code dialog close delay in milliseconds
    const val DIALOG_CLOSE_DELAY_MS = 300L
    
    /**
     * Calculate remaining time as percentage (0-1)
     * @param currentCountdown Current countdown in seconds
     * @param totalTime Total time in seconds
     * @return Progress as float (0-1)
     */
    fun getProgressPercentage(currentCountdown: Int, totalTime: Int = QR_EXPIRATION_SECONDS.toInt()): Float {
        return (currentCountdown.toFloat() / totalTime).coerceIn(0f, 1f)
    }
    
    /**
     * Check if we're in low time warning zone
     * @param countdown Current countdown in seconds
     * @return True if countdown is below threshold
     */
    fun isLowTimeWarning(countdown: Int): Boolean {
        return countdown <= LOW_TIME_THRESHOLD && countdown > 0
    }
    
    /**
     * Format countdown as string for display
     * @param countdown Countdown in seconds
     * @return Formatted string
     */
    fun formatCountdown(countdown: Int): String {
        return when {
            countdown > 60 -> "${countdown / 60}m ${countdown % 60}s"
            countdown > 0 -> "${countdown}s"
            else -> "Expired"
        }
    }
}
