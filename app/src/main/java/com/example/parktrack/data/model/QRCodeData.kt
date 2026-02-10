package com.example.parktrack.data.model

data class QRCodeData(
    val userId: String,
    val vehicleNumber: String,
    val vehicleId: String = "",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val timestamp: Long,
    val securityHash: String,
    val qrType: String = "ENTRY" // ENTRY or EXIT
) {
    // Format: "PARKTRACK|userId|vehicleNumber|vehicleId|vehicleModel|vehicleColor|timestamp|qrType|hash"
    fun toQRString(): String {
        return "PARKTRACK|$userId|$vehicleNumber|$vehicleId|$vehicleModel|$vehicleColor|$timestamp|$qrType|$securityHash"
    }
    
    companion object {
        fun fromQRString(qrString: String): QRCodeData? {
            val parts = qrString.split("|")
            if (parts[0] != "PARKTRACK") return null
            
            return try {
                when (parts.size) {
                    9 -> {
                        // New format with vehicle details
                        QRCodeData(
                            userId = parts[1],
                            vehicleNumber = parts[2],
                            vehicleId = parts[3],
                            vehicleModel = parts[4],
                            vehicleColor = parts[5],
                            timestamp = parts[6].toLong(),
                            securityHash = parts[8],
                            qrType = parts[7]
                        )
                    }
                    6 -> {
                        // Medium format with qrType but no vehicle details
                        QRCodeData(
                            userId = parts[1],
                            vehicleNumber = parts[2],
                            timestamp = parts[3].toLong(),
                            securityHash = parts[5],
                            qrType = parts[4]
                        )
                    }
                    5 -> {
                        // Old format without qrType (for backward compatibility)
                        QRCodeData(
                            userId = parts[1],
                            vehicleNumber = parts[2],
                            timestamp = parts[3].toLong(),
                            securityHash = parts[4],
                            qrType = "ENTRY" // Default to ENTRY
                        )
                    }
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}
