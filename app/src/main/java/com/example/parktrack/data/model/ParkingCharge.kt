package com.example.parktrack.data.model

import com.google.firebase.Timestamp
import com.example.parktrack.billing.calculateParkingCharge
import com.example.parktrack.billing.TierPricing

/**
 * FIXED: ParkingCharge model with correct billing logic
 * Tracks payment status and unpaid records per driver
 */
data class ParkingCharge(
    val id: String = "",
    val sessionId: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverEmail: String = "",
    val vehicleNumber: String = "",
    val vehicleModel: String = "",
    val parkingLotId: String = "",
    val parkingLotName: String = "",
    val entryTime: Timestamp? = null,
    val exitTime: Timestamp? = null,
    val durationMinutes: Long = 0,
    val rateType: String = "NORMAL",
    val subscriptionTier: String = "NORMAL",
    val baseRate: Double = TierPricing.NORMAL_HOURLY_RATE,
    val chargeableAmount: Double = 0.0,
    val calculatedCharge: Double = 0.0,
    val discountApplied: Double = 0.0,
    val finalCharge: Double = 0.0,
    // Payment tracking
    val isPaid: Boolean = false,
    val paymentMethod: String = "", // CASH, CARD, ONLINE
    val paymentDate: Timestamp? = null,
    val paymentConfirmedBy: String = "", // Admin who confirmed payment
    val paymentConfirmedByName: String = "",
    // Unpaid tracking
    val isOverdue: Boolean = false,
    val overdueDays: Int = 0,
    val overdueCharge: Double = 0.0,
    val remarks: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
) {
    /**
     * Calculate if this charge is unpaid
     */
    fun isUnpaid(): Boolean = !isPaid && finalCharge > 0

    /**
     * Get payment status display
     */
    fun getPaymentStatus(): String {
        return when {
            isPaid -> "PAID"
            isOverdue -> "OVERDUE"
            finalCharge > 0 -> "UNPAID"
            else -> "FREE"
        }
    }

    /**
     * Get formatted amount string
     */
    fun getFormattedAmount(): String {
        return "Rs. %.2f".format(finalCharge)
    }

    companion object {
        /**
         * FIXED: Calculate parking charge using the correct billing system
         * Fee is calculated ONLY on exit for ALL tiers
         * 
         * @param durationMinutes Total parking duration in minutes
         * @param subscriptionTier User's subscription tier
         * @param parkingRate Parking rate configuration
         * @return Calculated charge amount
         */
        fun calculateParkingCharge(
            durationMinutes: Long, 
            subscriptionTier: SubscriptionTier, 
            parkingRate: ParkingRate? = null
        ): Double {
            return com.example.parktrack.billing.calculateParkingCharge(
                durationMinutes, 
                subscriptionTier, 
                parkingRate
            )
        }

        /**
         * Get hourly rate for tier
         */
        fun getHourlyRate(tier: SubscriptionTier): Double {
            return when (tier) {
                SubscriptionTier.NORMAL -> TierPricing.NORMAL_HOURLY_RATE
                SubscriptionTier.GOLD -> TierPricing.GOLD_HOURLY_RATE
                SubscriptionTier.PLATINUM -> TierPricing.PLATINUM_HOURLY_RATE
            }
        }
    }
}

/**
 * Extension function to check if charge is in unpaid list
 */
fun ParkingCharge.isInUnpaidList(): Boolean {
    return !isPaid && finalCharge > 0 && !isOverdue
}

/**
 * Extension function to get charge summary
 */
fun ParkingCharge.getSummary(): String {
    val tier = SubscriptionTier.valueOf(subscriptionTier)
    val durationStr = when {
        durationMinutes >= 60 -> "${durationMinutes / 60}h ${durationMinutes % 60}m"
        else -> "${durationMinutes}m"
    }
    return "${vehicleNumber} - $durationStr - ${getPaymentStatus()} - ${getFormattedAmount()}"
}

/**
 * Data class for unpaid record summary per driver
 */
data class DriverUnpaidSummary(
    val driverId: String,
    val driverName: String,
    val driverEmail: String,
    val totalUnpaidCharges: Int,
    val totalUnpaidAmount: Double,
    val unpaidCharges: List<ParkingCharge>,
    val oldestUnpaidDate: Timestamp? = null
)
