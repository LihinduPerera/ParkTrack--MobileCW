package com.example.parktrack.utils

import com.example.parktrack.data.model.QRCodeData
import java.util.UUID

/**
 * Test utilities for QR code scanning validation and debugging
 */
object QRCodeTestUtil {
    
    /**
     * Generate a test QR code string for development/testing
     */
    fun generateTestQRCode(userId: String = "", vehicleNumber: String = ""): String {
        val testUserId = userId.ifEmpty { UUID.randomUUID().toString() }
        val testVehicle = vehicleNumber.ifEmpty { "KA01AB1234" }
        val timestamp = System.currentTimeMillis()
        val hash = QRCodeGenerator.generateSecurityHash(testUserId, timestamp)
        
        return "PARKTRACK|$testUserId|$testVehicle|$timestamp|$hash"
    }
    
    /**
     * Validate QR string format
     */
    fun isValidQRFormat(qrString: String): Boolean {
        val parts = qrString.split("|")
        return parts.size == 5 && parts[0] == "PARKTRACK"
    }
    
    /**
     * Extract QR code parts for debugging
     */
    fun extractQRParts(qrString: String): Map<String, String> {
        val parts = qrString.split("|")
        return if (parts.size == 5) {
            mapOf(
                "prefix" to parts[0],
                "userId" to parts[1],
                "vehicleNumber" to parts[2],
                "timestamp" to parts[3],
                "hash" to parts[4]
            )
        } else {
            mapOf("error" to "Invalid QR format")
        }
    }
    
    /**
     * Test QR code validation pipeline
     */
    fun testQRValidation(qrString: String): Pair<Boolean, String> {
        val qrData = QRCodeData.fromQRString(qrString)
        if (qrData == null) {
            return Pair(false, "Failed to parse QR code")
        }
        
        val validation = QRCodeValidator.validateQRCode(qrData)
        val result = validation is ValidationResult.Valid
        val message = when (validation) {
            is ValidationResult.Valid -> "QR code is valid and not expired"
            is ValidationResult.Expired -> "QR code has expired"
            is ValidationResult.InvalidHash -> "QR code security hash is invalid"
            is ValidationResult.InvalidFormat -> "QR code format is invalid"
        }
        
        return Pair(result, message)
    }
}
