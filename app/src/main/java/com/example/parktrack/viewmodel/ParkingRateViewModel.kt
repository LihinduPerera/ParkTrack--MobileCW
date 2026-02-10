package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.RateType
import com.example.parktrack.data.repository.ParkingLotRepository
import com.example.parktrack.data.repository.ParkingRateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParkingRateViewModel @Inject constructor(
    private val parkingRateRepository: ParkingRateRepository,
    private val parkingLotRepository: ParkingLotRepository
) : ViewModel() {

    private val _parkingLots = MutableStateFlow<List<String>>(emptyList())
    val parkingLots: StateFlow<List<String>> = _parkingLots.asStateFlow()

    private val _currentRates = MutableStateFlow<ParkingRate?>(null)
    val currentRates: StateFlow<ParkingRate?> = _currentRates.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    fun loadParkingLots() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val lots = parkingLotRepository.getAllParkingLotsList()
                _parkingLots.value = lots.map { it.name }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load parking lots: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadRatesForLot(parkingLotId: String, rateType: RateType) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                val rate = parkingRateRepository.getRateByType(parkingLotId, rateType.name)
                if (rate.isSuccess) {
                    _currentRates.value = rate.getOrNull()
                } else {
                    _errorMessage.value = "Failed to load rates: ${rate.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load rates: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveRate(
        parkingLotId: String,
        rateType: RateType,
        rateConfig: ParkingRate
    ) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null
                _successMessage.value = null

                val rateToSave = rateConfig.copy(
                    parkingLotId = parkingLotId,
                    rateType = rateType,
                    updatedAt = System.currentTimeMillis()
                )

                val result = if (rateToSave.id.isEmpty()) {
                    // Create new rate
                    val newRate = rateToSave.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis()
                    )
                    parkingRateRepository.addRate(newRate)
                } else {
                    // Update existing rate
                    parkingRateRepository.updateRate(rateToSave.id, mapOf(
                        "basePricePerHour" to rateToSave.basePricePerHour,
                        "maxDailyPrice" to rateToSave.maxDailyPrice,
                        "normalRate" to rateToSave.normalRate,
                        "goldRate" to rateToSave.goldRate,
                        "platinumRate" to rateToSave.platinumRate,
                        "vipMultiplier" to rateToSave.vipMultiplier,
                        "overnightRate" to rateToSave.overnightRate,
                        "overnightStartHour" to rateToSave.overnightStartHour,
                        "overnightEndHour" to rateToSave.overnightEndHour,
                        "updatedAt" to rateToSave.updatedAt
                    ))
                }

                if (result.isSuccess) {
                    _successMessage.value = "Rate configuration saved successfully!"
                    // Reload the current rates
                    loadRatesForLot(parkingLotId, rateType)
                } else {
                    _errorMessage.value = "Failed to save rate: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to save rate: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    fun loadDefaultRates() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _errorMessage.value = null

                // Load default normal rates for display
                val rate = parkingRateRepository.getRateByType("default", RateType.NORMAL.name)
                if (rate.isSuccess && rate.getOrNull() != null) {
                    _currentRates.value = rate.getOrNull()
                } else {
                    // Create default rates if none exist
                    val defaultRate = ParkingRate(
                        id = "default_normal",
                        parkingLotId = "default",
                        rateType = RateType.NORMAL,
                        basePricePerHour = 10.0,
                        maxDailyPrice = 50.0,
                        normalRate = 10.0,
                        goldRate = 8.0,
                        platinumRate = 6.0,
                        vipMultiplier = 1.5,
                        overnightRate = 5.0,
                        overnightStartHour = 22,
                        overnightEndHour = 6,
                        isActive = true
                    )
                    _currentRates.value = defaultRate
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load default rates: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
}