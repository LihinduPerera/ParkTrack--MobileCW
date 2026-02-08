package com.example.parktrack.data.model

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val fullName: String = "",
    val role: UserRole = UserRole.DRIVER,
    val phoneNumber: String = "",
    val vehicleNumber: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val profileImageUrl: String = "",
    val isVerified: Boolean = false
)

enum class UserRole {
    DRIVER,
    ADMIN
}