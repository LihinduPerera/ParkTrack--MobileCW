package com.example.parktrack.data.model

import com.google.firebase.Timestamp

data class EnrichedParkingSession(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverPhoneNumber: String = "",
    val driverProfileImageUrl: String = "",
    val vehicleNumber: String = "",
    val vehicleModel: String = "",
    val entryTime: Timestamp? = null,
    val exitTime: Timestamp? = null,
    val gateLocation: String = "",
    val scannedByAdminId: String = "",
    val adminName: String = "",
    val status: String = "ACTIVE",
    val qrCodeUsed: String = "",
    val durationMinutes: Long = 0,
    val createdAt: Timestamp = Timestamp.now()
)