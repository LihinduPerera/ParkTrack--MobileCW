package com.example.parktrack.data.model

data class QRCodeData(
    val userId: String,
    val vehicleNumber: String,
    val timestamp: Long,
    val securityHash: String
) {
    // Format: "PARKTRACK|userId|vehicleNumber|timestamp|hash"
    fun toQRString(): String {
        return "PARKTRACK|$userId|$vehicleNumber|$timestamp|$securityHash"
    }
    
    companion object {
        fun fromQRString(qrString: String): QRCodeData? {
            val parts = qrString.split("|")
            if (parts.size != 5 || parts[0] != "PARKTRACK") return null
            
            return try {
                QRCodeData(
                    userId = parts[1],
                    vehicleNumber = parts[2],
                    timestamp = parts[3].toLong(),
                    securityHash = parts[4]
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}
