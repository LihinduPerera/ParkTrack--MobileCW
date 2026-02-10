package com.example.parktrack.data.model

data class ParkingRate(
    val id: String = "",
    val parkingLotId: String = "",
    val rateType: RateType = RateType.NORMAL, // NORMAL, VIP, HOURLY, OVERNIGHT
    val basePricePerHour: Double = 0.0,
    val maxDailyPrice: Double = 0.0,
    val minChargeHours: Double = 1.0, // Minimum hours to charge
    val minChargeAmount: Double = 0.0,
    val overnightRate: Double = 0.0, // For overnight parking
    val overnightStartHour: Int = 22, // 10 PM
    val overnightEndHour: Int = 6, // 6 AM
    val maxOvernightPrice: Double = 0.0,
    val vipMultiplier: Double = 1.5, // 50% extra for VIP
    val hourlyRate: Double = 0.0, // Direct hourly rate
    val discountPercentage: Double = 0.0, // For frequent parkers
    // Tier-specific rates
    val normalRate: Double = 0.0, // Rate for NORMAL tier users
    val goldRate: Double = 0.0, // Rate for GOLD tier users
    val platinumRate: Double = 0.0, // Rate for PLATINUM tier users
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class RateType {
    NORMAL,
    VIP,
    HOURLY,
    OVERNIGHT
}
