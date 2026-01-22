package com.example.parktrack.data.model

import com.google.firebase.Timestamp

data class ParkingSession(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val vehicleNumber: String = "",
    val entryTime: Timestamp? = null,
    val exitTime: Timestamp? = null,
    val gateLocation: String = "",
    val scannedByAdminId: String = "",
    val adminName: String = "",
    val status: String = "ACTIVE", // ACTIVE or COMPLETED
    val qrCodeUsed: String = "",
    val durationMinutes: Long = 0,
    val createdAt: Timestamp = Timestamp.now()
)
