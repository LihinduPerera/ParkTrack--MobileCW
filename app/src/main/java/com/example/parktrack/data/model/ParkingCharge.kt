package com.example.parktrack.data.model

import com.google.firebase.Timestamp

data class ParkingCharge(
    val id: String = "",
    val sessionId: String = "",
    val driverId: String = "",
    val vehicleNumber: String = "",
    val parkingLotId: String = "",
    val parkingLotName: String = "",
    val entryTime: Timestamp? = null,
    val exitTime: Timestamp? = null,
    val durationMinutes: Long = 0,
    val rateType: String = "NORMAL",
    val baseRate: Double = 0.0,
    val chargeableAmount: Double = 0.0, // Rounded to nearest hour/unit
    val calculatedCharge: Double = 0.0,
    val discountApplied: Double = 0.0,
    val finalCharge: Double = 0.0,
    val isPaid: Boolean = false,
    val paymentMethod: String = "", // CASH, CARD, ONLINE, etc.
    val paymentDate: Timestamp? = null,
    val isOverdue: Boolean = false,
    val overdueCharge: Double = 0.0,
    val remarks: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    fun calculateCharge(
        baseRate: Double,
        durationMinutes: Long,
        rateType: String,
        subscriptionTier: SubscriptionTier? = null,
        parkingRate: ParkingRate? = null
    ): Double {
        // If subscription tier and parking rate are provided, use new billing logic
        if (subscriptionTier != null && parkingRate != null) {
            return calculateParkingCharge(durationMinutes, subscriptionTier, parkingRate)
        }

        // Fallback to old logic for backward compatibility
        val hours = (durationMinutes + 59) / 60 // Round up to nearest hour
        return when (rateType) {
            "VIP" -> baseRate * hours * 1.5
            "OVERNIGHT" -> baseRate * 0.5 * hours
            else -> baseRate * hours
        }
    }

    companion object {
        /**
         * Calculate parking charge using the new billing system
         */
        fun calculateParkingCharge(durationMinutes: Long, subscriptionTier: SubscriptionTier, parkingRate: ParkingRate): Double {
            return com.example.parktrack.billing.calculateParkingCharge(durationMinutes, subscriptionTier, parkingRate)
        }
    }
}
