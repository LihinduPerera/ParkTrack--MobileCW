package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.QRCodeData
import com.example.parktrack.data.model.User
import com.example.parktrack.data.repository.AuthRepository
import com.example.parktrack.data.repository.ParkingSessionRepository
import com.example.parktrack.utils.QRCodeValidator
import com.example.parktrack.utils.ValidationResult
import com.example.parktrack.utils.ScanDebounceManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
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
    private val parkingSessionRepository: ParkingSessionRepository,
    private val authRepository: AuthRepository
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
    
    private val _activeSessions = MutableStateFlow<List<ParkingSession>>(emptyList())
    val activeSessions: StateFlow<List<ParkingSession>> = _activeSessions.asStateFlow()
    
    private val _currentAdmin = MutableStateFlow<User?>(null)
    val currentAdmin: StateFlow<User?> = _currentAdmin.asStateFlow()
    
    private val _sessionType = MutableStateFlow<String>("") // "ENTRY" or "EXIT"
    val sessionType: StateFlow<String> = _sessionType.asStateFlow()
    
    private val _scannedDriver = MutableStateFlow<User?>(null)
    val scannedDriver: StateFlow<User?> = _scannedDriver.asStateFlow()
    
    private val _scannedVehicleModel = MutableStateFlow("")
    val scannedVehicleModel: StateFlow<String> = _scannedVehicleModel.asStateFlow()
    
    private val _scannedVehicleColor = MutableStateFlow("")
    val scannedVehicleColor: StateFlow<String> = _scannedVehicleColor.asStateFlow()
    
    // Processing lock to prevent duplicate scans
    private var isProcessing = false
    private var lastProcessedQRString = ""
    private var lastProcessedTime = 0L
    private val SCAN_DEBOUNCE_MS = 3000L // 3 seconds minimum between same QR processing
    
    init {
        fetchCurrentAdmin()
        fetchRecentScans()
        listenToActiveSessions()
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
     * Process scanned QR code string with debouncing and duplicate prevention
     */
    fun processScannedQR(qrString: String) {
        // Check if already processing or duplicate scan
        if (isProcessing) {
            return // Ignore if already processing
        }
        
        val currentTime = System.currentTimeMillis()
        // Allow reprocessing same QR only after debounce period
        if (qrString == lastProcessedQRString && currentTime - lastProcessedTime < SCAN_DEBOUNCE_MS) {
            return // Ignore duplicate scan too soon
        }
        
        isProcessing = true
        lastProcessedQRString = qrString
        lastProcessedTime = currentTime
        
        viewModelScope.launch {
            try {
                _scanState.value = ScanState.PROCESSING
                
                // Parse QR code
                val qrData = QRCodeData.fromQRString(qrString)
                if (qrData == null) {
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Invalid QR code format"
                    isProcessing = false
                    return@launch
                }
                
                // Validate QR code
                val validation = QRCodeValidator.validateQRCode(qrData)
                when (validation) {
                    is ValidationResult.Expired -> {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "QR code expired. Ask driver to generate new one."
                        isProcessing = false
                        return@launch
                    }
                    is ValidationResult.InvalidHash -> {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "Invalid QR code. Security check failed."
                        isProcessing = false
                        return@launch
                    }
                    is ValidationResult.InvalidFormat -> {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "Invalid QR format"
                        isProcessing = false
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
                    isProcessing = false
                    return@launch
                }
                
                 _scannedDriver.value = driver
                 _scannedQRData.value = qrData
                 _scannedVehicleModel.value = qrData.vehicleModel
                 _scannedVehicleColor.value = qrData.vehicleColor
                
                // Check if driver has active session
                val activeSession = parkingSessionRepository
                    .getActiveSessionForDriver(qrData.userId)
                    .getOrNull()
                
                val currentSessionStatus = activeSession?.status
                
                // Validate QR type matches the operation
                if (activeSession == null && qrData.qrType == "EXIT") {
                    // Trying to use EXIT QR without an active session
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Use ENTRY QR code first. No active session found."
                    isProcessing = false
                    return@launch
                }
                
                if (activeSession != null && qrData.qrType == "ENTRY") {
                    // Trying to use ENTRY QR when session already exists
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Use EXIT QR code. Driver already has an active session."
                    isProcessing = false
                    return@launch
                }
                
                // Use ScanDebounceManager to prevent duplicate scans
                if (!ScanDebounceManager.shouldProcessScan(qrData.userId, currentSessionStatus)) {
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Please wait before scanning again"
                    isProcessing = false
                    return@launch
                }
                
                if (activeSession == null) {
                    // Entry scan - create new session
                    _sessionType.value = "ENTRY"
                    try {
                        createEntrySession(qrData, driver)
                    } catch (e: Exception) {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "Error creating entry: ${e.message}"
                        e.printStackTrace()
                    }
                } else {
                    // Exit scan - complete session
                    _sessionType.value = "EXIT"
                    try {
                        completeExitSession(qrData, driver, activeSession)
                    } catch (e: Exception) {
                        _scanState.value = ScanState.ERROR
                        _scanResultMessage.value = "Error recording exit: ${e.message}"
                        e.printStackTrace()
                    }
                }
                
            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Error processing QR: ${e.message}"
                e.printStackTrace()
            } finally {
                isProcessing = false
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
                // Fetch updated scans asynchronously (don't wait for it)
                fetchRecentScans()
                // Increment admin's scan count
                incrementAdminScanCount()
            } else {
                val error = result.exceptionOrNull()
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Failed to record entry: ${error?.message ?: "Unknown error"}"
            }
        } catch (e: Exception) {
            _scanState.value = ScanState.ERROR
            _scanResultMessage.value = "Error creating session: ${e.message}"
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
                val duration = if (activeSession.entryTime != null) {
                    parkingSessionRepository.calculateDuration(
                        activeSession.entryTime,
                        exitTime
                    )
                } else {
                    0L
                }

                val hours = duration / 60
                val minutes = duration % 60
                val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"

                _scanState.value = ScanState.SUCCESS
                _scanResultMessage.value = "Exit recorded. Duration: $durationStr"

                // Update recent scans
                fetchRecentScans()
                // Increment admin's scan count
                incrementAdminScanCount()
            } else {
                val error = result.exceptionOrNull()
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Failed to record exit: ${error?.message ?: "Unknown error"}"
            }
        } catch (e: Exception) {
            _scanState.value = ScanState.ERROR
            _scanResultMessage.value = "Error recording exit: ${e.message}"
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
                
                // Fetch all sessions by this admin, sorted by entry time
                val sessions = firestore.collection("parkingSessions")
                    .whereEqualTo("scannedByAdminId", adminId)
                    .orderBy("entryTime", com.google.firebase.firestore.Query.Direction.DESCENDING)
                    .limit(10)
                    .get()
                    .await()
                    .mapNotNull { document ->
                        document.toObject(ParkingSession::class.java)?.copy(id = document.id)
                    }
                
                _recentScans.value = sessions
            } catch (e: Exception) {
                e.printStackTrace()
                // If orderBy fails (no index), try without ordering
                try {
                    val adminId = auth.currentUser?.uid ?: return@launch
                    val sessions = firestore.collection("parkingSessions")
                        .whereEqualTo("scannedByAdminId", adminId)
                        .limit(10)
                        .get()
                        .await()
                        .mapNotNull { document ->
                            document.toObject(ParkingSession::class.java)?.copy(id = document.id)
                        }
                        .sortedByDescending { it.entryTime?.toDate()?.time ?: 0L }
                    
                    _recentScans.value = sessions
                } catch (e2: Exception) {
                    e2.printStackTrace()
                }
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
        _scannedVehicleModel.value = ""
        _scannedVehicleColor.value = ""
        _sessionType.value = ""
        isProcessing = false
    }
    
override fun onCleared() {
        super.onCleared()
        // Clear debounce records when ViewModel is destroyed
        ScanDebounceManager.clearAll()
    }

    /**
     * Increment the admin's scan count (totalScans and scansToday)
     */
    private fun incrementAdminScanCount() {
        viewModelScope.launch {
            try {
                val adminId = auth.currentUser?.uid
                if (adminId != null) {
                    authRepository.incrementScanCount(adminId)
                }
            } catch (e: Exception) {
                // Silent fail - don't block scan flow for stats update
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Listen to all active parking sessions
     */
    private fun listenToActiveSessions() {
        viewModelScope.launch {
            try {
                parkingSessionRepository.observeAllActiveSessions()
                    .collect { sessions ->
                        _activeSessions.value = sessions
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Manual exit for a session (admin can mark exit without QR)
     */
    fun manualExit(sessionId: String) {
        viewModelScope.launch {
            try {
                val session = parkingSessionRepository.getSessionById(sessionId).getOrNull()
                if (session == null) {
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Session not found"
                    return@launch
                }
                
                val exitTime = Timestamp.now()
                val result = parkingSessionRepository.completeSession(sessionId, exitTime)
                
                if (result.isSuccess) {
                    val duration = if (session.entryTime != null) {
                        parkingSessionRepository.calculateDuration(
                            session.entryTime,
                            exitTime
                        )
                    } else {
                        0L
                    }
                    
                    val hours = duration / 60
                    val minutes = duration % 60
                    val durationStr = if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
                    
                    _scanState.value = ScanState.SUCCESS
                    _scanResultMessage.value = "Manual exit recorded. Duration: $durationStr"
                    
                    _scannedDriver.value = com.example.parktrack.data.model.User(
                        id = session.driverId,
                        fullName = session.driverName,
                        vehicleNumber = session.vehicleNumber
                    )
                    _sessionType.value = "EXIT"
                    
                    fetchRecentScans()
                    listenToActiveSessions()
                } else {
                    val error = result.exceptionOrNull()
                    _scanState.value = ScanState.ERROR
                    _scanResultMessage.value = "Failed to record manual exit: ${error?.message ?: "Unknown error"}"
                }
            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Error: ${e.message}"
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Record manual exit for a parking session
     */
    fun recordManualExit(sessionId: String) {
        viewModelScope.launch {
            try {
                parkingSessionRepository.completeSession(sessionId, Timestamp.now())
                _scanState.value = ScanState.SUCCESS
                _scanResultMessage.value = "Manual exit recorded successfully"
                // Refresh active sessions
                listenToActiveSessions()
            } catch (e: Exception) {
                _scanState.value = ScanState.ERROR
                _scanResultMessage.value = "Failed to record manual exit: ${e.message}"
                e.printStackTrace()
            }
        }
    }
}