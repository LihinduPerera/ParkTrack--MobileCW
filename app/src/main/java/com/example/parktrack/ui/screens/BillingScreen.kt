package com.example.parktrack.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.getValue




class BillingViewModel {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState
}

@Composable
fun BillingScreen(viewModel: BillingViewModel) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn {
        item { Text("Billing & Invoices", style = MaterialTheme.typography.headlineMedium) }

        // Pricing Info Section
        item { PricingInfoCard() }

        // Monthly Invoice Summary
        item { InvoiceSummaryCard(state.currentInvoiceAmount) }

        // Last 6 Months History
        items(state.invoiceHistory) { invoice ->
            InvoiceHistoryItem(invoice)
        }
    }
}

@Composable
fun InvoiceHistoryItem(invoice: Invoice) {
    Text(
        text = "Invoice ${invoice.id} - ${invoice.amount} (${invoice.status})"
    )

}

@Composable
fun InvoiceSummaryCard(currentInvoiceAmount: Double) {
    Text("Current Invoice Amount: Rs. $currentInvoiceAmount")

}

@Composable
fun PricingInfoCard() {
    Text("Pricing Info (Gold / Platinum)")
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun PreviewBillingScreen() {
    val mockViewModel = BillingViewModel()

    MaterialTheme {
        BillingScreen(viewModel = mockViewModel)
    }
}



// Updated data classes in BillingScreen.kt
data class Invoice(
    val id: String,
    val amount: Double,
    val date: String,
    val status: String = "Paid"
)

data class BillingUiState(
    val membershipTier: String = "Gold", // or "Platinum"
    val currentInvoiceAmount: Double = 0.0,
    val invoiceHistory: List<Invoice> = emptyList()
)

