package com.example.parktrack.data.repository

import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    
    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = getUserData(authResult.user?.uid ?: "")
            if (user.isSuccess) {
                Result.success(user.getOrThrow())
            } else {
                Result.failure(Exception("User data not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun register(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: UserRole
    ): Result<User> {
        return try {
            // Create user in Firebase Auth
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            
            // Update profile with display name
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            
            authResult.user?.updateProfile(profileUpdates)?.await()
            
            // Create user document in Firestore
            val user = User(
                id = authResult.user?.uid ?: "",
                email = email,
                fullName = fullName,
                phoneNumber = phoneNumber,
                role = role
            )
            
            db.collection("users").document(user.id).set(user).await()
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserData(userId: String): Result<User> {
        return try {
            val document = db.collection("users").document(userId).get().await()
            if (document.exists()) {
                val user = document.toObject(User::class.java)
                if (user != null) {
                    Result.success(user)
                } else {
                    Result.failure(Exception("User data could not be parsed"))
                }
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAccount(firebaseAuth: FirebaseAuth) {
        val user = auth.currentUser
            ?: throw Exception("User not logged in")
        
        val userId = user.uid

        // Delete Firestore user document first
        db.collection("users")
            .document(userId)
            .delete()
            .await()

        // Delete Firebase Auth account
        user.delete().await()
    }

suspend fun updateAssignedGate(userId: String, gate: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("assignedGate", gate)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun incrementScanCount(userId: String): Result<Unit> {
        return try {
            val userDoc = db.collection("users").document(userId).get().await()
            if (userDoc.exists()) {
                val currentScans = userDoc.getLong("totalScans")?.toInt() ?: 0
                val currentTodayScans = userDoc.getLong("scansToday")?.toInt() ?: 0
                
                db.collection("users").document(userId)
                    .update(
                        mapOf(
                            "totalScans" to currentScans + 1,
                            "scansToday" to currentTodayScans + 1
                        )
                    )
                    .await()
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updateProfileImage(userId: String, uri: android.net.Uri): Result<String> {
        return try {
            // For now, just update the URL in Firestore
            // In a real implementation, you would upload to Firebase Storage first
            val imageUrl = uri.toString() // Placeholder - should upload to Storage
            db.collection("users").document(userId)
                .update("profileImageUrl", imageUrl)
                .await()
            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFullName(userId: String, fullName: String): Result<Unit> {
        return try {
            db.collection("users").document(userId)
                .update("fullName", fullName)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun logout() {
        auth.signOut()
    }
    
    fun isLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}