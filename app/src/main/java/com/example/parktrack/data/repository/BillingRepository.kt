package com.example.parktrack.data.repository

import com.example.parktrack.billing.calculateParkingCharge
import com.example.parktrack.data.model.Invoice
import com.example.parktrack.data.model.ParkingCharge
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.User
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BillingRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val chargesCollection = "parkingCharges"
    private val invoicesCollection = "invoices"

    /**
     * Create new parking charge with billing calculation
     */
    suspend fun createCharge(
        sessionId: String,
        driverId: String,
        vehicleNumber: String,
        parkingLotId: String,
        parkingLotName: String,
        entryTime: Timestamp,
        exitTime: Timestamp,
        durationMinutes: Long,
        user: User,
        parkingRate: ParkingRate
    ): Result<String> = runCatching {
        // Calculate charge using new billing system
        val calculatedCharge = calculateParkingCharge(durationMinutes, user.subscriptionTier, parkingRate)

        val charge = ParkingCharge(
            id = "${sessionId}_charge",
            sessionId = sessionId,
            driverId = driverId,
            vehicleNumber = vehicleNumber,
            parkingLotId = parkingLotId,
            parkingLotName = parkingLotName,
            entryTime = entryTime,
            exitTime = exitTime,
            durationMinutes = durationMinutes,
            rateType = parkingRate.rateType.name,
            baseRate = parkingRate.basePricePerHour,
            chargeableAmount = calculatedCharge,
            calculatedCharge = calculatedCharge,
            discountApplied = 0.0, // Can be enhanced later
            finalCharge = calculatedCharge
        )

        val documentRef = firestore.collection(chargesCollection).document(charge.id)
        documentRef.set(charge).await()
        charge.id
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
     * Get unpaid charges for a driver
     */
    suspend fun getUnpaidCharges(driverId: String): Result<List<ParkingCharge>> = runCatching {
        firestore.collection(chargesCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("isPaid", false)
            .get()
            .await()
            .toObjects(ParkingCharge::class.java)
    }

    /**
     * Update charge payment status
     */
    suspend fun updateChargePayment(chargeId: String, isPaid: Boolean, paymentMethod: String? = null): Result<Unit> = runCatching {
        val updates = mutableMapOf<String, Any>(
            "isPaid" to isPaid,
            "updatedAt" to Timestamp.now()
        )
        if (isPaid) {
            updates["paymentDate"] = Timestamp.now()
            updates["paymentMethod"] = paymentMethod ?: "CASH"
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
}
