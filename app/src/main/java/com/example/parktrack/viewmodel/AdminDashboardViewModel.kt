package com.example.parktrack.viewmodel
import androidx.lifecycle.ViewModel
import com.example.parktrack.data.model.ParkingSession
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AdminDashboardViewModel @Inject constructor() : ViewModel() {

    private val firestore = Firebase.firestore
    private var listener: ListenerRegistration? = null

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _totalScansToday = MutableStateFlow(0)
    val totalScansToday: StateFlow<Int> = _totalScansToday.asStateFlow()

    private val _activeNow = MutableStateFlow(0)
    val activeNow: StateFlow<Int> = _activeNow.asStateFlow()

    private val _entriesToday = MutableStateFlow(0)
    val entriesToday: StateFlow<Int> = _entriesToday.asStateFlow()

    private val _exitsToday = MutableStateFlow(0)
    val exitsToday: StateFlow<Int> = _exitsToday.asStateFlow()

    private val _recentScans = MutableStateFlow<List<ParkingSession>>(emptyList())
    val recentScans: StateFlow<List<ParkingSession>> = _recentScans.asStateFlow()

    private val _last6hChart = MutableStateFlow(List(6) { 0 })
    val last6hChart: StateFlow<List<Int>> = _last6hChart.asStateFlow()

    init { listenRealtime() }

    fun refresh() {
        _isRefreshing.value = true
        firestore.collection("parkingSessions").get()
            .addOnSuccessListener {
                val sessions = it.documents.mapNotNull { d ->
                    d.toObject(ParkingSession::class.java)
                }
                processSessions(sessions)
                _isRefreshing.value = false
            }
            .addOnFailureListener { _isRefreshing.value = false }
    }

    private fun listenRealtime() {
        listener = firestore.collection("parkingSessions")
            .addSnapshotListener { snapshot, _ ->
                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(ParkingSession::class.java)
                } ?: return@addSnapshotListener

                processSessions(sessions)
            }
    }

    private fun processSessions(sessions: List<ParkingSession>) {
        val now = Calendar.getInstance()
        var total = 0
        var active = 0
        var entries = 0
        var exits = 0
        val hourBuckets = MutableList(6) { 0 }

        sessions.forEach { session ->

            val entryDate = session.entryTime?.toDate()
            val exitDate = session.exitTime?.toDate()

            val isEntryToday = entryDate?.let {
                val cal = Calendar.getInstance().apply { time = it }
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            } ?: false

            val isExitToday = exitDate?.let {
                val cal = Calendar.getInstance().apply { time = it }
                cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
            } ?: false

            // TOTAL SCANS = entries + exits today
            if (isEntryToday) {
                total++
                entries++

                val entryHour = Calendar.getInstance().apply { time = entryDate!! }.get(Calendar.HOUR_OF_DAY)
                val diff = now.get(Calendar.HOUR_OF_DAY) - entryHour
                if (diff in 0..5) hourBuckets[5 - diff]++
            }

            if (isExitToday) {
                total++
                exits++
            }

            if (session.exitTime == null && session.status == "ACTIVE") {
                active++
            }
        }

        _totalScansToday.value = total
        _activeNow.value = active
        _entriesToday.value = entries
        _exitsToday.value = exits
        _last6hChart.value = hourBuckets
        _recentScans.value = sessions.sortedByDescending { it.entryTime?.toDate()?.time ?: 0L }.take(5)
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
