package com.example.parktrack.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.DRIVER,
    val subscriptionTier: SubscriptionTier = SubscriptionTier.NORMAL,
    val phoneNumber: String = "",
    val vehicleNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val profileImageUrl: String = "",
    val isVerified: Boolean = false,
    // Admin-specific fields
    val badgeId: String = "",
    val assignedGate: String = "",
    val totalScans: Int = 0,
    val scansToday: Int = 0,
    val department: String = ""
)

enum class UserRole {
    DRIVER,
    ADMIN
}

enum class SubscriptionTier {
    NORMAL,
    GOLD,
    PLATINUM
}
