package com.example.parktrack.data.repository

import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val usersCollection = "users"

    /**
     * Get user by ID
     */
    suspend fun getUserById(userId: String): User? {
        return try {
            val document = firestore.collection(usersCollection)
                .document(userId)
                .get()
                .await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update user subscription tier
     */
    suspend fun updateSubscriptionTier(userId: String, newTier: SubscriptionTier): Result<Unit> {
        return try {
            firestore.collection(usersCollection)
                .document(userId)
                .update("subscriptionTier", newTier.name)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firestore.collection(usersCollection)
                .document(userId)
                .update(updates)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all users (for admin purposes)
     */
    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(usersCollection)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get users by subscription tier
     */
    suspend fun getUsersByTier(tier: SubscriptionTier): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(usersCollection)
                .whereEqualTo("subscriptionTier", tier.name)
                .get()
                .await()
            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Create new user
     */
    suspend fun createUser(user: User): Result<String> {
        return try {
            val documentRef = firestore.collection(usersCollection)
                .document(user.id)
            documentRef.set(user).await()
            Result.success(user.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete user
     */
    suspend fun deleteUser(userId: String): Result<Unit> {
        return try {
            firestore.collection(usersCollection)
                .document(userId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== ADMIN FUNCTIONS ====================

    /**
     * Search users by email (for admin to find drivers)
     */
    suspend fun searchUsersByEmail(emailQuery: String): Result<List<User>> {
        return try {
            // Firebase doesn't support partial text search, so we get all users and filter
            // In production, consider using Algolia or similar for better search
            val snapshot = firestore.collection(usersCollection)
                .orderBy("email")
                .startAt(emailQuery.lowercase())
                .endAt(emailQuery.lowercase() + "\uf8ff")
                .limit(20)
                .get()
                .await()

            val users = snapshot.toObjects(User::class.java)
                .filter { it.email.contains(emailQuery, ignoreCase = true) }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get user by email (exact match)
     */
    suspend fun getUserByEmail(email: String): Result<User?> {
        return try {
            val snapshot = firestore.collection(usersCollection)
                .whereEqualTo("email", email.lowercase())
                .limit(1)
                .get()
                .await()

            val user = snapshot.documents.firstOrNull()?.toObject(User::class.java)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get all drivers (users with DRIVER role)
     */
    suspend fun getAllDrivers(): Result<List<User>> {
        return try {
            val snapshot = firestore.collection(usersCollection)
                .whereEqualTo("role", "DRIVER")
                .orderBy("name")
                .get()
                .await()

            val users = snapshot.toObjects(User::class.java)
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update user tier by admin (after payment confirmation)
     */
    suspend fun updateUserTierByAdmin(
        userId: String,
        newTier: SubscriptionTier,
        adminId: String,
        adminName: String
    ): Result<Unit> {
        return try {
            val updates = mapOf(
                "subscriptionTier" to newTier.name,
                "tierUpdatedBy" to adminId,
                "tierUpdatedByName" to adminName,
                "tierUpdatedAt" to System.currentTimeMillis()
            )

            firestore.collection(usersCollection)
                .document(userId)
                .update(updates)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get users by email domain or partial match (admin search)
     */
    suspend fun searchDrivers(searchQuery: String): Result<List<User>> {
        return try {
            // Get all drivers and filter locally for more flexible search
            val snapshot = firestore.collection(usersCollection)
                .whereEqualTo("role", "DRIVER")
                .limit(100)
                .get()
                .await()

            val allDrivers = snapshot.toObjects(User::class.java)

            // Filter by search query (name, email, or vehicle number)
            val filteredDrivers = if (searchQuery.isBlank()) {
                allDrivers
            } else {
                allDrivers.filter { user ->
                    user.email.contains(searchQuery, ignoreCase = true) ||
                    user.name.contains(searchQuery, ignoreCase = true) ||
                    user.fullName.contains(searchQuery, ignoreCase = true) ||
                    user.vehicleNumber.contains(searchQuery, ignoreCase = true)
                }
            }

            Result.success(filteredDrivers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}