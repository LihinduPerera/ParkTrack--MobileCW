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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class DriverDashboardViewModel @Inject constructor() : ViewModel() {

    private val firestore = Firebase.firestore
    private val auth = Firebase.auth

    private var historyListener: ListenerRegistration? = null

    private val _previousSessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val previousSessions: StateFlow<List<ParkingSession>> = _previousSessions.asStateFlow()

    private val _todaySessionsCount = MutableStateFlow(0)
    val todaySessionsCount: StateFlow<Int> = _todaySessionsCount.asStateFlow()

    private val _todayTotalMinutes = MutableStateFlow(0L)
    val todayTotalMinutes: StateFlow<Long> = _todayTotalMinutes.asStateFlow()

    private val _monthlySessions = MutableStateFlow(0)
    val monthlySessions: StateFlow<Int> = _monthlySessions.asStateFlow()

    private val _monthlyMinutes = MutableStateFlow(0L)
    val monthlyMinutes: StateFlow<Long> = _monthlyMinutes.asStateFlow()

    private val _memberSince = MutableStateFlow("")
    val memberSince: StateFlow<String> = _memberSince.asStateFlow()

    private val _recentThreeSessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val recentThreeSessions: StateFlow<List<ParkingSession>> = _recentThreeSessions.asStateFlow()

    init {
        loadAllSessions()
        loadMemberSince()
    }

    private fun loadMemberSince() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val raw = doc.get("createdAt")
                val date = when (raw) {
                    is com.google.firebase.Timestamp -> raw.toDate()
                    is Long -> Date(raw)
                    else -> null
                }
                date?.let {
                    _memberSince.value =
                        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it)
                }
            }
    }

    private fun loadAllSessions() {
        val userId = auth.currentUser?.uid ?: return

        historyListener = firestore.collection("parkingSessions")
            .whereEqualTo("driverId", userId)
            .whereEqualTo("status", "COMPLETED")
            .addSnapshotListener { snapshot, _ ->

                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(ParkingSession::class.java)?.copy(id = it.id)
                } ?: emptyList()

                _previousSessions.value = sessions.sortedByDescending { it.entryTime }
                _recentThreeSessions.value = _previousSessions.value.take(3)

                calculateStatsFromSessions(sessions)
            }
    }

    private fun calculateStatsFromSessions(sessions: List<ParkingSession>) {

        val now = Calendar.getInstance()

        var todayCount = 0
        var todayMinutes = 0L
        var monthCount = 0
        var monthMinutes = 0L

        sessions.forEach { session ->
            val entry = session.entryTime?.toDate() ?: return@forEach
            val cal = Calendar.getInstance().apply { time = entry }

            val isToday =
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)

            val isThisMonth =
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.MONTH) == now.get(Calendar.MONTH)

            if (isToday) {
                todayCount++
                todayMinutes += session.durationMinutes
            }

            if (isThisMonth) {
                monthCount++
                monthMinutes += session.durationMinutes
            }
        }

        _todaySessionsCount.value = todayCount
        _todayTotalMinutes.value = todayMinutes
        _monthlySessions.value = monthCount
        _monthlyMinutes.value = monthMinutes
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
    }
}
