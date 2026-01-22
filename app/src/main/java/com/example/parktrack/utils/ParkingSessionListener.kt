package com.example.parktrack.utils

import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Utility class for managing real-time Firestore listeners for parking sessions
 */
object ParkingSessionListener {
    
    /**
     * Listen to active sessions for a specific driver in real-time
     * @param sessionsCollection The Firestore collection reference
     * @param driverId The driver's ID
     * @return Flow emitting ParkingSession when status is ACTIVE
     */
    fun observeDriverActiveSession(
        sessionsCollection: CollectionReference,
        driverId: String
    ): Flow<ParkingSession?> = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            listener = sessionsCollection
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
                    
                    trySend(session)
                }
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            listener?.remove()
        }
    }
    
    /**
     * Listen to all active sessions (for admin view)
     * @param sessionsCollection The Firestore collection reference
     * @return Flow emitting list of active ParkingSessions
     */
    fun observeAllActiveSessions(
        sessionsCollection: CollectionReference
    ): Flow<List<ParkingSession>> = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            listener = sessionsCollection
                .whereEqualTo("status", "ACTIVE")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val sessions = snapshot?.toObjects(ParkingSession::class.java) ?: emptyList()
                    trySend(sessions)
                }
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            listener?.remove()
        }
    }
    
    /**
     * Listen to specific session by ID
     * @param sessionsCollection The Firestore collection reference
     * @param sessionId The session ID
     * @return Flow emitting ParkingSession
     */
    fun observeSessionById(
        sessionsCollection: CollectionReference,
        sessionId: String
    ): Flow<ParkingSession?> = callbackFlow {
        var listener: ListenerRegistration? = null
        
        try {
            listener = sessionsCollection.document(sessionId)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    
                    val session = snapshot?.toObject(ParkingSession::class.java)
                    trySend(session)
                }
        } catch (e: Exception) {
            close(e)
        }
        
        awaitClose {
            listener?.remove()
        }
    }
}
