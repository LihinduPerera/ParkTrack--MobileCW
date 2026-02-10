package com.example.parktrack.data.repository

import com.example.parktrack.data.model.ParkingLot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ParkingLotRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val parkingLotsCollection = "parkingLots"

/**
     * Get all parking lots
     */
    suspend fun getAllParkingLots(): Result<List<ParkingLot>> = runCatching {
        firestore.collection(parkingLotsCollection)
            .orderBy("name")
            .get()
            .await()
            .toObjects(ParkingLot::class.java)
    }

    /**
     * Get all parking lots (simplified version for ViewModel)
     */
    suspend fun getAllParkingLotsList(): List<ParkingLot> {
        return try {
            firestore.collection(parkingLotsCollection)
                .orderBy("name")
                .get()
                .await()
                .toObjects(ParkingLot::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get parking lot by ID
     */
    suspend fun getParkingLotById(lotId: String): Result<ParkingLot?> = runCatching {
        firestore.collection(parkingLotsCollection).document(lotId).get().await()
            .toObject(ParkingLot::class.java)
    }

    /**
     * Add new parking lot (admin only)
     */
    suspend fun addParkingLot(parkingLot: ParkingLot): Result<String> = runCatching {
        val documentRef = firestore.collection(parkingLotsCollection).document(parkingLot.id)
        documentRef.set(parkingLot).await()
        parkingLot.id
    }

    /**
     * Update parking lot
     */
    suspend fun updateParkingLot(lotId: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        firestore.collection(parkingLotsCollection).document(lotId).update(updates).await()
    }

    /**
     * Delete parking lot
     */
    suspend fun deleteParkingLot(lotId: String): Result<Unit> = runCatching {
        firestore.collection(parkingLotsCollection).document(lotId).delete().await()
    }

    /**
     * Update available spaces
     */
    suspend fun updateAvailableSpaces(lotId: String, delta: Int): Result<Unit> = runCatching {
        firestore.runTransaction { transaction ->
            val docRef = firestore.collection(parkingLotsCollection).document(lotId)
            val snapshot = transaction.get(docRef)
            val lot = snapshot.toObject(ParkingLot::class.java)
            
            if (lot != null) {
                val newAvailable = (lot.availableSpaces + delta).coerceIn(0, lot.totalSpaces)
                val newOccupied = lot.totalSpaces - newAvailable
                
                transaction.update(docRef, mapOf(
                    "availableSpaces" to newAvailable,
                    "occupiedSpaces" to newOccupied,
                    "updatedAt" to System.currentTimeMillis()
                ))
            }
        }.await()
    }

    /**
     * Observe parking lot availability (real-time)
     */
    fun observeParkingLot(lotId: String): Flow<ParkingLot?> = callbackFlow {
        val listener = firestore.collection(parkingLotsCollection).document(lotId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val lot = snapshot?.toObject(ParkingLot::class.java)
                trySend(lot).isSuccess
            }
        
        awaitClose { listener.remove() }
    }

    /**
     * Search parking lots by city
     */
    suspend fun getParkingLotsByCity(city: String): Result<List<ParkingLot>> = runCatching {
        firestore.collection(parkingLotsCollection)
            .whereEqualTo("city", city)
            .get()
            .await()
            .toObjects(ParkingLot::class.java)
    }

    /**
     * Get nearby parking lots (within radius)
     */
    suspend fun getNearbyParkingLots(latitude: Double, longitude: Double, radiusKm: Double): Result<List<ParkingLot>> = runCatching {
        // This is a simplified version - Firestore doesn't have built-in geo queries
        // For production, consider using Firestore with GeoHash library or Cloud Functions
        val lots = firestore.collection(parkingLotsCollection)
            .get()
            .await()
            .toObjects(ParkingLot::class.java)
        
        lots.filter { lot ->
            val distance = calculateDistance(latitude, longitude, lot.latitude, lot.longitude)
            distance <= radiusKm
        }
    }

    /**
     * Calculate distance between two coordinates (Haversine formula)
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371 // Earth's radius in km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.asin(Math.sqrt(a))
        return R * c
    }
}
