package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.UserRole
import com.example.parktrack.data.repository.AuthRepository
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
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isCheckingAuth = MutableStateFlow(true)
    val isCheckingAuth: StateFlow<Boolean> = _isCheckingAuth.asStateFlow()

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

    fun updateProfileImage(uri: android.net.Uri, onSuccess: () -> Unit) {
        viewModelScope.launch {

            onSuccess()
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


}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}