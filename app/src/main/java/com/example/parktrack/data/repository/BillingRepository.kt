package com.example.parktrack.data.repository

import com.example.parktrack.billing.calculateParkingCharge
import com.example.parktrack.billing.TierPricing
import com.example.parktrack.data.model.DriverUnpaidSummary
import com.example.parktrack.data.model.Invoice
import com.example.parktrack.data.model.ParkingCharge
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.PaymentConfirmation
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.model.TierUpgradeRecord
import com.example.parktrack.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val chargesCollection = "parkingCharges"
    private val invoicesCollection = "invoices"

    /**
     * FIXED: Create new parking charge record on entry
     * NO CHARGE is calculated on entry - fee is calculated ONLY on exit for ALL tiers
     */
    suspend fun createCharge(
        sessionId: String,
        driverId: String,
        driverName: String,
        driverEmail: String,
        vehicleNumber: String,
        vehicleModel: String,
        parkingLotId: String,
        parkingLotName: String,
        entryTime: Timestamp,
        user: User
    ): Result<ParkingCharge> = runCatching {
        val tier = user.subscriptionTier
        
        // NO CHARGE on entry - fee calculated only on exit
        val charge = ParkingCharge(
            id = "${sessionId}_charge",
            sessionId = sessionId,
            driverId = driverId,
            driverName = driverName,
            driverEmail = driverEmail,
            vehicleNumber = vehicleNumber,
            vehicleModel = vehicleModel,
            parkingLotId = parkingLotId,
            parkingLotName = parkingLotName,
            entryTime = entryTime,
            subscriptionTier = tier.name,
            baseRate = when (tier) {
                SubscriptionTier.NORMAL -> TierPricing.NORMAL_HOURLY_RATE
                SubscriptionTier.GOLD -> TierPricing.GOLD_HOURLY_RATE
                SubscriptionTier.PLATINUM -> TierPricing.PLATINUM_HOURLY_RATE
            },
            calculatedCharge = 0.0, // No charge on entry
            finalCharge = 0.0, // Will be calculated on exit
            isPaid = false // All charges start as unpaid
        )

        val documentRef = firestore.collection(chargesCollection).document(charge.id)
        documentRef.set(charge).await()
        charge
    }

    /**
     * FIXED: Update charge on exit with final calculation
     * Fee is calculated ONLY on exit for ALL tiers
     */
    suspend fun updateChargeOnExit(
        chargeId: String,
        exitTime: Timestamp,
        durationMinutes: Long,
        subscriptionTier: SubscriptionTier
    ): Result<ParkingCharge> = runCatching {
        // Get existing charge
        val chargeDoc = firestore.collection(chargesCollection).document(chargeId).get().await()
        val existingCharge = chargeDoc.toObject(ParkingCharge::class.java)
            ?: throw Exception("Charge not found")

        // Calculate final charge on exit using billing logic
        val finalCharge = calculateParkingCharge(durationMinutes, subscriptionTier)

        val updates = mutableMapOf<String, Any>(
            "exitTime" to exitTime,
            "durationMinutes" to durationMinutes,
            "calculatedCharge" to finalCharge,
            "finalCharge" to finalCharge,
            "updatedAt" to Timestamp.now()
        )

        firestore.collection(chargesCollection).document(chargeId).update(updates).await()
        
        // Return updated charge
        existingCharge.copy(
            exitTime = exitTime,
            durationMinutes = durationMinutes,
            calculatedCharge = finalCharge,
            finalCharge = finalCharge
        )
    }

    /**
     * Create new parking charge (legacy method for backward compatibility)
     */
    suspend fun createCharge(charge: ParkingCharge): Result<String> = runCatching {
        val documentRef = firestore.collection(chargesCollection).document(charge.id)
        documentRef.set(charge).await()
        charge.id
    }

    /**
     * Get charges for a driver in a month
     */
    suspend fun getDriverCharges(driverId: String, yearMonth: String): Result<List<ParkingCharge>> = runCatching {
        firestore.collection(chargesCollection)
            .whereEqualTo("driverId", driverId)
            .get()
            .await()
            .toObjects(ParkingCharge::class.java)
            .filter { charge ->
                val entryMonth = getMonthYear(charge.entryTime)
                entryMonth == yearMonth
            }
    }

    /**
     * Get all charges for a driver
     */
    suspend fun getAllDriverCharges(driverId: String, limit: Int = 100): Result<List<ParkingCharge>> = runCatching {
        firestore.collection(chargesCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(ParkingCharge::class.java)
    }

    /**
     * Get unpaid charges for a driver with overdue detection
     */
    suspend fun getUnpaidCharges(driverId: String): Result<List<ParkingCharge>> = runCatching {
        val charges = firestore.collection(chargesCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("isPaid", false)
            .whereGreaterThan("finalCharge", 0.0)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(ParkingCharge::class.java)

        // Update overdue status for each charge
        charges.forEach { charge ->
            if (charge.shouldBeOverdue() && !charge.isOverdue) {
                val daysSinceSession = calculateDaysSinceSession(charge)
                if (daysSinceSession >= 7) {
                    markChargeAsOverdue(charge.id, daysSinceSession)
                }
            }
        }

        charges
    }

    /**
     * FIXED: Get comprehensive unpaid summary for a driver
     */
    suspend fun getDriverUnpaidSummary(driverId: String): Result<DriverUnpaidSummary> = runCatching {
        val unpaidCharges = getUnpaidCharges(driverId).getOrThrow()
        
        if (unpaidCharges.isEmpty()) {
            // Get driver info
            val userDoc = firestore.collection("users").document(driverId).get().await()
            val user = userDoc.toObject(User::class.java)
            
            DriverUnpaidSummary(
                driverId = driverId,
                driverName = user?.fullName ?: "Unknown",
                driverEmail = user?.email ?: "",
                totalUnpaidCharges = 0,
                totalUnpaidAmount = 0.0,
                unpaidCharges = emptyList(),
                oldestUnpaidDate = null
            )
        } else {
            val oldestUnpaid = unpaidCharges.minByOrNull { it.createdAt.toDate().time }
            val driverName = unpaidCharges.firstOrNull()?.driverName ?: "Unknown"
            val driverEmail = unpaidCharges.firstOrNull()?.driverEmail ?: ""
            
            DriverUnpaidSummary(
                driverId = driverId,
                driverName = driverName,
                driverEmail = driverEmail,
                totalUnpaidCharges = unpaidCharges.size,
                totalUnpaidAmount = unpaidCharges.sumOf { it.finalCharge },
                unpaidCharges = unpaidCharges,
                oldestUnpaidDate = oldestUnpaid?.createdAt
            )
        }
    }

    /**
     * Get ALL unpaid charges across all drivers (for admin)
     */
    suspend fun getAllUnpaidCharges(limit: Int = 100): Result<List<ParkingCharge>> = runCatching {
        firestore.collection(chargesCollection)
            .whereEqualTo("isPaid", false)
            .whereGreaterThan("finalCharge", 0.0)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(ParkingCharge::class.java)
    }

    /**
     * FIXED: Update charge payment status with admin confirmation
     */
    suspend fun updateChargePayment(
        chargeId: String, 
        isPaid: Boolean, 
        paymentMethod: String? = null,
        confirmedByAdminId: String? = null,
        confirmedByAdminName: String? = null
    ): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "isPaid" to isPaid,
            "updatedAt" to Timestamp.now()
        )
        if (isPaid) {
            updates["paymentDate"] = Timestamp.now()
            updates["paymentMethod"] = paymentMethod ?: "CASH"
            if (confirmedByAdminId != null) {
                updates["paymentConfirmedBy"] = confirmedByAdminId
            }
            if (confirmedByAdminName != null) {
                updates["paymentConfirmedByName"] = confirmedByAdminName
            }
            updates["isOverdue"] = false
            updates["overdueDays"] = 0
            updates["overdueCharge"] = 0.0
        }
        firestore.collection(chargesCollection).document(chargeId).update(updates).await()
    }

    /**
     * Create monthly invoice
     */
    suspend fun createInvoice(invoice: Invoice): Result<String> = runCatching {
        val documentRef = firestore.collection(invoicesCollection).document(invoice.id)
        documentRef.set(invoice).await()
        invoice.id
    }

    /**
     * Get invoice by ID
     */
    suspend fun getInvoiceById(invoiceId: String): Result<Invoice?> = runCatching {
        firestore.collection(invoicesCollection).document(invoiceId).get().await()
            .toObject(Invoice::class.java)
    }

    /**
     * Get driver's invoices
     */
    suspend fun getDriverInvoices(driverId: String): Result<List<Invoice>> = runCatching {
        firestore.collection(invoicesCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("month", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(Invoice::class.java)
    }

    /**
     * Get invoice for driver month
     */
    suspend fun getInvoiceForMonth(driverId: String, yearMonth: String): Result<Invoice?> = runCatching {
        val snapshot = firestore.collection(invoicesCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("month", yearMonth)
            .limit(1)
            .get()
            .await()
        snapshot.documents.firstOrNull()?.toObject(Invoice::class.java)
    }

    /**
     * Update invoice payment
     */
    suspend fun updateInvoicePayment(invoiceId: String, amountPaid: Double, paymentStatus: String): Result<Unit> = runCatching {
        val invoice = getInvoiceById(invoiceId).getOrNull() ?: throw Exception("Invoice not found")
        val newBalance = invoice.netAmount - amountPaid
        
        firestore.collection(invoicesCollection).document(invoiceId).update(mapOf(
            "amountPaid" to amountPaid,
            "balanceDue" to if (newBalance > 0) newBalance else 0.0,
            "isPaid" to (newBalance <= 0),
            "paymentStatus" to paymentStatus,
            "paidDate" to if (newBalance <= 0) Timestamp.now() else null,
            "updatedAt" to Timestamp.now()
        )).await()
    }

    /**
     * Get overdue invoices
     */
    suspend fun getOverdueInvoices(driverId: String): Result<List<Invoice>> = runCatching {
        firestore.collection(invoicesCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("paymentStatus", "OVERDUE")
            .get()
            .await()
            .toObjects(Invoice::class.java)
    }

    /**
     * Calculate monthly invoice for driver
     */
    suspend fun generateMonthlyInvoice(driverId: String, yearMonth: String): Result<Invoice> = runCatching {
        val charges = getDriverCharges(driverId, yearMonth).getOrThrow()
        
        val totalSessions = charges.size
        val totalDurationMinutes = charges.sumOf { it.durationMinutes }
        val totalCharges = charges.sumOf { it.finalCharge }
        val totalOverdue = charges.filter { it.isOverdue }.sumOf { it.overdueCharge }
        val amountPaid = charges.filter { it.isPaid }.sumOf { it.finalCharge }
        val netAmount = totalCharges + totalOverdue
        val balanceDue = netAmount - amountPaid

        val parts = yearMonth.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()

        val invoice = Invoice(
            id = "${driverId}_${yearMonth}",
            driverId = driverId,
            month = yearMonth,
            year = year,
            monthNumber = month,
            totalSessions = totalSessions,
            totalDurationMinutes = totalDurationMinutes,
            totalCharges = totalCharges,
            totalDiscount = charges.sumOf { it.discountApplied },
            totalOverdueCharges = totalOverdue,
            netAmount = netAmount,
            amountPaid = amountPaid,
            balanceDue = balanceDue,
            isPaid = balanceDue <= 0,
            paymentStatus = if (balanceDue <= 0) "PAID" else "PENDING",
            charges = charges.map { it.id }
        )

        createInvoice(invoice).getOrThrow()
        invoice
    }

    /**
     * Observe invoices (real-time)
     */
    fun observeDriverInvoices(driverId: String): Flow<List<Invoice>> = callbackFlow {
        val listener = firestore.collection(invoicesCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("month", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val invoices = snapshot?.toObjects(Invoice::class.java) ?: emptyList()
                trySend(invoices).isSuccess
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Helper function to extract month-year from Timestamp
     */
    private fun getMonthYear(timestamp: Timestamp?): String {
        if (timestamp == null) return ""
        val cal = Calendar.getInstance().apply {
            time = timestamp.toDate()
        }
        val year = cal.get(Calendar.YEAR)
        val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
        return "$year-$month"
    }

    // ==================== ADMIN FUNCTIONS ====================

    private val paymentConfirmationsCollection = "paymentConfirmations"
    private val tierUpgradeRecordsCollection = "tierUpgradeRecords"

    /**
     * Confirm payment for an invoice (Admin only)
     */
    suspend fun confirmInvoicePayment(
        invoiceId: String,
        amountPaid: Double,
        adminId: String,
        adminName: String,
        paymentMethod: String = "CASH",
        notes: String = ""
    ): Result<PaymentConfirmation> = runCatching {
        // Get the invoice
        val invoice = getInvoiceById(invoiceId).getOrThrow()
            ?: throw Exception("Invoice not found")

        // Create payment confirmation record
        val confirmation = PaymentConfirmation(
            id = UUID.randomUUID().toString(),
            driverId = invoice.driverId,
            invoiceId = invoiceId,
            amount = amountPaid,
            paymentMethod = paymentMethod,
            paymentType = "PARKING_CHARGE",
            confirmedByAdminId = adminId,
            confirmedByAdminName = adminName,
            notes = notes
        )

        // Save payment confirmation
        firestore.collection(paymentConfirmationsCollection)
            .document(confirmation.id)
            .set(confirmation)
            .await()

        // Update invoice payment status
        val newBalance = invoice.netAmount - (invoice.amountPaid + amountPaid)
        val isFullyPaid = newBalance <= 0

        firestore.collection(invoicesCollection).document(invoiceId).update(mapOf(
            "amountPaid" to (invoice.amountPaid + amountPaid),
            "balanceDue" to if (newBalance > 0) newBalance else 0.0,
            "isPaid" to isFullyPaid,
            "paymentStatus" to if (isFullyPaid) "PAID" else "PARTIAL",
            "paidDate" to if (isFullyPaid) Timestamp.now() else null,
            "updatedAt" to Timestamp.now()
        )).await()

        confirmation
    }

    /**
     * FIXED: Confirm payment for a specific charge with admin tracking
     */
    suspend fun confirmChargePayment(
        chargeId: String,
        adminId: String,
        adminName: String,
        paymentMethod: String = "CASH",
        notes: String = ""
    ): Result<PaymentConfirmation> = runCatching {
        // Get the charge
        val chargeDoc = firestore.collection(chargesCollection).document(chargeId).get().await()
        val charge = chargeDoc.toObject(ParkingCharge::class.java)
            ?: throw Exception("Charge not found")

        // Create payment confirmation record
        val confirmation = PaymentConfirmation(
            id = UUID.randomUUID().toString(),
            driverId = charge.driverId,
            chargeId = chargeId,
            amount = charge.finalCharge,
            paymentMethod = paymentMethod,
            paymentType = "PARKING_CHARGE",
            confirmedByAdminId = adminId,
            confirmedByAdminName = adminName,
            notes = notes
        )

        // Save payment confirmation
        firestore.collection(paymentConfirmationsCollection)
            .document(confirmation.id)
            .set(confirmation)
            .await()

        // Update charge payment status
        updateChargePayment(
            chargeId = chargeId,
            isPaid = true,
            paymentMethod = paymentMethod,
            confirmedByAdminId = adminId,
            confirmedByAdminName = adminName
        )

        confirmation
    }

    /**
     * Get all invoices for admin review
     */
    suspend fun getAllInvoices(limit: Int = 100): Result<List<Invoice>> = runCatching {
        firestore.collection(invoicesCollection)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(Invoice::class.java)
    }

    /**
     * Get pending invoices for admin review
     */
    suspend fun getPendingInvoices(limit: Int = 100): Result<List<Invoice>> = runCatching {
        firestore.collection(invoicesCollection)
            .whereEqualTo("paymentStatus", "PENDING")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(Invoice::class.java)
    }

    /**
     * Get payment confirmations for a driver
     */
    suspend fun getDriverPaymentConfirmations(driverId: String): Result<List<PaymentConfirmation>> = runCatching {
        firestore.collection(paymentConfirmationsCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(PaymentConfirmation::class.java)
    }

    /**
     * Create tier upgrade record
     */
    suspend fun createTierUpgradeRecord(
        driverId: String,
        driverEmail: String,
        driverName: String,
        fromTier: String,
        toTier: String,
        upgradeFee: Double,
        adminId: String,
        adminName: String
    ): Result<TierUpgradeRecord> = runCatching {
        val record = TierUpgradeRecord(
            id = UUID.randomUUID().toString(),
            driverId = driverId,
            driverEmail = driverEmail,
            driverName = driverName,
            fromTier = fromTier,
            toTier = toTier,
            upgradeFee = upgradeFee,
            isPaid = false,
            processedByAdminId = adminId,
            processedByAdminName = adminName
        )

        firestore.collection(tierUpgradeRecordsCollection)
            .document(record.id)
            .set(record)
            .await()

        record
    }

    /**
     * Confirm tier upgrade payment and activate the tier
     */
    suspend fun confirmTierUpgradePayment(
        upgradeRecordId: String,
        adminId: String,
        adminName: String,
        paymentMethod: String = "CASH",
        notes: String = ""
    ): Result<PaymentConfirmation> = runCatching {
        // Get the upgrade record
        val recordDoc = firestore.collection(tierUpgradeRecordsCollection)
            .document(upgradeRecordId)
            .get()
            .await()
        val record = recordDoc.toObject(TierUpgradeRecord::class.java)
            ?: throw Exception("Tier upgrade record not found")

        // Create payment confirmation
        val confirmation = PaymentConfirmation(
            id = UUID.randomUUID().toString(),
            driverId = record.driverId,
            amount = record.upgradeFee,
            paymentMethod = paymentMethod,
            paymentType = "TIER_UPGRADE",
            confirmedByAdminId = adminId,
            confirmedByAdminName = adminName,
            notes = notes
        )

        // Save payment confirmation
        firestore.collection(paymentConfirmationsCollection)
            .document(confirmation.id)
            .set(confirmation)
            .await()

        // Update upgrade record
        firestore.collection(tierUpgradeRecordsCollection)
            .document(upgradeRecordId)
            .update(mapOf(
                "isPaid" to true,
                "paymentConfirmationId" to confirmation.id,
                "processedAt" to Timestamp.now()
            ))
            .await()

        confirmation
    }

    /**
     * Get all charges for a driver
     */
    suspend fun getAllDriverCharges(driverId: String): Result<List<ParkingCharge>> = runCatching {
        firestore.collection(chargesCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(ParkingCharge::class.java)
    }
    
    /**
     * Observe all charges for a driver in real-time
     */
    fun observeDriverCharges(driverId: String): kotlinx.coroutines.flow.Flow<List<ParkingCharge>> {
        return callbackFlow {
            val listener = firestore.collection(chargesCollection)
                .whereEqualTo("driverId", driverId)
                .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val charges = snapshot?.toObjects(ParkingCharge::class.java) ?: emptyList()
                    trySend(charges)
                }
            
            awaitClose { listener.remove() }
        }
    }

    /**
     * Get tier upgrade records for a specific driver
     */
    suspend fun getDriverTierUpgradeRecords(driverId: String): Result<List<TierUpgradeRecord>> = runCatching {
        firestore.collection(tierUpgradeRecordsCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(TierUpgradeRecord::class.java)
    }

    /**
     * Get pending tier upgrades
     */
    suspend fun getPendingTierUpgrades(): Result<List<TierUpgradeRecord>> = runCatching {
        firestore.collection(tierUpgradeRecordsCollection)
            .whereEqualTo("isPaid", false)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(TierUpgradeRecord::class.java)
    }

    /**
     * Calculate days since parking session ended
     */
    private fun calculateDaysSinceSession(charge: ParkingCharge): Int {
        val now = System.currentTimeMillis()
        val sessionEndTime = charge.exitTime?.toDate()?.time ?: charge.entryTime?.toDate()?.time ?: now
        return ((now - sessionEndTime) / (24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Mark charge as overdue (for cron job or periodic check)
     */
    suspend fun markChargeAsOverdue(chargeId: String, overdueDays: Int): Result<Unit> = runCatching {
        val chargeDoc = firestore.collection(chargesCollection).document(chargeId).get().await()
        val charge = chargeDoc.toObject(ParkingCharge::class.java)
            ?: throw Exception("Charge not found")
        
        if (!charge.isPaid && charge.finalCharge > 0) {
            val overdueCharge = calculateOverdueCharge(charge.finalCharge, overdueDays)
            firestore.collection(chargesCollection).document(chargeId).update(mapOf(
                "isOverdue" to true,
                "overdueDays" to overdueDays,
                "overdueCharge" to overdueCharge,
                "updatedAt" to Timestamp.now()
            )).await()
        }
    }

    /**
     * Calculate overdue charge using billing logic
     */
    private fun calculateOverdueCharge(finalCharge: Double, daysOverdue: Int): Double {
        return finalCharge * 0.05 * daysOverdue // 5% per day
    }
}
