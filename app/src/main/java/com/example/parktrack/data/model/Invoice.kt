package com.example.parktrack.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Invoice model for monthly billing
 * 
 * NOTE: Firestore requires @PropertyName annotations for boolean fields
 * with "is" prefix to properly serialize/deserialize
 */
data class Invoice(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val month: String = "", // YYYY-MM format
    val year: Int = 0,
    val monthNumber: Int = 0,
    val totalSessions: Int = 0,
    val totalDurationMinutes: Long = 0,
    val totalCharges: Double = 0.0,
    val totalDiscount: Double = 0.0,
    val totalOverdueCharges: Double = 0.0,
    val netAmount: Double = 0.0,
    val amountPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    @get:PropertyName("isPaid")
    val isPaid: Boolean = false,
    val paymentStatus: String = "PENDING", // PENDING, PAID, OVERDUE
    val dueDate: Timestamp? = null,
    val paidDate: Timestamp? = null,
    val charges: List<String> = emptyList(), // List of ParkingCharge IDs
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
