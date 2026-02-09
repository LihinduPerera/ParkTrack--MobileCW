package com.example.parktrack.viewmodel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.EnrichedParkingSession
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.Vehicle
import com.example.parktrack.utils.FirebaseInitializationHelper
import com.google.firebase.Firebase
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    private val _recentScans = MutableStateFlow<List<EnrichedParkingSession>>(emptyList())
    val recentScans: StateFlow<List<EnrichedParkingSession>> = _recentScans.asStateFlow()

    private val _last6hChart = MutableStateFlow(List(6) { 0 })
    val last6hChart: StateFlow<List<Int>> = _last6hChart.asStateFlow()

    private val _isInitializingData = MutableStateFlow(false)
    val isInitializingData: StateFlow<Boolean> = _isInitializingData.asStateFlow()

    init { listenRealtime() }

    fun refresh() {
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                val sessions = firestore.collection("parkingSessions").get().await()
                    .documents.mapNotNull { d ->
                        d.toObject(ParkingSession::class.java)
                    }
                processSessions(sessions)
                _isRefreshing.value = false
            } catch (e: Exception) {
                _isRefreshing.value = false
            }
        }
    }

    private fun listenRealtime() {
        listener = firestore.collection("parkingSessions")
            .addSnapshotListener { snapshot, _ ->
                val sessions = snapshot?.documents?.mapNotNull {
                    it.toObject(ParkingSession::class.java)
                } ?: return@addSnapshotListener

                viewModelScope.launch {
                    processSessions(sessions)
                }
            }
    }

    private suspend fun processSessions(sessions: List<ParkingSession>) {
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
        val enrichedSessions = sessions.sortedByDescending { it.entryTime?.toDate()?.time ?: 0L }.take(5).map { session ->
            enrichSession(session)
        }
        _recentScans.value = enrichedSessions
    }

    private suspend fun enrichSession(session: ParkingSession): EnrichedParkingSession {
        val user = try {
            if (session.driverId.isNotEmpty()) {
                firestore.collection("users").document(session.driverId).get().await()
                    .toObject(User::class.java)
            } else null
        } catch (e: Exception) { null }

        val vehicle = try {
            if (session.vehicleNumber.isNotEmpty()) {
                firestore.collection("vehicles")
                    .whereEqualTo("vehicleNumber", session.vehicleNumber)
                    .get().await()
                    .documents.firstOrNull()
                    ?.toObject(Vehicle::class.java)
            } else null
        } catch (e: Exception) { null }

        return EnrichedParkingSession(
            id = session.id,
            driverId = session.driverId,
            driverName = session.driverName,
            driverPhoneNumber = user?.phoneNumber ?: "",
            vehicleNumber = session.vehicleNumber,
            vehicleModel = vehicle?.vehicleModel ?: "",
            entryTime = session.entryTime,
            exitTime = session.exitTime,
            gateLocation = session.gateLocation,
            scannedByAdminId = session.scannedByAdminId,
            adminName = session.adminName,
            status = session.status,
            qrCodeUsed = session.qrCodeUsed,
            durationMinutes = session.durationMinutes,
            createdAt = session.createdAt
        )
    }

    /**
     * Initialize sample data for development
     */
    fun initializeSampleData() {
        viewModelScope.launch {
            _isInitializingData.value = true
            try {
                FirebaseInitializationHelper.initializeSampleData(firestore)
                _isInitializingData.value = false
            } catch (e: Exception) {
                _isInitializingData.value = false
                // You might want to handle this error appropriately
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listener?.remove()
    }
}
