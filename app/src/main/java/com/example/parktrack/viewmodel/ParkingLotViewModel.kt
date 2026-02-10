package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingLot
import com.example.parktrack.data.repository.ParkingLotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ParkingLotViewModel @Inject constructor(
    private val parkingLotRepository: ParkingLotRepository
) : ViewModel() {

    private val _parkingLots = MutableStateFlow<List<ParkingLot>>(emptyList())
    val parkingLots: StateFlow<List<ParkingLot>> = _parkingLots.asStateFlow()

    private val _selectedParkingLot = MutableStateFlow<ParkingLot?>(null)
    val selectedParkingLot: StateFlow<ParkingLot?> = _selectedParkingLot.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadAllParkingLots()
    }

    fun loadAllParkingLots() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = parkingLotRepository.getAllParkingLots()
                _parkingLots.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun loadParkingLotById(lotId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = parkingLotRepository.getParkingLotById(lotId)
                _selectedParkingLot.value = result.getOrNull()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun addParkingLot(
        name: String,
        location: String,
        latitude: Double,
        longitude: Double,
        totalSpaces: Int,
        address: String,
        city: String,
        state: String,
        zipCode: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val parkingLot = ParkingLot(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    location = location,
                    latitude = latitude,
                    longitude = longitude,
                    totalSpaces = totalSpaces,
                    availableSpaces = totalSpaces,
                    occupiedSpaces = 0,
                    address = address,
                    city = city,
                    state = state,
                    zipCode = zipCode
                )
                val result = parkingLotRepository.addParkingLot(parkingLot)
                if (result.isSuccess) {
                    _errorMessage.value = null
                    loadAllParkingLots()
                } else {
                    _errorMessage.value = "Failed to add parking lot"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun getNearbyParkingLots(latitude: Double, longitude: Double, radiusKm: Double = 5.0) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = parkingLotRepository.getNearbyParkingLots(latitude, longitude, radiusKm)
                _parkingLots.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun getParkingLotsByCity(city: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = parkingLotRepository.getParkingLotsByCity(city)
                _parkingLots.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun observeParkingLot(lotId: String): Flow<ParkingLot?> {
        return parkingLotRepository.observeParkingLot(lotId)
    }

    fun updateParkingLotSpaces(lotId: String, delta: Int) {
        viewModelScope.launch {
            try {
                parkingLotRepository.updateAvailableSpaces(lotId, delta)
                loadAllParkingLots()
            } catch (e: Exception) {
                _errorMessage.value = e.message
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
