package com.example.parktrack.data.repository

import com.example.parktrack.data.model.Vehicle
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val vehiclesCollection = "vehicles"

    /**
     * Add new vehicle for a driver
     */
    suspend fun addVehicle(vehicle: Vehicle): Result<String> = runCatching {
        val vehicleData = vehicle.copy(isActive = true)
        val documentRef = firestore.collection(vehiclesCollection).document(vehicle.id)
        documentRef.set(vehicleData).await()
        vehicle.id
    }

    /**
     * Get all vehicles for a driver
     */
    suspend fun getDriverVehicles(driverId: String): Result<List<Vehicle>> = runCatching {
        firestore.collection(vehiclesCollection)
            .whereEqualTo("ownerId", driverId)
            .get()
            .await()
            .toObjects(Vehicle::class.java)
    }

    /**
     * Get vehicle by ID
     */
    suspend fun getVehicleById(vehicleId: String): Result<Vehicle?> = runCatching {
        firestore.collection(vehiclesCollection).document(vehicleId).get().await()
            .toObject(Vehicle::class.java)
    }

    /**
     * Get vehicle by registration number
     */
    suspend fun getVehicleByNumber(vehicleNumber: String): Result<Vehicle?> = runCatching {
        val snapshot = firestore.collection(vehiclesCollection)
            .whereEqualTo("vehicleNumber", vehicleNumber)
            .limit(1)
            .get()
            .await()
        snapshot.documents.firstOrNull()?.toObject(Vehicle::class.java)
    }

    /**
     * Update vehicle
     */
    suspend fun updateVehicle(vehicleId: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        firestore.collection(vehiclesCollection).document(vehicleId).update(updates).await()
    }

    /**
     * Delete (deactivate) vehicle
     */
    suspend fun deleteVehicle(vehicleId: String): Result<Unit> = runCatching {
        firestore.collection(vehiclesCollection).document(vehicleId)
            .update(mapOf("isActive" to false))
            .await()
    }

    /**
     * Search vehicles by number
     */
    suspend fun searchVehicles(searchQuery: String): Result<List<Vehicle>> = runCatching {
        firestore.collection(vehiclesCollection)
            .whereGreaterThanOrEqualTo("vehicleNumber", searchQuery)
            .whereLessThan("vehicleNumber", searchQuery + "\uf8ff")
            .limit(10)
            .get()
            .await()
            .toObjects(Vehicle::class.java)
    }
}
