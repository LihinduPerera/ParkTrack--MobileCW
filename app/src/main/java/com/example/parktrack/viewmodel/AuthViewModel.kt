package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.UserRole
import com.example.parktrack.data.repository.AuthRepository
import com.example.parktrack.data.repository.ParkingSessionRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firebaseAuth: FirebaseAuth,
    private val parkingSessionRepository: ParkingSessionRepository
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

    private val _isUploadingProfileImage = MutableStateFlow(false)
    val isUploadingProfileImage: StateFlow<Boolean> = _isUploadingProfileImage.asStateFlow()

    init {
        checkCurrentUser()
    }

    private fun checkCurrentUser() {
        viewModelScope.launch {
            _isCheckingAuth.value = true
            try {
                if (authRepository.isLoggedIn()) {
                    authRepository.currentUser?.uid?.let { userId ->
                        val userData = authRepository.getUserData(userId)
                        if (userData.isSuccess) {
                            _currentUser.value = userData.getOrNull()
                            _authState.value = AuthState.Authenticated(userData.getOrNull()!!)
                        } else {
                            _authState.value = AuthState.Unauthenticated
                        }
                    } ?: run {
                        _authState.value = AuthState.Unauthenticated
                    }
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to check authentication")
            } finally {
                _isCheckingAuth.value = false
            }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.login(email, password)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                    _authState.value = AuthState.Authenticated(result.getOrNull()!!)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Login failed"
                    // Provide more user-friendly error messages
                    val userFriendlyError = when {
                        errorMsg.contains("invalid-email", ignoreCase = true) -> "Invalid email format"
                        errorMsg.contains("user-not-found", ignoreCase = true) -> "User not found"
                        errorMsg.contains("wrong-password", ignoreCase = true) -> "Incorrect password"
                        errorMsg.contains("network", ignoreCase = true) -> "Network error. Please check your connection"
                        else -> "Login failed: $errorMsg"
                    }
                    _authState.value = AuthState.Error(userFriendlyError)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = authRepository.register(email, password, fullName, phoneNumber, role)
                if (result.isSuccess) {
                    _currentUser.value = result.getOrNull()
                    _authState.value = AuthState.Authenticated(result.getOrNull()!!)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Registration failed"
                    // Provide more user-friendly error messages
                    val userFriendlyError = when {
                        errorMsg.contains("email-already-in-use", ignoreCase = true) -> "Email already registered"
                        errorMsg.contains("invalid-email", ignoreCase = true) -> "Invalid email format"
                        errorMsg.contains("weak-password", ignoreCase = true) -> "Password is too weak"
                        errorMsg.contains("network", ignoreCase = true) -> "Network error. Please check your connection"
                        else -> "Registration failed: $errorMsg"
                    }
                    _authState.value = AuthState.Error(userFriendlyError)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Registration failed")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                authRepository.logout()
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Logout failed: ${e.message}")
            }
        }
    }

    fun changePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        user?.updatePassword(newPassword)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Failed to update password")
                }
            }
    }

fun updateProfileImage(uri: android.net.Uri, onSuccess: () -> Unit, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            _isUploadingProfileImage.value = true
            try {
                val result = authRepository.updateProfileImage(_currentUser.value?.id ?: "", uri)
                if (result.isSuccess) {
                    // Refresh user data
                    _currentUser.value?.id?.let { userId ->
                        val userData = authRepository.getUserData(userId)
                        if (userData.isSuccess) {
                            _currentUser.value = userData.getOrNull()
                        }
                    }
                    onSuccess()
                } else {
                    onError(result.exceptionOrNull()?.message ?: "Failed to upload image")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to upload image")
            } finally {
                _isUploadingProfileImage.value = false
            }
        }
    }
    
    fun updateAssignedGate(gate: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id
                if (userId != null) {
                    val result = authRepository.updateAssignedGate(userId, gate)
                    if (result.isSuccess) {
                        // Refresh user data
                        val userData = authRepository.getUserData(userId)
                        if (userData.isSuccess) {
                            _currentUser.value = userData.getOrNull()
                        }
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun incrementScanCount() {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id
                if (userId != null) {
                    val result = authRepository.incrementScanCount(userId)
                    if (result.isSuccess) {
                        // Refresh user data
                        val userData = authRepository.getUserData(userId)
                        if (userData.isSuccess) {
                            _currentUser.value = userData.getOrNull()
                        }
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun updateEmail(newEmail: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        user?.updateEmail(newEmail)
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Also update the email in your Firestore 'users' collection here if necessary
                    onSuccess()
                } else {
                    onError(task.exception?.message ?: "Failed to update email")
                }
            }
    }

    fun deleteAccount(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                authRepository.deleteAccount(firebaseAuth = firebaseAuth)
                _currentUser.value = null
                _authState.value = AuthState.Unauthenticated
                onSuccess()
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Failed to delete account")
                onError(e.message ?: "Failed to delete account")
            }
        }
    }

    /**
     * Load driver statistics including total completed parking sessions
     */
    fun loadDriverStats() {
        viewModelScope.launch {
            val userId = _currentUser.value?.id
            if (userId != null) {
                try {
                    val result = parkingSessionRepository.getTotalCompletedSessionsCount(userId)
                    if (result.isSuccess) {
                        val totalParks = result.getOrDefault(0)
                        // Update the user with the calculated total parks
                        _currentUser.value = _currentUser.value?.copy(totalParks = totalParks)
                    }
                } catch (e: Exception) {
                    // Handle error silently - keep existing value
                }
            }
        }
    }

/**
     * Refresh current user data from Firestore
     */
    fun refreshCurrentUser() {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id
                if (userId != null) {
                    val userData = authRepository.getUserData(userId)
                    if (userData.isSuccess) {
                        _currentUser.value = userData.getOrNull()
                        // Reload driver stats after refreshing user (totalParks is calculated, not stored)
                        loadDriverStats()
                    }
                }
            } catch (e: Exception) {
                // Handle error silently
            }
        }
    }

    /**
     * Update user's full name
     */
    fun updateFullName(newFullName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val userId = _currentUser.value?.id
                if (userId != null) {
                    val result = authRepository.updateFullName(userId, newFullName)
                    if (result.isSuccess) {
                        // Refresh user data
                        val userData = authRepository.getUserData(userId)
                        if (userData.isSuccess) {
                            _currentUser.value = userData.getOrNull()
                        }
                        onSuccess()
                    } else {
                        onError(result.exceptionOrNull()?.message ?: "Failed to update full name")
                    }
                } else {
                    onError("User not found")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to update full name")
            }
        }
    }


}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}