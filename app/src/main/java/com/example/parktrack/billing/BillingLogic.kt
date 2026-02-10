package com.example.parktrack.billing

import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.SubscriptionTier
import kotlin.math.ceil

enum class MembershipTier(val hourlyRate: Double, val dailyCap: Double, val monthlyUnlimited: Double?) {
    NORMAL(10.0, 50.0, null), // Normal users pay full rate
    GOLD(5.0, 40.0, null),    // Gold users get discount
    PLATINUM(4.0, 30.0, 200.0) // Platinum users get more discount and monthly cap
}

data class ParkingSession(val id: String, val hours: Double, val date: String)

/**
 * Calculate parking charge based on subscription tier and parking rates
 * @param durationMinutes Total parking duration in minutes
 * @param subscriptionTier User's subscription tier
 * @param parkingRate Parking rate configuration
 * @return Calculated charge amount
 */
fun calculateParkingCharge(durationMinutes: Long, subscriptionTier: SubscriptionTier, parkingRate: ParkingRate): Double {
    val hours = durationMinutes / 60.0

    // Determine the applicable rate based on subscription tier
    val applicableRate = when (subscriptionTier) {
        SubscriptionTier.NORMAL -> parkingRate.normalRate.takeIf { it > 0 } ?: parkingRate.basePricePerHour
        SubscriptionTier.GOLD -> parkingRate.goldRate.takeIf { it > 0 } ?: parkingRate.basePricePerHour * 0.8 // 20% discount if not specified
        SubscriptionTier.PLATINUM -> parkingRate.platinumRate.takeIf { it > 0 } ?: parkingRate.basePricePerHour * 0.6 // 40% discount if not specified
    }

    // Apply billing rules based on tier
    val baseCharge = when (subscriptionTier) {
        SubscriptionTier.NORMAL -> {
            // Normal users: Pay full hour even for 1 minute
            val chargeableHours = ceil(hours)
            applicableRate * chargeableHours
        }
        SubscriptionTier.GOLD, SubscriptionTier.PLATINUM -> {
            // Gold/Platinum users: No charge until exceeding 1 hour
            if (hours <= 1.0) {
                0.0
            } else {
                val chargeableHours = hours - 1.0 // Free first hour
                applicableRate * chargeableHours
            }
        }
    }.coerceAtLeast(0.0) // Ensure non-negative

    // Apply daily cap if applicable
    return minOf(baseCharge, parkingRate.maxDailyPrice)
}

/**
 * Legacy function for backward compatibility
 */
fun calculateSessionCost(session: ParkingSession, tier: MembershipTier): Double {
    val rawCost = session.hours * tier.hourlyRate
    return minOf(rawCost, tier.dailyCap)
}
