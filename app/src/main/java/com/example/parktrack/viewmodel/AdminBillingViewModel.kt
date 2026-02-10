package com.example.parktrack.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.parktrack.data.model.Invoice
import com.example.parktrack.data.model.ParkingCharge
import com.example.parktrack.data.model.PaymentConfirmation
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.model.TierUpgradeRecord
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.calculateTierUpgradeFee
import com.example.parktrack.data.repository.AuthRepository
import com.example.parktrack.data.repository.BillingRepository
import com.example.parktrack.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DriverBillingInfo(
    val user: User,
    val invoices: List<Invoice> = emptyList(),
    val unpaidCharges: List<ParkingCharge> = emptyList(),
    val paymentConfirmations: List<PaymentConfirmation> = emptyList(),
    val tierUpgradeRecords: List<TierUpgradeRecord> = emptyList()
)

@HiltViewModel
class AdminBillingViewModel @Inject constructor(
    private val billingRepository: BillingRepository,
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // Search results
    private val _searchResults = MutableStateFlow<List<User>>(emptyList())
    val searchResults: StateFlow<List<User>> = _searchResults.asStateFlow()

    // Selected driver info
    private val _selectedDriver = MutableStateFlow<DriverBillingInfo?>(null)
    val selectedDriver: StateFlow<DriverBillingInfo?> = _selectedDriver.asStateFlow()

    // Search query
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Loading states
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isProcessingPayment = MutableStateFlow(false)
    val isProcessingPayment: StateFlow<Boolean> = _isProcessingPayment.asStateFlow()

    private val _isUpdatingTier = MutableStateFlow(false)
    val isUpdatingTier: StateFlow<Boolean> = _isUpdatingTier.asStateFlow()

    // Success/Error messages
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Current admin info
    private val _currentAdmin = MutableStateFlow<User?>(null)
    val currentAdmin: StateFlow<User?> = _currentAdmin.asStateFlow()

    init {
        loadCurrentAdmin()
    }

    private fun loadCurrentAdmin() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.currentUser
                if (currentUser != null) {
                    val admin = userRepository.getUserById(currentUser.uid)
                    _currentAdmin.value = admin
                }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load admin info: ${e.message}"
            }
        }
    }

    /**
     * Search for drivers by email or name
     */
    fun searchDrivers(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = userRepository.searchDrivers(query)
                if (result.isSuccess) {
                    _searchResults.value = result.getOrDefault(emptyList())
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Search failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Search error: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Load complete billing information for a driver
     */
    fun loadDriverBillingInfo(driver: User) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                // Load all billing data in parallel
                val invoicesResult = billingRepository.getDriverInvoices(driver.id)
                val unpaidChargesResult = billingRepository.getUnpaidCharges(driver.id)
                val paymentConfirmationsResult = billingRepository.getDriverPaymentConfirmations(driver.id)
                val tierUpgradeRecordsResult = billingRepository.getDriverTierUpgradeRecords(driver.id)

                _selectedDriver.value = DriverBillingInfo(
                    user = driver,
                    invoices = invoicesResult.getOrDefault(emptyList()),
                    unpaidCharges = unpaidChargesResult.getOrDefault(emptyList()),
                    paymentConfirmations = paymentConfirmationsResult.getOrDefault(emptyList()),
                    tierUpgradeRecords = tierUpgradeRecordsResult.getOrDefault(emptyList())
                )
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load driver billing info: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Confirm payment for an invoice
     */
    fun confirmInvoicePayment(
        invoiceId: String,
        amountPaid: Double,
        paymentMethod: String = "CASH",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isProcessingPayment.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val admin = _currentAdmin.value
                if (admin == null) {
                    _errorMessage.value = "Admin not authenticated"
                    return@launch
                }

                val result = billingRepository.confirmInvoicePayment(
                    invoiceId = invoiceId,
                    amountPaid = amountPaid,
                    adminId = admin.id,
                    adminName = admin.name,
                    paymentMethod = paymentMethod,
                    notes = notes
                )

                if (result.isSuccess) {
                    _successMessage.value = "Payment confirmed successfully!"
                    // Refresh driver info
                    _selectedDriver.value?.let { loadDriverBillingInfo(it.user) }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Payment confirmation failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Payment error: ${e.message}"
            } finally {
                _isProcessingPayment.value = false
            }
        }
    }

    /**
     * Confirm payment for a specific charge
     */
    fun confirmChargePayment(
        chargeId: String,
        paymentMethod: String = "CASH",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isProcessingPayment.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val admin = _currentAdmin.value
                if (admin == null) {
                    _errorMessage.value = "Admin not authenticated"
                    return@launch
                }

                val result = billingRepository.confirmChargePayment(
                    chargeId = chargeId,
                    adminId = admin.id,
                    adminName = admin.name,
                    paymentMethod = paymentMethod,
                    notes = notes
                )

                if (result.isSuccess) {
                    _successMessage.value = "Charge payment confirmed successfully!"
                    // Refresh driver info
                    _selectedDriver.value?.let { loadDriverBillingInfo(it.user) }
                } else {
                    _errorMessage.value = result.exceptionOrNull()?.message ?: "Payment confirmation failed"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Payment error: ${e.message}"
            } finally {
                _isProcessingPayment.value = false
            }
        }
    }

    /**
     * Create tier upgrade request and confirm payment
     */
    fun upgradeDriverTier(
        driver: User,
        newTier: SubscriptionTier,
        paymentMethod: String = "CASH",
        notes: String = ""
    ) {
        viewModelScope.launch {
            _isUpdatingTier.value = true
            _errorMessage.value = null
            _successMessage.value = null

            try {
                val admin = _currentAdmin.value
                if (admin == null) {
                    _errorMessage.value = "Admin not authenticated"
                    return@launch
                }

                val currentTier = driver.subscriptionTier
                if (currentTier == newTier) {
                    _errorMessage.value = "Driver is already on ${newTier.name} tier"
                    return@launch
                }

                // Calculate upgrade fee
                val upgradeFee = calculateTierUpgradeFee(currentTier, newTier)

                // Step 1: Create tier upgrade record
                val upgradeResult = billingRepository.createTierUpgradeRecord(
                    driverId = driver.id,
                    driverEmail = driver.email,
                    driverName = driver.name,
                    fromTier = currentTier.name,
                    toTier = newTier.name,
                    upgradeFee = upgradeFee,
                    adminId = admin.id,
                    adminName = admin.name
                )

                if (upgradeResult.isFailure) {
                    _errorMessage.value = upgradeResult.exceptionOrNull()?.message ?: "Failed to create upgrade record"
                    return@launch
                }

                val upgradeRecord = upgradeResult.getOrThrow()

                // Step 2: Confirm payment for the upgrade
                val paymentResult = billingRepository.confirmTierUpgradePayment(
                    upgradeRecordId = upgradeRecord.id,
                    adminId = admin.id,
                    adminName = admin.name,
                    paymentMethod = paymentMethod,
                    notes = notes
                )

                if (paymentResult.isFailure) {
                    _errorMessage.value = paymentResult.exceptionOrNull()?.message ?: "Failed to confirm payment"
                    return@launch
                }

                // Step 3: Update user's tier
                val updateResult = userRepository.updateUserTierByAdmin(
                    userId = driver.id,
                    newTier = newTier,
                    adminId = admin.id,
                    adminName = admin.name
                )

                if (updateResult.isSuccess) {
                    _successMessage.value = "Driver upgraded to ${newTier.name} tier successfully!"
                    // Refresh driver info with updated tier
                    val updatedUser = driver.copy(subscriptionTier = newTier)
                    loadDriverBillingInfo(updatedUser)
                } else {
                    _errorMessage.value = updateResult.exceptionOrNull()?.message ?: "Failed to update tier"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Tier upgrade error: ${e.message}"
            } finally {
                _isUpdatingTier.value = false
            }
        }
    }

    /**
     * Clear selected driver
     */
    fun clearSelectedDriver() {
        _selectedDriver.value = null
    }

    /**
     * Clear messages
     */
    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    /**
     * Refresh current driver info
     */
    fun refreshDriverInfo() {
        _selectedDriver.value?.let { loadDriverBillingInfo(it.user) }
    }
}
