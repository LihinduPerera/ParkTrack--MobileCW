package com.example.parktrack.viewmodel


import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BillingUiState(
    val tier: String = "Gold", // Gold or Platinum
    val currentBalance: Double = 0.0,
    val history: List<Invoice> = emptyList()
)

data class Invoice(val month: String, val amount: Double, val status: String)

class BillingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BillingUiState(
        tier = "Gold",
        currentBalance = 45.50,
        history = listOf(
            Invoice("January 2024", 120.0, "Paid"),
            Invoice("December 2023", 95.0, "Paid")
        )
    ))
    val uiState = _uiState.asStateFlow()

    // Requirement: Gold: $5/hr, $40 cap | Platinum: $4/hr, $30 cap, $200 unlimited
    fun calculateSession(hours: Double, isPlatinum: Boolean): Double {
        val rate = if (isPlatinum) 4.0 else 5.0
        val cap = if (isPlatinum) 30.0 else 40.0
        return minOf(hours * rate, cap)
    }
}