package com.example.parktrack.data.model

import com.google.firebase.Timestamp

/**
 * Admin/System-wide parking report containing aggregate data.
 * These reports are visible only to ADMIN users.
 */
data class ParkingReport(
    val id: String = "",
    val reportType: ReportType = ReportType.MONTHLY, // MONTHLY, QUARTERLY, ANNUAL
    val periodStart: Timestamp? = null,
    val periodEnd: Timestamp? = null,
    val year: Int = 0,
    val month: Int = 0,
    
    // System-wide statistics
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
    
    // Access control fields
    val isAdminReport: Boolean = true, // Flag to distinguish admin reports from driver reports
    val visibleToRole: String = "ADMIN", // ADMIN, DRIVER, or BOTH
    
    // Metadata
    val createdAt: Timestamp = Timestamp.now(),
    val generatedBy: String = "", // Admin ID who generated it
    val generatedByName: String = "" // Admin name for display
)

enum class ReportType {
    MONTHLY,
    QUARTERLY,
    ANNUAL,
    CUSTOM,
    DRIVER_PERSONAL // Legacy type for backward compatibility with existing data
}
