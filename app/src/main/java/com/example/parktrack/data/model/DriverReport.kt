package com.example.parktrack.data.model

import com.google.firebase.Timestamp

/**
 * Driver-specific report containing only the driver's own data.
 * These reports are private and visible only to the driver who generated them.
 */
data class DriverReport(
    val id: String = "",
    val driverId: String = "",
    val driverName: String = "",
    val driverEmail: String = "",
    val driverPhoneNumber: String = "",
    val vehicleNumber: String = "",
    val reportType: ReportType = ReportType.MONTHLY,
    val periodStart: Timestamp? = null,
    val periodEnd: Timestamp? = null,
    val year: Int = 0,
    val month: Int = 0,
    
    // Driver-specific statistics
    val totalSessions: Int = 0,
    val totalDurationMinutes: Long = 0,
    val averageSessionDuration: Long = 0, // in minutes
    val totalCharges: Double = 0.0,
    val totalPaid: Double = 0.0,
    val totalOutstanding: Double = 0.0,
    val overdueCharges: Double = 0.0,
    
    // Session breakdown
    val completedSessions: Int = 0,
    val activeSessions: Int = 0,
    val paidSessions: Int = 0,
    val unpaidSessions: Int = 0,
    val overdueSessions: Int = 0,
    
    // Parking details
    val favoriteParkingLocation: String = "",
    val totalVisitsByLocation: Map<String, Int> = emptyMap(),
    val peakParkingDays: List<String> = emptyList(), // Days with most parking activity
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val generatedBy: String = "", // Driver ID who generated it
    val isDriverReport: Boolean = true // Flag to distinguish from admin reports
)

/**
 * Sealed class to represent different report types for type safety
 */
sealed class Report {
    abstract val id: String
    abstract val reportType: ReportType
    abstract val createdAt: Timestamp
    abstract val generatedBy: String
}
