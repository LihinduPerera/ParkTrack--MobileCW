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
}