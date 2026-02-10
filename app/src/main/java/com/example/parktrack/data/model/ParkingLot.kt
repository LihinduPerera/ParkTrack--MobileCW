package com.example.parktrack.data.model

data class ParkingLot(
    val id: String = "",
    val name: String = "",
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val totalSpaces: Int = 0,
    val availableSpaces: Int = 0,
    val occupiedSpaces: Int = 0,
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zipCode: String = "",
    val openingTime: String = "00:00", // HH:mm format
    val closingTime: String = "23:59", // HH:mm format
    val twentyFourHours: Boolean = true,
    val hasEVCharging: Boolean = false,
    val hasDisabledParking: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
