package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.model.User
import com.example.parktrack.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserTierViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadCurrentUser() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId != null) {
                    val user = authRepository.getUserData(userId)
                    if (user.isSuccess) {
                        _currentUser.value = user.getOrNull()
                    } else {
                        _errorMessage.value = "Failed to load user data: ${user.exceptionOrNull()?.message}"
                    }
                } else {
                    _errorMessage.value = "No user logged in"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load user information: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}