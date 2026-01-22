package com.example.parktrack.utils

import com.example.parktrack.data.model.QRCodeData
import java.security.MessageDigest
import java.util.Base64

object QRCodeValidator {
    private const val QR_EXPIRATION_MINUTES = 2L
    
    /**
     * Validate QR code format and expiration
     * @param qrCodeData The QR code data to validate
     * @return ValidationResult indicating if the QR code is valid
     */
    fun validateQRCode(qrCodeData: QRCodeData): ValidationResult {
        // Check if expired
        if (isExpired(qrCodeData.timestamp)) {
            return ValidationResult.Expired
        }
        
        // Verify hash
        if (!verifyHash(qrCodeData)) {
            return ValidationResult.InvalidHash
        }
        
        return ValidationResult.Valid
    }
    
    /**
     * Check if QR code is expired
     * @param timestamp The timestamp of the QR code generation
     * @return True if QR code is expired, false otherwise
     */
    fun isExpired(timestamp: Long): Boolean {
        val currentTime = System.currentTimeMillis()
        val expirationTimeMs = QR_EXPIRATION_MINUTES * 60 * 1000
        return (currentTime - timestamp) > expirationTimeMs
    }
    
    /**
     * Verify security hash of QR code
     * @param qrCodeData The QR code data to verify
     * @return True if hash is valid, false otherwise
     */
    fun verifyHash(qrCodeData: QRCodeData): Boolean = try {
        val expectedHash = QRCodeGenerator.generateSecurityHash(qrCodeData.userId, qrCodeData.timestamp)
        qrCodeData.securityHash == expectedHash
    } catch (e: Exception) {
        false
    }
    
    /**
     * Validate QR code string format and return parsed data
     * @param qrString The QR code string to validate
     * @return ValidationResult with optional QRCodeData if valid
     */
    fun validateAndParseQRCode(qrString: String): Pair<ValidationResult, QRCodeData?> {
        val qrCodeData = QRCodeData.fromQRString(qrString)
            ?: return Pair(ValidationResult.InvalidFormat, null)
        
        return Pair(validateQRCode(qrCodeData), qrCodeData)
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    object Expired : ValidationResult()
    object InvalidFormat : ValidationResult()
    object InvalidHash : ValidationResult()
}
