package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingLot
import com.example.parktrack.data.repository.ParkingLotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddParkingLotViewModel @Inject constructor(
    private val parkingLotRepository: ParkingLotRepository
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isSuccess = MutableStateFlow(false)
    val isSuccess: StateFlow<Boolean> = _isSuccess.asStateFlow()

    private val _parkingLots = MutableStateFlow<List<ParkingLot>>(emptyList())
    val parkingLots: StateFlow<List<ParkingLot>> = _parkingLots.asStateFlow()

    private val _selectedParkingLot = MutableStateFlow<ParkingLot?>(null)
    val selectedParkingLot: StateFlow<ParkingLot?> = _selectedParkingLot.asStateFlow()

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode.asStateFlow()

    init {
        loadExistingParkingLots()
    }

    private fun loadExistingParkingLots() {
        viewModelScope.launch {
            try {
                val result = parkingLotRepository.getAllParkingLots()
                _parkingLots.value = result.getOrNull() ?: emptyList()
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load existing parking lots: ${e.message}"
            }
        }
    }

    fun selectParkingLot(parkingLot: ParkingLot) {
        _selectedParkingLot.value = parkingLot
        _isEditMode.value = true
    }

    fun clearSelection() {
        _selectedParkingLot.value = null
        _isEditMode.value = false
    }

    fun addParkingLot(
        name: String,
        totalSpaces: Int,
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        openingTime: String,
        closingTime: String,
        twentyFourHours: Boolean,
        hasEVCharging: Boolean,
        hasDisabledParking: Boolean
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Parking lot name is required"
            return
        }

        if (totalSpaces <= 0) {
            _errorMessage.value = "Total spaces must be greater than 0"
            return
        }

        if (address.isBlank()) {
            _errorMessage.value = "Address is required"
            return
        }

        if (city.isBlank()) {
            _errorMessage.value = "City is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isSuccess.value = false

            try {
                val result = parkingLotRepository.addParkingLot(
                    com.example.parktrack.data.model.ParkingLot(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        location = city, // Use city as location for now
                        latitude = latitude,
                        longitude = longitude,
                        totalSpaces = totalSpaces,
                        availableSpaces = totalSpaces, // All spaces available initially
                        occupiedSpaces = 0,
                        address = address.trim(),
                        city = city.trim(),
                        state = state.trim(),
                        zipCode = zipCode.trim(),
                        openingTime = openingTime,
                        closingTime = closingTime,
                        twentyFourHours = twentyFourHours,
                        hasEVCharging = hasEVCharging,
                        hasDisabledParking = hasDisabledParking,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                )

                if (result.isSuccess) {
                    _isLoading.value = false
                    _isSuccess.value = true
                    loadExistingParkingLots() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to add parking lot: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to add parking lot: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun updateParkingLot(
        id: String,
        name: String,
        totalSpaces: Int,
        latitude: Double,
        longitude: Double,
        address: String,
        city: String,
        state: String,
        zipCode: String,
        openingTime: String,
        closingTime: String,
        twentyFourHours: Boolean,
        hasEVCharging: Boolean,
        hasDisabledParking: Boolean
    ) {
        if (name.isBlank()) {
            _errorMessage.value = "Parking lot name is required"
            return
        }

        if (totalSpaces <= 0) {
            _errorMessage.value = "Total spaces must be greater than 0"
            return
        }

        if (address.isBlank()) {
            _errorMessage.value = "Address is required"
            return
        }

        if (city.isBlank()) {
            _errorMessage.value = "City is required"
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _isSuccess.value = false

            try {
                val updates = mapOf(
                    "name" to name.trim(),
                    "location" to city.trim(),
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "totalSpaces" to totalSpaces,
                    "address" to address.trim(),
                    "city" to city.trim(),
                    "state" to state.trim(),
                    "zipCode" to zipCode.trim(),
                    "openingTime" to openingTime,
                    "closingTime" to closingTime,
                    "twentyFourHours" to twentyFourHours,
                    "hasEVCharging" to hasEVCharging,
                    "hasDisabledParking" to hasDisabledParking,
                    "updatedAt" to System.currentTimeMillis()
                )

                val result = parkingLotRepository.updateParkingLot(id, updates)

                if (result.isSuccess) {
                    _isLoading.value = false
                    _isSuccess.value = true
                    loadExistingParkingLots() // Refresh the list
                } else {
                    _errorMessage.value = "Failed to update parking lot: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to update parking lot: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun deleteParkingLot(parkingLot: ParkingLot) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = parkingLotRepository.deleteParkingLot(parkingLot.id)

                if (result.isSuccess) {
                    _isLoading.value = false
                    _isSuccess.value = true
                    loadExistingParkingLots() // Refresh the list
                    clearSelection() // Clear any selection
                } else {
                    _errorMessage.value = "Failed to delete parking lot: ${result.exceptionOrNull()?.message}"
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to delete parking lot: ${e.message}"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _isSuccess.value = false
    }
}