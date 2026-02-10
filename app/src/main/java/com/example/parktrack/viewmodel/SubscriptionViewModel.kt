package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.repository.AuthRepository
import com.example.parktrack.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _currentTier = MutableStateFlow(SubscriptionTier.NORMAL)
    val currentTier: StateFlow<SubscriptionTier> = _currentTier.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadCurrentSubscription() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    val user = userRepository.getUserById(currentUser.uid)
                    if (user != null) {
                        _currentTier.value = user.subscriptionTier
                    }
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load subscription: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upgradeSubscription(newTier: SubscriptionTier) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    // Update user subscription tier
                    userRepository.updateSubscriptionTier(currentUser.uid, newTier)
                    _currentTier.value = newTier
                } else {
                    _errorMessage.value = "User not authenticated"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to upgrade subscription: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}