package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.User
import com.example.parktrack.data.repository.ParkingSessionRepository
import com.example.parktrack.utils.QRCodeGenerator
import com.google.firebase.auth.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Firebase
import com.google.firebase.firestore.firestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import android.graphics.Bitmap
import javax.inject.Inject
import java.util.concurrent.TimeUnit

@HiltViewModel
class DriverQRViewModel @Inject constructor(
    private val parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {
    
    private val firestore = Firebase.firestore
    private val auth = Firebase.auth
    
    // StateFlows
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    
    private val _activeSession = MutableStateFlow<ParkingSession?>(null)
    val activeSession: StateFlow<ParkingSession?> = _activeSession.asStateFlow()
    
    private val _showQRDialog = MutableStateFlow(false)
    val showQRDialog: StateFlow<Boolean> = _showQRDialog.asStateFlow()
    
    private val _qrCodeBitmap = MutableStateFlow<Bitmap?>(null)
    val qrCodeBitmap: StateFlow<Bitmap?> = _qrCodeBitmap.asStateFlow()
    
    private val _qrCountdown = MutableStateFlow(30)
    val qrCountdown: StateFlow<Int> = _qrCountdown.asStateFlow()
    
    private val _qrCodeData = MutableStateFlow("")
    val qrCodeData: StateFlow<String> = _qrCodeData.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _parkingDuration = MutableStateFlow("0h 0m")
    val parkingDuration: StateFlow<String> = _parkingDuration.asStateFlow()
    
    private val _hasActiveSession = MutableStateFlow(false)
    val hasActiveSession: StateFlow<Boolean> = _hasActiveSession.asStateFlow()
    
    private var qrRefreshJob: Job? = null
    private var sessionListenerJob: Job? = null
    private var durationJob: Job? = null
    
    init {
        fetchUserData()
        listenToActiveSession()
    }
    
    /**
     * Fetch current driver's user data from Firestore
     */
    fun fetchUserData() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            
            try {
                firestore.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        val user = document.toObject(User::class.java)?.copy(id = userId)
                        _currentUser.value = user
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Listen to active parking session in real-time
     */
    private fun listenToActiveSession() {
        sessionListenerJob?.cancel()
        
        val userId = auth.currentUser?.uid ?: return
        
        sessionListenerJob = viewModelScope.launch {
            try {
                parkingSessionRepository.observeActiveSession(userId)
                    .collectLatest { session ->
                        _activeSession.value = session
                        _hasActiveSession.value = session != null
                        
                        // Manage duration counter
                        if (session != null && session.entryTime != null) {
                            startDurationCounter(session.entryTime!!)
                        } else {
                            durationJob?.cancel()
                            _parkingDuration.value = "0h 0m"
                        }
                    }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Start duration counter (updates every minute)
     */
    private fun startDurationCounter(entryTime: com.google.firebase.Timestamp) {
        durationJob?.cancel()
        
        durationJob = viewModelScope.launch {
            while (true) {
                try {
                    val now = System.currentTimeMillis()
                    val entryMillis = entryTime.toDate().time
                    val durationMillis = now - entryMillis
                    
                    val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60
                    
                    _parkingDuration.value = if (hours > 0) {
                        "${hours}h ${minutes}m"
                    } else {
                        "${minutes}m"
                    }
                    
                    delay(60000) // Update every minute
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        }
    }
    
    /**
     * Generate QR code for parking entry/exit
     */
    fun generateQRCode() {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                fetchUserData()
                delay(500)
                return@launch
            }
            
            _isLoading.value = true
            
            try {
                // Create QR data with vehicle number
                val qrData = QRCodeGenerator.createQRCodeData(
                    userId = user.id,
                    vehicleNumber = user.vehicleNumber.ifEmpty { user.phoneNumber }
                )
                
                // Generate QR string
                val qrString = qrData.toQRString()
                _qrCodeData.value = qrString
                
                // Generate bitmap
                val bitmap = QRCodeGenerator.generateQRCode(qrString, size = 512)
                _qrCodeBitmap.value = bitmap
                
                // Show dialog
                _showQRDialog.value = true
                
                // Start countdown timer
                startQRRefreshTimer()
                
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Start QR code refresh timer (30 second countdown)
     */
    private fun startQRRefreshTimer() {
        qrRefreshJob?.cancel()
        
        qrRefreshJob = viewModelScope.launch {
            var countdown = 30
            
            while (countdown > 0 && _showQRDialog.value) {
                _qrCountdown.value = countdown
                delay(1000)
                countdown--
            }
            
            // Auto-refresh QR code when timer expires
            if (_showQRDialog.value) {
                generateQRCode()
            }
        }
    }
    
    /**
     * Stop QR refresh timer and close dialog
     */
    fun closeQRDialog() {
        qrRefreshJob?.cancel()
        _showQRDialog.value = false
        _qrCodeBitmap.value = null
        _qrCodeData.value = ""
        _qrCountdown.value = 30
    }
    
    override fun onCleared() {
        super.onCleared()
        qrRefreshJob?.cancel()
        sessionListenerJob?.cancel()
        durationJob?.cancel()
    }
}