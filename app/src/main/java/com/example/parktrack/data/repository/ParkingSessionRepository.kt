package com.example.parktrack.data.repository

import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.firestore.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

class ParkingSessionRepository {
    private val firestore = Firebase.firestore
    private val sessionsCollection = "parkingSessions"
    
    // Create new parking session (entry)
    suspend fun createSession(session: ParkingSession): Result<String> = runCatching {
        val documentRef = firestore.collection(sessionsCollection).document()
        val sessionWithId = session.copy(id = documentRef.id)
        documentRef.set(sessionWithId).await()
        documentRef.id
    }
    
    // Update session with exit time (exit)
    suspend fun completeSession(sessionId: String, exitTime: Timestamp): Result<Unit> = runCatching {
        val snapshot = firestore.collection(sessionsCollection).document(sessionId).get().await()
        val session = snapshot.toObject(ParkingSession::class.java)
        
        if (session != null && session.entryTime != null) {
            val durationMinutes = calculateDuration(session.entryTime!!, exitTime)
            firestore.collection(sessionsCollection).document(sessionId).update(
                mapOf(
                    "exitTime" to exitTime,
                    "status" to "COMPLETED",
                    "durationMinutes" to durationMinutes
                )
            ).await()
        }
    }
    
    // Get active session for a driver
    suspend fun getActiveSessionForDriver(driverId: String): Result<ParkingSession?> = runCatching {
        val snapshot = firestore.collection(sessionsCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .get()
            .await()
        
        snapshot.documents.firstOrNull()?.toObject(ParkingSession::class.java)
    }
    
    // Get all active sessions (for admin)
    suspend fun getAllActiveSessions(): Result<List<ParkingSession>> = runCatching {
        firestore.collection(sessionsCollection)
            .whereEqualTo("status", "ACTIVE")
            .get()
            .await()
            .toObjects(ParkingSession::class.java)
    }
    
    // Get driver's session history
    suspend fun getDriverSessions(driverId: String, limit: Int = 20): Result<List<ParkingSession>> = runCatching {
        firestore.collection(sessionsCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(ParkingSession::class.java)
    }
    
    // Listen to active session changes (real-time)
    fun observeActiveSession(driverId: String): Flow<ParkingSession?> = flow {
        val listener = firestore.collection(sessionsCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    return@addSnapshotListener
                }
                
                val session = snapshot?.documents?.firstOrNull()?.toObject(ParkingSession::class.java)
                // Note: This flow will emit the initial state, but continuous updates
                // require proper lifecycle management in the collecting component
            }
    }
    
    // Calculate duration in minutes
    fun calculateDuration(entryTime: Timestamp, exitTime: Timestamp): Long {
        val entryMillis = entryTime.toDate().time
        val exitMillis = exitTime.toDate().time
        return (exitMillis - entryMillis) / (1000 * 60)
    }
    
    // Get session by ID
    suspend fun getSessionById(sessionId: String): Result<ParkingSession?> = runCatching {
        firestore.collection(sessionsCollection).document(sessionId).get().await()
            .toObject(ParkingSession::class.java)
    }
    
    // Update session (for admin operations)
    suspend fun updateSession(sessionId: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        firestore.collection(sessionsCollection).document(sessionId).update(updates).await()
    }
    
    // Delete session (admin only)
    suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        firestore.collection(sessionsCollection).document(sessionId).delete().await()
    }
}
