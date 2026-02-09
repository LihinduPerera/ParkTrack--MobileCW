package com.example.parktrack.data.model

import com.google.firebase.Timestamp

data class ParkingReport(
    val id: String = "",
    val reportType: ReportType = ReportType.MONTHLY, // MONTHLY, QUARTERLY, ANNUAL
    val periodStart: Timestamp? = null,
    val periodEnd: Timestamp? = null,
    val year: Int = 0,
    val month: Int = 0,
    val totalSessions: Int = 0,
    val totalRevenue: Double = 0.0,
    val totalOverdueCharges: Double = 0.0,
    val amountCollected: Double = 0.0,
    val outstandingAmount: Double = 0.0,
    val averageSessionDuration: Long = 0, // in minutes
    val peakHours: List<String> = emptyList(),
    val averageOccupancy: Double = 0.0, // percentage
    val numberOfUniqueVehicles: Int = 0,
    val numberOfRegisteredDrivers: Int = 0,
    val paidSessions: Int = 0,
    val unpaidSessions: Int = 0,
    val overdueSessions: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val generatedBy: String = "" // Admin ID
)

enum class ReportType {
    MONTHLY,
    QUARTERLY,
    ANNUAL,
    CUSTOM
}
