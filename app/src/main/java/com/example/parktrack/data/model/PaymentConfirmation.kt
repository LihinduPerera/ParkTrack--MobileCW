package com.example.parktrack.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

/**
 * Represents a payment confirmation made by an admin.
 * This is used for manual payment confirmation without payment gateway integration.
 */
data class PaymentConfirmation(
    val id: String = "",
    val driverId: String = "",
    val driverEmail: String = "",
    val driverName: String = "",
    val invoiceId: String = "", // Optional - if paying for specific invoice
    val chargeId: String = "", // Optional - if paying for specific charge
    val amount: Double = 0.0,
    val paymentMethod: String = "CASH", // CASH, BANK_TRANSFER, etc.
    val paymentType: String = "PARKING_CHARGE", // PARKING_CHARGE, TIER_UPGRADE, etc.
    val confirmedByAdminId: String = "",
    val confirmedByAdminName: String = "",
    val confirmationDate: Timestamp = Timestamp.now(),
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now()
)

/**
 * Represents a tier upgrade request/record.
 * Tracks when a driver upgrades their subscription tier.
 * 
 * NOTE: Firestore requires @PropertyName annotations for boolean fields
 * with "is" prefix to properly serialize/deserialize
 */
data class TierUpgradeRecord(
    val id: String = "",
    val driverId: String = "",
    val driverEmail: String = "",
    val driverName: String = "",
    val fromTier: String = "NORMAL",
    val toTier: String = "",
    val upgradeFee: Double = 0.0,
    @get:PropertyName("isPaid")
    val isPaid: Boolean = false,
    val paymentConfirmationId: String = "",
    val processedByAdminId: String = "",
    val processedByAdminName: String = "",
    val processedAt: Timestamp? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val expiresAt: Timestamp? = null // Optional expiration date for tier
)

/**
 * Extension function to calculate upgrade fee between tiers
 */
fun calculateTierUpgradeFee(currentTier: SubscriptionTier, newTier: SubscriptionTier): Double {
    return when {
        currentTier == SubscriptionTier.NORMAL && newTier == SubscriptionTier.GOLD -> 500.0
        currentTier == SubscriptionTier.NORMAL && newTier == SubscriptionTier.PLATINUM -> 1000.0
        currentTier == SubscriptionTier.GOLD && newTier == SubscriptionTier.PLATINUM -> 700.0
        else -> 0.0 // No fee for downgrades or same tier
    }
}

/**
 * Get display name for payment status
 */
fun getPaymentStatusDisplay(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "Pending"
        "PAID" -> "Paid"
        "OVERDUE" -> "Overdue"
        "PARTIAL" -> "Partial Payment"
        "CANCELLED" -> "Cancelled"
        else -> status
    }
}

/**
 * Get color for payment status
 */
fun getPaymentStatusColor(status: String): Long {
    return when (status.uppercase()) {
        "PENDING" -> 0xFFFF9800 // Orange
        "PAID" -> 0xFF4CAF50 // Green
        "OVERDUE" -> 0xFFF44336 // Red
        "PARTIAL" -> 0xFF2196F3 // Blue
        "CANCELLED" -> 0xFF9E9E9E // Gray
        else -> 0xFF9E9E9E // Gray
    }
}
