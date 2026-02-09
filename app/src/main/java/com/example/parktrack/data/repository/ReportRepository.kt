package com.example.parktrack.data.repository

import com.example.parktrack.data.model.ParkingReport
import com.example.parktrack.data.model.ReportType
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
    private val billingRepository: BillingRepository
) {
    private val reportsCollection = "parkingReports"

    /**
     * Generate monthly report
     */
    suspend fun generateMonthlyReport(year: Int, month: Int, adminId: String): Result<ParkingReport> = runCatching {
        val (startDate, endDate) = getMonthBoundaries(year, month)
        
        // Get all sessions for the month
        val allSessions = firestore.collection("parkingSessions")
            .whereLessThanOrEqualTo("entryTime", endDate)
            .whereGreaterThanOrEqualTo("entryTime", startDate)
            .get()
            .await()
            .toObjects(com.example.parktrack.data.model.ParkingSession::class.java)

        val completedSessions = allSessions.filter { it.status == "COMPLETED" }
        val charges = firestore.collection("parkingCharges")
            .whereGreaterThanOrEqualTo("createdAt", startDate)
            .whereLessThanOrEqualTo("createdAt", endDate)
            .get()
            .await()
            .toObjects(com.example.parktrack.data.model.ParkingCharge::class.java)

        val totalRevenue = charges.sumOf { it.finalCharge }
        val amountCollected = charges.filter { it.isPaid }.sumOf { it.finalCharge }
        val outstandingAmount = totalRevenue - amountCollected
        val averageDuration = if (completedSessions.isNotEmpty()) {
            completedSessions.sumOf { it.durationMinutes } / completedSessions.size
        } else 0L

        val report = ParkingReport(
            id = "${year}-${month}-${System.currentTimeMillis()}",
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
            paidSessions = charges.filter { it.isPaid }.size,
            unpaidSessions = charges.filter { !it.isPaid }.size,
            overdueSessions = charges.filter { it.isOverdue }.size,
            generatedBy = adminId
        )

        // Save report to Firestore
        firestore.collection(reportsCollection).document(report.id).set(report).await()
        report
    }

    /**
     * Get report by ID
     */
    suspend fun getReportById(reportId: String): Result<ParkingReport?> = runCatching {
        firestore.collection(reportsCollection).document(reportId).get().await()
            .toObject(ParkingReport::class.java)
    }

    /**
     * Get all reports
     */
    suspend fun getAllReports(limit: Int = 50): Result<List<ParkingReport>> = runCatching {
        firestore.collection(reportsCollection)
            .orderBy("periodStart", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(ParkingReport::class.java)
    }

    /**
     * Get reports by type
     */
    suspend fun getReportsByType(reportType: ReportType, limit: Int = 50): Result<List<ParkingReport>> = runCatching {
        firestore.collection(reportsCollection)
            .whereEqualTo("reportType", reportType.name)
            .orderBy("periodStart", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(ParkingReport::class.java)
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
        }

        val endCal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month - 1)
            set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
        }

        return Pair(Timestamp(startCal.time), Timestamp(endCal.time))
    }

    /**
     * Delete old reports (for cleanup)
     */
    suspend fun deleteReport(reportId: String): Result<Unit> = runCatching {
        firestore.collection(reportsCollection).document(reportId).delete().await()
    }
}
