package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.Vehicle
import com.example.parktrack.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class VehicleViewModel @Inject constructor(
    private val vehicleRepository: VehicleRepository
) : ViewModel() {

    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadVehicles(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = vehicleRepository.getDriverVehicles(driverId)
                _vehicles.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun addVehicle(
        driverId: String,
        vehicleNumber: String,
        vehicleModel: String,
        vehicleColor: String,
        vehicleType: String = "Car"
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val vehicle = Vehicle(
                    id = UUID.randomUUID().toString(),
                    ownerId = driverId,
                    vehicleNumber = vehicleNumber,
                    vehicleModel = vehicleModel,
                    vehicleColor = vehicleColor,
                    vehicleType = vehicleType
                )
                val result = vehicleRepository.addVehicle(vehicle)
                if (result.isSuccess) {
                    _errorMessage.value = null
                    loadVehicles(driverId)
                } else {
                    _errorMessage.value = "Failed to add vehicle"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun deleteVehicle(vehicleId: String, driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = vehicleRepository.deleteVehicle(vehicleId)
                if (result.isSuccess) {
                    _errorMessage.value = null
                    loadVehicles(driverId)
                } else {
                    _errorMessage.value = "Failed to delete vehicle"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun searchVehicles(query: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = vehicleRepository.searchVehicles(query)
                _vehicles.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
