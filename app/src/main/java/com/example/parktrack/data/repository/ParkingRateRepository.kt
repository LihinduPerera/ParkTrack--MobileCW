package com.example.parktrack.data.repository

import com.example.parktrack.data.model.ParkingRate
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingRateRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val ratesCollection = "parkingRates"

    /**
     * Get rates for a parking lot
     */
    suspend fun getRatesForLot(parkingLotId: String): Result<List<ParkingRate>> = runCatching {
        firestore.collection(ratesCollection)
            .whereEqualTo("parkingLotId", parkingLotId)
            .whereEqualTo("isActive", true)
            .get()
            .await()
            .toObjects(ParkingRate::class.java)
    }

    /**
     * Get specific rate
     */
    suspend fun getRate(rateId: String): Result<ParkingRate?> = runCatching {
        firestore.collection(ratesCollection).document(rateId).get().await()
            .toObject(ParkingRate::class.java)
    }

    /**
     * Get rate for a lot by type
     */
    suspend fun getRateByType(parkingLotId: String, rateType: String): Result<ParkingRate?> = runCatching {
        val snapshot = firestore.collection(ratesCollection)
            .whereEqualTo("parkingLotId", parkingLotId)
            .whereEqualTo("rateType", rateType)
            .whereEqualTo("isActive", true)
            .limit(1)
            .get()
            .await()
        snapshot.documents.firstOrNull()?.toObject(ParkingRate::class.java)
    }

    /**
     * Add new rate
     */
    suspend fun addRate(rate: ParkingRate): Result<String> = runCatching {
        val documentRef = firestore.collection(ratesCollection).document(rate.id)
        documentRef.set(rate).await()
        rate.id
    }

    /**
     * Update rate
     */
    suspend fun updateRate(rateId: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        firestore.collection(ratesCollection).document(rateId).update(updates).await()
    }

    /**
     * Deactivate rate
     */
    suspend fun deactivateRate(rateId: String): Result<Unit> = runCatching {
        firestore.collection(ratesCollection).document(rateId)
            .update(mapOf("isActive" to false))
            .await()
    }

    /**
     * Calculate parking charge
     */
    fun calculateCharge(
        durationMinutes: Long,
        baseRate: Double,
        rateType: String,
        maxDailyPrice: Double = Double.MAX_VALUE
    ): Double {
        val hours = (durationMinutes + 59) / 60 // Round up
        val charge = when (rateType.uppercase()) {
            "VIP" -> baseRate * hours * 1.5
            "OVERNIGHT" -> baseRate * 0.5 * hours
            "HOURLY" -> baseRate * hours
            else -> baseRate * hours
        }
        return minOf(charge, maxDailyPrice)
    }

    /**
     * Calculate charge with minimum
     */
    fun calculateChargeWithMinimum(
        durationMinutes: Long,
        baseRate: Double,
        rateType: String,
        minChargeHours: Double = 1.0,
        maxDailyPrice: Double = Double.MAX_VALUE
    ): Double {
        val hours = maxOf((durationMinutes) / 60.0, minChargeHours)
        val charge = when (rateType.uppercase()) {
            "VIP" -> baseRate * hours * 1.5
            "OVERNIGHT" -> baseRate * 0.5 * hours
            "HOURLY" -> baseRate * hours
            else -> baseRate * hours
        }
        return minOf(charge, maxDailyPrice)
    }
}
