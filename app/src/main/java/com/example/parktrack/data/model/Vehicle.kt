package com.example.parktrack.data.model

import com.google.firebase.Timestamp

data class Vehicle(
    val id: String = "",
    val ownerId: String = "",
    val vehicleNumber: String = "",
    val vehicleModel: String = "",
    val vehicleColor: String = "",
    val vehicleType: String = "Car", // Car, Motorcycle, Truck, etc.
    val registrationNumber: String = "",
    val registrationExpiryDate: Timestamp? = null,
    val insuranceExpiryDate: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val isActive: Boolean = true
)
