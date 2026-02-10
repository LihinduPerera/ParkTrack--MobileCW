package com.example.parktrack.billing

import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.SubscriptionTier
import kotlin.math.ceil
import kotlin.math.floor

/**
 * FIXED TIER PRICING - As per requirements:
 * - Normal Tier: Rs. 100/hour (fee calculated only on exit)
 * - Gold Tier: Rs. 80/hour (1st hour free, then charged for completed hours only)
 * - Platinum Tier: Rs. 60/hour (1st hour free, then charged for completed hours only)
 * 
 * ALL TIERS: Fee is calculated ONLY on exit, not on entry
 */
object TierPricing {
    const val NORMAL_HOURLY_RATE = 100.0
    const val GOLD_HOURLY_RATE = 80.0
    const val PLATINUM_HOURLY_RATE = 60.0
    
    const val FREE_HOURS_GOLD_PLATINUM = 1 // First hour free for Gold/Platinum
}

/**
 * Legacy enum for backward compatibility
 */
enum class MembershipTier(val hourlyRate: Double, val dailyCap: Double, val monthlyUnlimited: Double?) {
    NORMAL(TierPricing.NORMAL_HOURLY_RATE, 500.0, null),
    GOLD(TierPricing.GOLD_HOURLY_RATE, 400.0, null),
    PLATINUM(TierPricing.PLATINUM_HOURLY_RATE, 300.0, 2000.0)
}

data class ParkingSession(val id: String, val hours: Double, val date: String)

/**
 * Calculate parking charge based on subscription tier and parking rates
 * FIXED: Fee is calculated ONLY on exit for ALL tiers
 * 
 * NORMAL TIER:
 * - Hours rounded UP to nearest hour
 * - Example: 1 min = 1 hour, 61 min = 2 hours
 * 
 * GOLD/PLATINUM TIERS:
 * - First hour is FREE (no charge)
 * - After first hour, charge ONLY for COMPLETED full hours (rounded DOWN)
 * - Example: 2h 30m = 1 hour free + 1 completed hour charged
 * 
 * @param durationMinutes Total parking duration in minutes
 * @param subscriptionTier User's subscription tier
 * @param parkingRate Parking rate configuration (optional, uses fixed rates if not provided)
 * @return Calculated charge amount
 */
fun calculateParkingCharge(
    durationMinutes: Long, 
    subscriptionTier: SubscriptionTier, 
    parkingRate: ParkingRate? = null
): Double {
    // Get the applicable rate based on subscription tier (use fixed rates)
    val applicableRate = when (subscriptionTier) {
        SubscriptionTier.NORMAL -> if (parkingRate?.normalRate != null && parkingRate.normalRate > 0) {
            parkingRate.normalRate
        } else {
            TierPricing.NORMAL_HOURLY_RATE
        }
        SubscriptionTier.GOLD -> if (parkingRate?.goldRate != null && parkingRate.goldRate > 0) {
            parkingRate.goldRate
        } else {
            TierPricing.GOLD_HOURLY_RATE
        }
        SubscriptionTier.PLATINUM -> if (parkingRate?.platinumRate != null && parkingRate.platinumRate > 0) {
            parkingRate.platinumRate
        } else {
            TierPricing.PLATINUM_HOURLY_RATE
        }
    }

    return when (subscriptionTier) {
        SubscriptionTier.NORMAL -> {
            // NORMAL TIER: Minimum charge Rs 100 (1 hour) even for 0 minutes
            // Round UP to nearest hour for any duration
            val hours = durationMinutes / 60.0
            val chargeableHours = if (hours <= 0) 1 else ceil(hours).toInt()
            applicableRate * chargeableHours
        }
        SubscriptionTier.GOLD, SubscriptionTier.PLATINUM -> {
            // GOLD/PLATINUM: First hour FREE, then charge only COMPLETED hours
            val hours = durationMinutes / 60.0
            
            if (hours <= TierPricing.FREE_HOURS_GOLD_PLATINUM) {
                // Still within free hour
                0.0
            } else {
                // Subtract the free hour, then count only COMPLETED hours (floor)
                val chargeableHours = floor(hours - TierPricing.FREE_HOURS_GOLD_PLATINUM).toInt()
                if (chargeableHours > 0) {
                    applicableRate * chargeableHours
                } else {
                    0.0
                }
            }
        }
    }.coerceAtLeast(0.0) // Ensure non-negative
}

/**
 * Calculate charge for Gold/Platinum tier drivers
 * First hour free, then only completed full hours are charged
 */
fun calculateTierWithFreeHour(
    durationMinutes: Long,
    hourlyRate: Double
): Double {
    val hours = durationMinutes / 60.0
    
    return if (hours <= TierPricing.FREE_HOURS_GOLD_PLATINUM) {
        0.0
    } else {
        val chargeableHours = floor(hours - TierPricing.FREE_HOURS_GOLD_PLATINUM).toInt()
        if (chargeableHours > 0) hourlyRate * chargeableHours else 0.0
    }
}

/**
 * Get the hourly rate for a specific tier
 */
fun getTierHourlyRate(tier: SubscriptionTier): Double {
    return when (tier) {
        SubscriptionTier.NORMAL -> TierPricing.NORMAL_HOURLY_RATE
        SubscriptionTier.GOLD -> TierPricing.GOLD_HOURLY_RATE
        SubscriptionTier.PLATINUM -> TierPricing.PLATINUM_HOURLY_RATE
    }
}

/**
 * Get tier display name with pricing info
 */
fun getTierDisplayName(tier: SubscriptionTier): String {
    return when (tier) {
        SubscriptionTier.NORMAL -> "Normal (Rs. 100/hr)"
        SubscriptionTier.GOLD -> "Gold (Rs. 80/hr, 1st hr free)"
        SubscriptionTier.PLATINUM -> "Platinum (Rs. 60/hr, 1st hr free)"
    }
}

/**
 * Legacy function for backward compatibility
 */
fun calculateSessionCost(session: ParkingSession, tier: MembershipTier): Double {
    val rawCost = session.hours * tier.hourlyRate
    return minOf(rawCost, tier.dailyCap)
}
