package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.EnrichedParkingSession
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.Vehicle
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
class AdminQRHistoryViewModel @Inject constructor() : ViewModel() {

    private val firestore = Firebase.firestore
    private var historyListener: ListenerRegistration? = null

    private val _allScans = MutableStateFlow<List<EnrichedParkingSession>>(emptyList())
    val allScans: StateFlow<List<EnrichedParkingSession>> = _allScans.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _filterType = MutableStateFlow<ScanFilter>(ScanFilter.ALL)
    val filterType: StateFlow<ScanFilter> = _filterType.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadQRHistory()
    }

    private fun loadQRHistory() {
        _isLoading.value = true

        historyListener = firestore.collection("parkingSessions")
            .orderBy("entryTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _isLoading.value = false
                    return@addSnapshotListener
                }

                val sessions = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ParkingSession::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                viewModelScope.launch {
                    val enrichedSessions = sessions.map { session ->
                        enrichSession(session)
                    }
                    _allScans.value = enrichedSessions
                    _isLoading.value = false
                }
            }
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

    fun setFilter(filter: ScanFilter) {
        _filterType.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun getFilteredScans(): List<EnrichedParkingSession> {
        var filtered = _allScans.value

        // Apply status filter
        when (_filterType.value) {
            ScanFilter.ALL -> { /* no filter */ }
            ScanFilter.ACTIVE -> filtered = filtered.filter { it.status == "ACTIVE" }
            ScanFilter.COMPLETED -> filtered = filtered.filter { it.status == "COMPLETED" }
            ScanFilter.ENTRY_TODAY -> {
                val now = Calendar.getInstance()
                filtered = filtered.filter { session ->
                    session.entryTime?.toDate()?.let { entryDate ->
                        val cal = Calendar.getInstance().apply { time = entryDate }
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    } ?: false
                }
            }
            ScanFilter.EXIT_TODAY -> {
                val now = Calendar.getInstance()
                filtered = filtered.filter { session ->
                    session.exitTime?.toDate()?.let { exitDate ->
                        val cal = Calendar.getInstance().apply { time = exitDate }
                        cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                        cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
                    } ?: false
                }
            }
        }

        // Apply search filter
        val query = _searchQuery.value.trim().lowercase()
        if (query.isNotEmpty()) {
            filtered = filtered.filter { session ->
                session.driverName.lowercase().contains(query) ||
                session.driverPhoneNumber.contains(query) ||
                session.vehicleNumber.lowercase().contains(query) ||
                session.vehicleModel.lowercase().contains(query)
            }
        }

        return filtered
    }

    override fun onCleared() {
        super.onCleared()
        historyListener?.remove()
    }

    enum class ScanFilter {
        ALL,
        ACTIVE,
        COMPLETED,
        ENTRY_TODAY,
        EXIT_TODAY
    }
}
