package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.QRCodeData
import com.example.parktrack.data.model.User
import com.example.parktrack.data.repository.ParkingSessionRepository
import com.example.parktrack.utils.QRCodeValidator
import com.example.parktrack.utils.ValidationResult
import com.google.firebase.auth.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

enum class ScanState {
    IDLE,
    SCANNING,
    PROCESSING,
    SUCCESS,
    ERROR
}

@HiltViewModel
class AdminScannerViewModel @Inject constructor(
    private val parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {
    
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    
    // StateFlows
    private val _scanState = MutableStateFlow<ScanState>(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _scannedQRData = MutableStateFlow<QRCodeData?>(null)
    val scannedQRData: StateFlow<QRCodeData?> = _scannedQRData.asStateFlow()
    
    private val _selectedGate = MutableStateFlow("Main Gate")
    val selectedGate: StateFlow<String> = _selectedGate.asStateFlow()
    
    private val _scanResultMessage = MutableStateFlow("")
    val scanResultMessage: StateFlow<String> = _scanResultMessage.asStateFlow()
    
    private val _recentScans = MutableStateFlow<List<ParkingSession>>(emptyList())
    val recentScans: StateFlow<List<ParkingSession>> = _recentScans.asStateFlow()
    
    private val _currentAdmin = MutableStateFlow<User?>(null)
    val currentAdmin: StateFlow<User?> = _currentAdmin.asStateFlow()
    
    private val _sessionType = MutableStateFlow<String>("") // "ENTRY" or "EXIT"
    val sessionType: StateFlow<String> = _sessionType.asStateFlow()
    
    private val _scannedDriver = MutableStateFlow<User?>(null)
    val scannedDriver: StateFlow<User?> = _scannedDriver.asStateFlow()
    
    init {
        fetchCurrentAdmin()
        fetchRecentScans()
    }
    
    fun setGate(gate: String) {
        _selectedGate.value = gate
    }
    
    /**
     * Fetch current admin user data
     */
    private fun fetchCurrentAdmin() {
        viewModelScope.launch {
            val adminId = auth.currentUser?.uid ?: return@launch
            
            try {
                firestore.collection("users").document(adminId).get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)?.copy(id = adminId)
                        _currentAdmin.value = user
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Process scanned QR code string
     */
    fun processScannedQR(qrString: String) {
        viewModelScope.launch {
            _scanState.value = ScanState.PROCESSING
            
            try {
                // Parse QR code
                val qrData = QRCodeData.fromQRString(qrString)
                if (qrData == null) {
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Invalid QR code format"
                    return@launch
                }
                
                // Validate QR code
                val validation = QRCodeValidator.validateQRCode(qrData)
                when (validation) {
                    is ValidationResult.Expired -> {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "QR code expired. Ask driver to generate new one."
                        return@launch
                    }
                    is ValidationResult.InvalidHash -> {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "Invalid QR code. Security check failed."
                        return@launch
                    }
                    is ValidationResult.InvalidFormat -> {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "Invalid QR format"
                        return@launch
                    }
                    else -> {} // ValidationResult.Valid - continue
                }
                
                // Get driver info
                val driverSnapshot = firestore.collection("users")
                    .document(qrData.userId)
                    .get()
                    .await()
                
                val driver = driverSnapshot.toObject(User::class.java)
                    ?.copy(id = qrData.userId)
                
                if (driver == null) {
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Driver not found"
                    return@launch
                }
                
                _scannedDriver.value = driver
                _scannedQRData.value = qrData
                
                // Check if driver has active session
                val activeSession = parkingSessionRepository
                    .getActiveSessionForDriver(qrData.userId)
                    .getOrNull()
                
                if (activeSession == null) {
                    // Entry scan - create new session
                    _sessionType.value = "ENTRY"
                    createEntrySession(qrData, driver)
                } else {
                    // Exit scan - complete session
                    _sessionType.value = "EXIT"
                    completeExitSession(qrData, driver, activeSession)
                }
                
            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Error processing QR: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Create entry parking session
     */
    private suspend fun createEntrySession(qrData: QRCodeData, driver: User) {
        try {
            val admin = _currentAdmin.value
            
            val session = ParkingSession(
                id = UUID.randomUUID().toString(),
                driverId = qrData.userId,
                driverName = driver.fullName,
                vehicleNumber = qrData.vehicleNumber,
                entryTime = Timestamp.now(),
                gateLocation = _selectedGate.value,
                scannedByAdminId = admin?.id ?: "",
                adminName = admin?.fullName ?: "Unknown",
                status = "ACTIVE",
                qrCodeUsed = qrData.toQRString()
            )
            
            val result = parkingSessionRepository.createSession(session)
            
            if (result.isSuccess) {
                _scanState.value = ScanState.SUCCESS
                _scanResultMessage.value = "Entry recorded for ${driver.fullName}"
                fetchRecentScans()
            } else {
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Failed to record entry"
            }
        } catch (e: Exception) {
            _scanState.value = ScanState.ERROR
            _scanResultMessage.value = "Error: ${e.message}"
            e.printStackTrace()
        }
    }
    
    /**
     * Complete exit parking session
     */
    private suspend fun completeExitSession(
        qrData: QRCodeData,
        driver: User,
        activeSession: ParkingSession
    ) {
        try {
            val exitTime = Timestamp.now()
            val result = parkingSessionRepository.completeSession(activeSession.id, exitTime)
            
            if (result.isSuccess) {
                _scanState.value = ScanState.SUCCESS
                _scanResultMessage.value = "Exit recorded for ${driver.fullName}"
                fetchRecentScans()
            } else {
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Failed to record exit"
            }
        } catch (e: Exception) {
            _scanState.value = ScanState.ERROR
            _scanResultMessage.value = "Error: ${e.message}"
            e.printStackTrace()
        }
    }
    
    /**
     * Fetch recent scans by current admin
     */
    fun fetchRecentScans() {
        viewModelScope.launch {
            try {
                val adminId = auth.currentUser?.uid ?: return@launch
                
                val sessions = firestore.collection("parkingSessions")
                    .whereEqualTo("scannedByAdminId", adminId)
                    .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                    .toObjects(ParkingSession::class.java)
                
                _recentScans.value = sessions
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Reset scan state
     */
    fun resetScanState() {
        _scanState.value = ScanState.IDLE
        _scanResultMessage.value = ""
        _scannedQRData.value = null
        _scannedDriver.value = null
        _sessionType.value = ""
    }
}
