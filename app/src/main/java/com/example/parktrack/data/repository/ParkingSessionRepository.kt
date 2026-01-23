package com.example.parktrack.data.repository

import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.firestore.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class ParkingSessionRepository {
    private val firestore = Firebase.firestore
    private val sessionsCollection = "parkingSessions"
    
    /**
     * Create new parking session (entry scan)
     */
    suspend fun createSession(session: ParkingSession): Result<String> = runCatching {
        val documentRef = firestore.collection(sessionsCollection).document(session.id)
        documentRef.set(session).await()
        session.id
    }
    
    /**
     * Complete parking session (exit scan)
     */
    suspend fun completeSession(sessionId: String, exitTime: Timestamp): Result<Unit> = runCatching {
        val snapshot = firestore.collection(sessionsCollection).document(sessionId).get().await()
        val session = snapshot.toObject(ParkingSession::class.java)
            ?: throw Exception("Session not found")
        
        if (session.entryTime == null) {
            throw Exception("Invalid session: no entry time")
        }
        
        val durationMinutes = calculateDuration(session.entryTime!!, exitTime)
        
        firestore.collection(sessionsCollection).document(sessionId).update(
            mapOf(
                "exitTime" to exitTime,
                "status" to "COMPLETED",
                "durationMinutes" to durationMinutes
            )
        ).await()
    }
    
    /**
     * Get active session for a driver (suspend function)
     */
    suspend fun getActiveSessionForDriver(driverId: String): Result<ParkingSession?> = runCatching {
        val snapshot = firestore.collection(sessionsCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .get()
            .await()
        
        snapshot.documents.firstOrNull()?.toObject(ParkingSession::class.java)
    }
    
    /**
     * Listen to active session changes (real-time)
     */
    fun observeActiveSession(driverId: String): Flow<ParkingSession?> = callbackFlow {
        val listener = firestore.collection(sessionsCollection)
            .whereEqualTo("driverId", driverId)
            .whereEqualTo("status", "ACTIVE")
            .limit(1)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val session = snapshot?.documents?.firstOrNull()
                    ?.toObject(ParkingSession::class.java)
                
                trySend(session).isSuccess
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get all active sessions (for admin)
     */
    suspend fun getAllActiveSessions(): Result<List<ParkingSession>> = runCatching {
        firestore.collection(sessionsCollection)
            .whereEqualTo("status", "ACTIVE")
            .orderBy("entryTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .await()
            .toObjects(ParkingSession::class.java)
    }
    
    /**
     * Listen to all active sessions (real-time, for admin)
     */
    fun observeAllActiveSessions(): Flow<List<ParkingSession>> = callbackFlow {
        val listener = firestore.collection(sessionsCollection)
            .whereEqualTo("status", "ACTIVE")
            .orderBy("entryTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                
                val sessions = snapshot?.toObjects(ParkingSession::class.java) ?: emptyList()
                trySend(sessions).isSuccess
            }
        
        awaitClose { listener.remove() }
    }
    
    /**
     * Get driver's session history
     */
    suspend fun getDriverSessions(driverId: String, limit: Int = 20): Result<List<ParkingSession>> = runCatching {
        firestore.collection(sessionsCollection)
            .whereEqualTo("driverId", driverId)
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .await()
            .toObjects(ParkingSession::class.java)
    }
    
    /**
     * Get session by ID
     */
    suspend fun getSessionById(sessionId: String): Result<ParkingSession?> = runCatching {
        firestore.collection(sessionsCollection).document(sessionId).get().await()
            .toObject(ParkingSession::class.java)
    }
    
    /**
     * Update session manually (for admin manual exit)
     */
    suspend fun updateSession(sessionId: String, updates: Map<String, Any>): Result<Unit> = runCatching {
        firestore.collection(sessionsCollection).document(sessionId).update(updates).await()
    }
    
    /**
     * Delete session (admin only)
     */
    suspend fun deleteSession(sessionId: String): Result<Unit> = runCatching {
        firestore.collection(sessionsCollection).document(sessionId).delete().await()
    }
    
    /**
     * Calculate duration between two timestamps in minutes
     */
    fun calculateDuration(entryTime: Timestamp, exitTime: Timestamp): Long {
        val durationMillis = exitTime.toDate().time - entryTime.toDate().time
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    }
    
    /**
     * Calculate duration from entry time to now (for live counter)
     */
    fun calculateCurrentDuration(entryTime: Timestamp): Long {
        val durationMillis = System.currentTimeMillis() - entryTime.toDate().time
        return TimeUnit.MILLISECONDS.toMinutes(durationMillis)
    }
    
    /**
     * Format duration to readable string
     */
    fun formatDurationString(minutes: Long): String {
        val hours = minutes / 60
        val mins = minutes % 60
        return when {
            hours > 0 -> "${hours}h ${mins}m"
            else -> "${mins}m"
        }
    }
}