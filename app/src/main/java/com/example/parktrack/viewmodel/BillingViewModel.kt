package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.Invoice
import com.example.parktrack.data.model.ParkingCharge
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.repository.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository
) : ViewModel() {

    private val _invoices = MutableStateFlow<List<Invoice>>(emptyList())
    val invoices: StateFlow<List<Invoice>> = _invoices.asStateFlow()

    private val _currentInvoice = MutableStateFlow<Invoice?>(null)
    val currentInvoice: StateFlow<Invoice?> = _currentInvoice.asStateFlow()

    private val _charges = MutableStateFlow<List<ParkingCharge>>(emptyList())
    val charges: StateFlow<List<ParkingCharge>> = _charges.asStateFlow()

    private val _unpaidCharges = MutableStateFlow<List<ParkingCharge>>(emptyList())
    val unpaidCharges: StateFlow<List<ParkingCharge>> = _unpaidCharges.asStateFlow()

    private val _overdueSessions = MutableStateFlow<List<Invoice>>(emptyList())
    val overdueSessions: StateFlow<List<Invoice>> = _overdueSessions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun loadDriverInvoices(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                billingRepository.observeDriverInvoices(driverId).collectLatest { invoices ->
                    _invoices.value = invoices
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load invoices"
                _isLoading.value = false
            }
        }
    }

    fun loadCurrentMonthInvoice(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val cal = Calendar.getInstance()
                val year = cal.get(Calendar.YEAR)
                val month = (cal.get(Calendar.MONTH) + 1).toString().padStart(2, '0')
                val yearMonth = "$year-$month"

                val result = billingRepository.getInvoiceForMonth(driverId, yearMonth)
                _currentInvoice.value = result.getOrNull()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load current invoice"
                _isLoading.value = false
            }
        }
    }

    fun loadUnpaidCharges(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingRepository.getUnpaidCharges(driverId)
                _unpaidCharges.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load unpaid charges"
                _isLoading.value = false
            }
        }
    }

    fun loadOverdueInvoices(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingRepository.getOverdueInvoices(driverId)
                _overdueSessions.value = result.getOrNull() ?: emptyList()
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load overdue invoices"
                _isLoading.value = false
            }
        }
    }

    fun payInvoice(invoiceId: String, amountPaid: Double) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingRepository.updateInvoicePayment(invoiceId, amountPaid, "PAID")
                if (result.isSuccess) {
                    _errorMessage.value = null
                } else {
                    _errorMessage.value = "Failed to process payment"
                }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Payment failed"
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
