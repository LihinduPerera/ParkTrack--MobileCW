package com.example.parktrack.utils

import android.graphics.Bitmap
import android.util.Base64
import com.example.parktrack.data.model.QRCodeData
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.security.MessageDigest

object QRCodeGenerator {
    
    /**
     * Generate QR code bitmap from string
     * @param data The data to encode in the QR code
     * @param size The size of the generated QR code (default 512x512)
     * @return Bitmap containing the QR code or null if generation fails
     */
    fun generateQRCode(data: String, size: Int = 512): Bitmap? = try {
        val writer = QRCodeWriter()
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val bitMatrix = writer.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
        
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        
        bitmap
    } catch (e: Exception) {
        null
    }
    
    /**
     * Generate security hash for QR code
     * @param userId The user ID
     * @param timestamp The timestamp
     * @return Base64 encoded SHA-256 hash
     */
    fun generateSecurityHash(userId: String, timestamp: Long): String = try {
        val input = "$userId|$timestamp|PARKTRACK_SECRET_KEY"
        val messageDigest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = messageDigest.digest(input.toByteArray())
        Base64.encodeToString(hashedBytes, Base64.NO_WRAP)
    } catch (e: Exception) {
        ""
    }
    
    /**
     * Create QRCodeData object with current timestamp (old method for backward compatibility)
     * @param userId The user ID
     * @param vehicleNumber The vehicle number
     * @param qrType The type of QR code - "ENTRY" or "EXIT"
     * @return QRCodeData object with generated security hash
     */
    fun createQRCodeData(userId: String, vehicleNumber: String, qrType: String = "ENTRY"): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val hash = generateSecurityHash(userId, timestamp)
        
        return QRCodeData(
            userId = userId,
            vehicleNumber = vehicleNumber,
            timestamp = timestamp,
            securityHash = hash,
            qrType = qrType
        )
    }
    
    /**
     * Create QRCodeData object with vehicle details and current timestamp
     * @param userId The user ID
     * @param vehicleNumber The vehicle number
     * @param vehicleId The vehicle ID
     * @param vehicleModel The vehicle model
     * @param vehicleColor The vehicle color
     * @param qrType The type of QR code - "ENTRY" or "EXIT"
     * @return QRCodeData object with generated security hash
     */
    fun createQRCodeData(
        userId: String, 
        vehicleNumber: String,
        vehicleId: String = "",
        vehicleModel: String = "",
        vehicleColor: String = "",
        qrType: String = "ENTRY"
    ): QRCodeData {
        val timestamp = System.currentTimeMillis()
        val hash = generateSecurityHash(userId, timestamp)
        
        return QRCodeData(
            userId = userId,
            vehicleNumber = vehicleNumber,
            vehicleId = vehicleId,
            vehicleModel = vehicleModel,
            vehicleColor = vehicleColor,
            timestamp = timestamp,
            securityHash = hash,
            qrType = qrType
        )
    }
}
