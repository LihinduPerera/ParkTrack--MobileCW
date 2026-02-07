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
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class DriverDashboardViewModel @Inject constructor() : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private var historyListener: ListenerRegistration? = null
    private var statsListener: ListenerRegistration? = null

    private val _previousSessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val previousSessions: StateFlow<List<ParkingSession>> = _previousSessions.asStateFlow()

    private val _isHistoryLoading = MutableStateFlow(false)
    val isHistoryLoading: StateFlow<Boolean> = _isHistoryLoading.asStateFlow()

    private val _todaySessionsCount = MutableStateFlow(0)
    val todaySessionsCount: StateFlow<Int> = _todaySessionsCount.asStateFlow()

    private val _todayTotalMinutes = MutableStateFlow(0L)
    val todayTotalMinutes: StateFlow<Long> = _todayTotalMinutes.asStateFlow()

    init {
        listenToPreviousSessions()
        listenToTodayStats()
    }

    private fun listenToPreviousSessions() {
        val userId = auth.currentUser?.uid ?: return
        _isHistoryLoading.value = true

        historyListener = firestore.collection("parkingSessions")
            .whereEqualTo("driverId", userId)
            .whereEqualTo("status", "COMPLETED")
            .limit(5)
            .addSnapshotListener { snapshot, _ ->

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ParkingSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                _previousSessions.value = sessions.sortedByDescending { it.entryTime }
                _isHistoryLoading.value = false
            }
    }

    private fun listenToTodayStats() {
        val userId = auth.currentUser?.uid ?: return

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        val startOfDayTimestamp = com.google.firebase.Timestamp(calendar.time)

        statsListener = firestore.collection("parkingSessions")
            .whereEqualTo("driverId", userId)
            .whereGreaterThanOrEqualTo("entryTime", startOfDayTimestamp)
            .addSnapshotListener { snapshot, _ ->

                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(ParkingSession::class.java)
                } ?: emptyList()

                // Count sessions today
                _todaySessionsCount.value = sessions.size

                // Sum durations safely
                _todayTotalMinutes.value =
                    sessions.sumOf { it.durationMinutes ?: 0L }
            }
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
        statsListener?.remove()
    }
}
