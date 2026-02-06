package com.example.parktrack.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel


class BillingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(BillingUiState(
        membershipTier = "Gold",
        currentInvoiceAmount = 40.0,
        invoiceHistory = listOf(
            Invoice("1", 50.0, "2024-01-01", "Paid"),
            Invoice("2", 30.0, "2023-12-01", "Paid")
        )
    ))
    val uiState: StateFlow<BillingUiState> = _uiState
}

@Composable
fun BillingScreen(viewModel: BillingViewModel) {
    // 1. Get the state from the ViewModel
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {
        Text("My Invoices", style = MaterialTheme.typography.headlineMedium)

        // VaultPark Style Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)), // Gold Theme
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Current Month Due (${state.membershipTier})", color = Color.Black)
                Text("Rs. ${state.currentInvoiceAmount}", style = MaterialTheme.typography.displaySmall, color = Color.Black)
            }
        }

        Text("Invoice History (Last 6 Months)",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))

        // Actual LazyColumn
        LazyColumn {
            items(state.invoiceHistory) { invoice ->
                InvoiceHistoryItem(invoice)
            }
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

@SuppressLint("ViewModelConstructorInComposable")
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

