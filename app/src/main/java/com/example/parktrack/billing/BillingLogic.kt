package com.example.parktrack.billing

enum class MembershipTier(val hourlyRate: Double, val dailyCap: Double, val monthlyUnlimited: Double?) {
    GOLD(5.0, 40.0, null),
    PLATINUM(4.0, 30.0, 200.0)
}

data class ParkingSession(val id: String, val hours: Double, val date: String)

fun calculateSessionCost(session: ParkingSession, tier: MembershipTier): Double {
    val rawCost = session.hours * tier.hourlyRate
    return minOf(rawCost, tier.dailyCap)
}