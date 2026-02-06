package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DriverDashboardViewModel @Inject constructor() : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private var historyListener: ListenerRegistration? = null

    private val _previousSessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val previousSessions: StateFlow<List<ParkingSession>> = _previousSessions.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    init {
        listenToPreviousSessions()
    }

    private fun listenToPreviousSessions() {
        val userId = auth.currentUser?.uid ?: return

        _isHistoryLoading.value = true

        //  Firestore Listener
        historyListener = firestore.collection("parkingSessions")
            .whereEqualTo("driverId", userId)
            .whereEqualTo("status", "COMPLETED")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _isHistoryLoading.value = false
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ParkingSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _previousSessions.value = sessions
                _isHistoryLoading.value = false
            }
    }

    // Stop listening when the user leaves the screen
    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
    }
}