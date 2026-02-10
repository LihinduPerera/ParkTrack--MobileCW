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

    private val _overdueCharges = MutableStateFlow<List<ParkingCharge>>(emptyList())
    val overdueCharges: StateFlow<List<ParkingCharge>> = _overdueCharges.asStateFlow()

    private val _paidCharges = MutableStateFlow<List<ParkingCharge>>(emptyList())
    val paidCharges: StateFlow<List<ParkingCharge>> = _paidCharges.asStateFlow()

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
                val charges = result.getOrNull() ?: emptyList()
                
                // Separate unpaid charges by status
                _unpaidCharges.value = charges.filter { !it.isOverdue }
                _overdueCharges.value = charges.filter { it.isOverdue }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load unpaid charges"
                _isLoading.value = false
            }
        }
    }

    fun loadPaidCharges(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = billingRepository.getAllDriverCharges(driverId)
                val charges = result.getOrNull() ?: emptyList()
                _paidCharges.value = charges.filter { it.isPaid }
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to load paid charges"
                _isLoading.value = false
            }
        }
    }

    fun refreshAllPaymentStatuses(driverId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load all charges
                val allChargesResult = billingRepository.getAllDriverCharges(driverId)
                val allCharges = allChargesResult.getOrNull() ?: emptyList()
                
                // Check and update overdue status for each unpaid charge
                allCharges.filter { !it.isPaid && it.finalCharge > 0 }.forEach { charge ->
                    if (charge.shouldBeOverdue() && !charge.isOverdue) {
                        // Mark as overdue in repository
                        val daysSince = calculateDaysSinceSession(charge)
                        billingRepository.markChargeAsOverdue(charge.id, daysSince)
                    }
                }
                
                // Reload charges to get updated overdue status
                val updatedChargesResult = billingRepository.getAllDriverCharges(driverId)
                val updatedCharges = updatedChargesResult.getOrNull() ?: allCharges
                
                // Categorize by payment status
                _paidCharges.value = updatedCharges.filter { it.isPaid }
                _unpaidCharges.value = updatedCharges.filter { !it.isPaid && !it.isOverdue && it.finalCharge > 0 }
                _overdueCharges.value = updatedCharges.filter { it.isOverdue && !it.isPaid }
                
                _isLoading.value = false
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to refresh payment statuses"
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Calculate days since parking session ended
     */
    private fun calculateDaysSinceSession(charge: ParkingCharge): Int {
        val now = System.currentTimeMillis()
        val sessionEndTime = charge.exitTime?.toDate()?.time ?: charge.entryTime?.toDate()?.time ?: now
        return ((now - sessionEndTime) / (24 * 60 * 60 * 1000)).toInt()
    }
    
    /**
     * FIXED: Observe all charges for a driver in real-time
     * Properly categorizes charges and detects overdue status
     */
    fun observeAllCharges(driverId: String) {
        viewModelScope.launch {
            try {
                billingRepository.observeDriverCharges(driverId).collectLatest { charges ->
                    // Check for overdue charges
                    val now = System.currentTimeMillis()
                    charges.filter { !it.isPaid && it.finalCharge > 0 && !it.isOverdue }.forEach { charge ->
                        val sessionEndTime = charge.exitTime?.toDate()?.time ?: charge.entryTime?.toDate()?.time ?: now
                        val daysSince = ((now - sessionEndTime) / (24 * 60 * 60 * 1000)).toInt()
                        
                        if (daysSince >= 7) {
                            // Mark as overdue
                            viewModelScope.launch {
                                billingRepository.markChargeAsOverdue(charge.id, daysSince)
                            }
                        }
                    }
                    
                    // Categorize by payment status in real-time
                    _paidCharges.value = charges.filter { it.isPaid }
                    _unpaidCharges.value = charges.filter { !it.isPaid && !it.isOverdue && it.finalCharge > 0 }
                    _overdueCharges.value = charges.filter { it.isOverdue && !it.isPaid }
                    
                    println("Billing data updated - Paid: ${_paidCharges.value.size}, Unpaid: ${_unpaidCharges.value.size}, Overdue: ${_overdueCharges.value.size}")
                }
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to observe charges"
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
