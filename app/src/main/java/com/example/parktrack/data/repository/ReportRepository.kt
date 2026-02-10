package com.example.parktrack.data.repository

import com.example.parktrack.data.model.DriverReport
import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.data.model.ReportType
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.UserRole
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val parkingSessionRepository: ParkingSessionRepository,
    private val billingRepository: BillingRepository,
    private val userRepository: UserRepository
) {
    private val reportsCollection = "parkingReports"
    private val driverReportsCollection = "driverReports"

    /**
     * Generate monthly admin report - only accessible to ADMIN users
     */
    suspend fun generateMonthlyAdminReport(year: Int, month: Int, admin: User): Result<ParkingReport> = runCatching {
        require(admin.role == UserRole.ADMIN) { "Only admins can generate admin reports" }
        
        android.util.Log.d("ReportRepository", "Generating admin report for $year-$month by admin: ${admin.id}")
        
        val (startDate, endDate) = getMonthBoundaries(year, month)
        android.util.Log.d("ReportRepository", "Date range: ${startDate.toDate()} to ${endDate.toDate()}")
        
        // Get all sessions for the month
        val allSessions = firestore.collection("parkingSessions")
            .whereGreaterThanOrEqualTo("entryTime", startDate)
            .whereLessThanOrEqualTo("entryTime", endDate)
            .get()
            .await()
            .toObjects(com.example.parktrack.data.model.ParkingSession::class.java)
        
        android.util.Log.d("ReportRepository", "Found ${allSessions.size} sessions")

        val completedSessions = allSessions.filter { it.status == "COMPLETED" }
        
        // Get charges for the month - don't filter by date if collection might be empty
        val chargesSnapshot = firestore.collection("parkingCharges")
            .whereGreaterThanOrEqualTo("createdAt", startDate)
            .whereLessThanOrEqualTo("createdAt", endDate)
            .get()
            .await()
        
        val charges = chargesSnapshot.toObjects(com.example.parktrack.data.model.ParkingCharge::class.java)
        android.util.Log.d("ReportRepository", "Found ${charges.size} charges")

        val totalRevenue = charges.sumOf { it.finalCharge }
        val amountCollected = charges.filter { it.isPaid }.sumOf { it.finalCharge }
        val outstandingAmount = totalRevenue - amountCollected
        val averageDuration = if (completedSessions.isNotEmpty()) {
            completedSessions.sumOf { it.durationMinutes } / completedSessions.size
        } else 0L

        // Count unique drivers
        val uniqueDrivers = allSessions.map { it.driverId }.distinct().size

        val report = ParkingReport(
            id = "admin-${year}-${month}-${System.currentTimeMillis()}",
            reportType = ReportType.MONTHLY,
            periodStart = startDate,
            periodEnd = endDate,
            year = year,
            month = month,
            totalSessions = completedSessions.size,
            totalRevenue = totalRevenue,
            totalOverdueCharges = charges.filter { it.isOverdue }.sumOf { it.overdueCharge },
            amountCollected = amountCollected,
            outstandingAmount = outstandingAmount,
            averageSessionDuration = averageDuration,
            numberOfUniqueVehicles = allSessions.map { it.vehicleNumber }.distinct().size,
            numberOfRegisteredDrivers = uniqueDrivers,
            paidSessions = charges.filter { it.isPaid }.size,
            unpaidSessions = charges.filter { !it.isPaid }.size,
            overdueSessions = charges.filter { it.isOverdue }.size,
            isAdminReport = true,
            visibleToRole = "ADMIN",
            createdAt = Timestamp.now(),
            generatedBy = admin.id,
            generatedByName = admin.fullName
        )

        android.util.Log.d("ReportRepository", "Saving report: ${report.id}")
        // Save report to Firestore
        firestore.collection(reportsCollection).document(report.id).set(report).await()
        android.util.Log.d("ReportRepository", "Report saved successfully")
        report
    }.onFailure { e ->
        android.util.Log.e("ReportRepository", "Error generating admin report", e)
    }

    /**
     * Generate driver-specific report - contains only that driver's data
     * This report is private to the driver
     */
    suspend fun generateDriverReport(
        year: Int, 
        month: Int, 
        driver: User
    ): Result<DriverReport> = runCatching {
        require(driver.role == UserRole.DRIVER) { "Only drivers can generate driver reports" }
        
        val (startDate, endDate) = getMonthBoundaries(year, month)
        
        // Get driver's sessions for the month
        val driverSessions = firestore.collection("parkingSessions")
            .whereEqualTo("driverId", driver.id)
            .whereLessThanOrEqualTo("entryTime", endDate)
            .whereGreaterThanOrEqualTo("entryTime", startDate)
            .get()
            .await()
            .toObjects(com.example.parktrack.data.model.ParkingSession::class.java)

        val completedSessions = driverSessions.filter { it.status == "COMPLETED" }
        val activeSessions = driverSessions.filter { it.status == "ACTIVE" }
        
        // Get driver's charges for the month
        val driverCharges = firestore.collection("parkingCharges")
            .whereEqualTo("driverId", driver.id)
            .whereGreaterThanOrEqualTo("createdAt", startDate)
            .whereLessThanOrEqualTo("createdAt", endDate)
            .get()
            .await()
            .toObjects(com.example.parktrack.data.model.ParkingCharge::class.java)

        val totalCharges = driverCharges.sumOf { it.finalCharge }
        val totalPaid = driverCharges.filter { it.isPaid }.sumOf { it.finalCharge }
        val totalOutstanding = totalCharges - totalPaid
        val overdueCharges = driverCharges.filter { it.isOverdue }.sumOf { it.overdueCharge }
        
        val totalDuration = completedSessions.sumOf { it.durationMinutes }
        val averageDuration = if (completedSessions.isNotEmpty()) {
            totalDuration / completedSessions.size
        } else 0L

        // Calculate visits by location
        val visitsByLocation = completedSessions
            .groupBy { it.gateLocation }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
        
        val favoriteLocation = visitsByLocation.keys.firstOrNull() ?: "N/A"

        // Calculate peak parking days
        val peakDays = completedSessions
            .groupBy { 
                val cal = Calendar.getInstance()
                cal.time = it.entryTime?.toDate() ?: java.util.Date()
                cal.get(Calendar.DAY_OF_WEEK)
            }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { 
                when (it.first) {
                    Calendar.SUNDAY -> "Sunday"
                    Calendar.MONDAY -> "Monday"
                    Calendar.TUESDAY -> "Tuesday"
                    Calendar.WEDNESDAY -> "Wednesday"
                    Calendar.THURSDAY -> "Thursday"
                    Calendar.FRIDAY -> "Friday"
                    Calendar.SATURDAY -> "Saturday"
                    else -> "Unknown"
                }
            }

        val report = DriverReport(
            id = "driver-${driver.id}-${year}-${month}-${System.currentTimeMillis()}",
            driverId = driver.id,
            driverName = driver.fullName,
            driverEmail = driver.email,
            driverPhoneNumber = driver.phoneNumber,
            vehicleNumber = driver.vehicleNumber,
            reportType = ReportType.MONTHLY,
            periodStart = startDate,
            periodEnd = endDate,
            year = year,
            month = month,
            totalSessions = driverSessions.size,
            totalDurationMinutes = totalDuration,
            averageSessionDuration = averageDuration,
            totalCharges = totalCharges,
            totalPaid = totalPaid,
            totalOutstanding = totalOutstanding,
            overdueCharges = overdueCharges,
            completedSessions = completedSessions.size,
            activeSessions = activeSessions.size,
            paidSessions = driverCharges.filter { it.isPaid }.size,
            unpaidSessions = driverCharges.filter { !it.isPaid }.size,
            overdueSessions = driverCharges.filter { it.isOverdue }.size,
            favoriteParkingLocation = favoriteLocation,
            totalVisitsByLocation = visitsByLocation,
            peakParkingDays = peakDays,
            createdAt = Timestamp.now(),
            generatedBy = driver.id,
            isDriverReport = true
        )

        // Save report to Firestore in driverReports collection
        firestore.collection(driverReportsCollection).document(report.id).set(report).await()
        report
    }

    /**
     * Get admin reports - only accessible to ADMIN users
     */
    suspend fun getAdminReports(limit: Int = 50): Result<List<ParkingReport>> = runCatching {
        android.util.Log.d("ReportRepository", "Fetching admin reports from collection: $reportsCollection")
        
        // Try simple query first - just order by createdAt (most likely to have index)
        val snapshot = firestore.collection(reportsCollection)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong() * 2) // Get more and filter in memory
            .get()
            .await()
        
        val allReports = snapshot.toObjects(ParkingReport::class.java)
        android.util.Log.d("ReportRepository", "Fetched ${allReports.size} total reports")
        
        // Filter admin reports in memory
        val adminReports = allReports.filter { it.isAdminReport }.take(limit)
        android.util.Log.d("ReportRepository", "Filtered to ${adminReports.size} admin reports")
        
        adminReports
    }.onFailure { e ->
        android.util.Log.e("ReportRepository", "Error fetching admin reports", e)
    }

    /**
     * Get driver reports - returns only the specified driver's reports
     * Used for both drivers viewing their own reports and admins viewing specific driver reports
     */
    suspend fun getDriverReports(driverId: String, limit: Int = 50): Result<List<DriverReport>> = runCatching {
        android.util.Log.d("ReportRepository", "Fetching driver reports for driverId: $driverId")
        
        // Try with a simpler query first - may avoid composite index issues
        val snapshot = firestore.collection(driverReportsCollection)
            .whereEqualTo("driverId", driverId)
            .get()
            .await()
        
        val reports = snapshot.toObjects(DriverReport::class.java)
            .sortedByDescending { it.createdAt.toDate().time }
            .take(limit)
        
        android.util.Log.d("ReportRepository", "Fetched ${reports.size} driver reports")
        reports
    }.onFailure { e ->
        android.util.Log.e("ReportRepository", "Error fetching driver reports", e)
    }

    /**
     * Get reports accessible to a user based on their role
     */
    suspend fun getReportsForUser(user: User, limit: Int = 50): Result<Any> = runCatching {
        when (user.role) {
            UserRole.ADMIN -> getAdminReports(limit)
            UserRole.DRIVER -> getDriverReports(user.id, limit)
        }
    }.let { result ->
        result.getOrNull() ?: Result.success(emptyList<Any>())
    }

    /**
     * Get report by ID - checks access permissions
     */
    suspend fun getReportById(reportId: String, currentUser: User): Result<Any?> = runCatching {
        when (currentUser.role) {
            UserRole.ADMIN -> {
                // Admins can view admin reports
                firestore.collection(reportsCollection).document(reportId).get().await()
                    .toObject(ParkingReport::class.java)
            }
            UserRole.DRIVER -> {
                // Drivers can only view their own driver reports
                val driverReport = firestore.collection(driverReportsCollection).document(reportId).get().await()
                    .toObject(DriverReport::class.java)
                
                // Verify the report belongs to this driver
                if (driverReport?.driverId == currentUser.id) {
                    driverReport
                } else {
                    null // Not authorized to view this report
                }
            }
        }
    }

    /**
     * Delete report - only the generator or admin can delete
     */
    suspend fun deleteReport(reportId: String, currentUser: User): Result<Unit> = runCatching {
        when (currentUser.role) {
            UserRole.ADMIN -> {
                firestore.collection(reportsCollection).document(reportId).delete().await()
            }
            UserRole.DRIVER -> {
                // Drivers can only delete their own reports
                val report = firestore.collection(driverReportsCollection).document(reportId).get().await()
                    .toObject(DriverReport::class.java)
                
                if (report?.driverId == currentUser.id) {
                    firestore.collection(driverReportsCollection).document(reportId).delete().await()
                } else {
                    throw IllegalAccessException("Not authorized to delete this report")
                }
            }
        }
    }

    /**
     * Get month boundaries for report period
     */
    private fun getMonthBoundaries(year: Int, month: Int): Pair<Timestamp, Timestamp> {
        val startCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return Pair(Timestamp(startCal.time), Timestamp(endCal.time))
    }
}
