package com.example.parktrack.data.model

data class QRCodeData(
    val userId: String,
    val vehicleNumber: String,
    val timestamp: Long,
    val securityHash: String,
    val qrType: String = "ENTRY" // ENTRY or EXIT
) {
    // Format: "PARKTRACK|userId|vehicleNumber|timestamp|qrType|hash"
    fun toQRString(): String {
        return "PARKTRACK|$userId|$vehicleNumber|$timestamp|$qrType|$securityHash"
    }
    
    companion object {
        fun fromQRString(qrString: String): QRCodeData? {
            val parts = qrString.split("|")
            // Support both old format (5 parts) and new format (6 parts)
            if (parts.size < 5 || parts[0] != "PARKTRACK") return null
            
            return try {
                if (parts.size == 6) {
                    // New format with qrType
                    QRCodeData(
                        userId = parts[1],
                        vehicleNumber = parts[2],
                        timestamp = parts[3].toLong(),
                        securityHash = parts[5],
                        qrType = parts[4]
                    )
                } else {
                    // Old format without qrType (for backward compatibility)
                    QRCodeData(
                        userId = parts[1],
                        vehicleNumber = parts[2],
                        timestamp = parts[3].toLong(),
                        securityHash = parts[4],
                        qrType = "ENTRY" // Default to ENTRY
                    )
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
