package com.example.parktrack.utils

import android.graphics.Bitmap
import com.example.parktrack.data.model.QRCodeData

/**
 * Manages QR code caching and session state
 */
data class QRCodeSession(
    val qrCodeData: QRCodeData,
    val bitmap: Bitmap?,
    val generatedAt: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (QRCodeConfig.QR_EXPIRATION_SECONDS * 1000)
) {
    
    /**
     * Check if this QR code session has expired
     */
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    /**
     * Get remaining validity time in seconds
     */
    fun getRemainingSeconds(): Int {
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        return remaining.toInt().coerceAtLeast(0)
    }
    
    /**
     * Get remaining validity time as percentage (0-1)
     */
    fun getRemainingPercentage(): Float {
        val remaining = (expiresAt - System.currentTimeMillis()).toFloat()
        val total = (QRCodeConfig.QR_EXPIRATION_SECONDS * 1000).toFloat()
        return (remaining / total).coerceIn(0f, 1f)
    }
}
