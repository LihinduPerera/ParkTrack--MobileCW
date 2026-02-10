package com.example.parktrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.ui.components.ErrorDialog
import com.example.parktrack.ui.components.UserTierCard
import com.example.parktrack.viewmodel.BillingViewModel
import com.example.parktrack.viewmodel.UserTierViewModel
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onBackClick: () -> Unit,
    onViewPricing: () -> Unit = {},
    viewModel: BillingViewModel = hiltViewModel(),
    userTierViewModel: UserTierViewModel = hiltViewModel()
) {
    val invoices by viewModel.invoices.collectAsStateWithLifecycle()
    val currentInvoice by viewModel.currentInvoice.collectAsStateWithLifecycle()
    val unpaidCharges by viewModel.unpaidCharges.collectAsStateWithLifecycle()
    val overdueInvoices by viewModel.overdueSessions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    
    val currentUser by userTierViewModel.currentUser.collectAsStateWithLifecycle()
    val isUserLoading by userTierViewModel.isLoading.collectAsStateWithLifecycle()
    val userErrorMessage by userTierViewModel.errorMessage.collectAsStateWithLifecycle()

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadDriverInvoices(userId)
            viewModel.loadCurrentMonthInvoice(userId)
            viewModel.loadUnpaidCharges(userId)
            viewModel.loadOverdueInvoices(userId)
            userTierViewModel.loadCurrentUser()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing & Invoices") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // User Tier Card
            if (!isUserLoading && currentUser != null) {
                UserTierCard(
                    tier = currentUser!!.subscriptionTier,
                    modifier = Modifier.padding(16.dp),
                    onViewPricing = onViewPricing
                )
            }

            // Current Invoice Card
            if (!isLoading && currentInvoice != null) {
                CurrentInvoiceCard(currentInvoice!!)
            }

            // Tab bar
            TabRow(selectedTabIndex = selectedTabIndex, modifier = Modifier.fillMaxWidth()) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("All Invoices") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Unpaid (${unpaidCharges.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Overdue (${overdueInvoices.size})") }
                )
            }

            // Content
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTabIndex) {
                    0 -> AllInvoicesTab(invoices)
                    1 -> UnpaidChargesTab(unpaidCharges)
                    2 -> OverdueInvoicesTab(overdueInvoices)
                }
            }
        }
    }

    if (errorMessage != null) {
        ErrorDialog(
            title = "Error",
            message = errorMessage ?: "Unknown error",
            onDismiss = { viewModel.clearError() }
        )
    }
}

@Composable
private fun CurrentInvoiceCard(invoice: com.example.parktrack.data.model.Invoice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (invoice.paymentStatus == "PAID") Color(0xFF4CAF50) else Color(0xFFFF9800)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Current Month (${invoice.month})",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Rs. ${String.format("%.2f", invoice.netAmount)}",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column {
                    Text("Sessions", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text("${invoice.totalSessions}", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                }
                Column {
                    Text("Status", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text(
                        invoice.paymentStatus,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
                Column {
                    Text("Balance", style = MaterialTheme.typography.labelSmall, color = Color.White)
                    Text(
                        "Rs. ${String.format("%.2f", invoice.balanceDue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AllInvoicesTab(invoices: List<com.example.parktrack.data.model.Invoice>) {
    if (invoices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No invoices available")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(invoices) { invoice ->
                InvoiceItemCard(invoice)
            }
        }
    }
}

@Composable
private fun UnpaidChargesTab(charges: List<com.example.parktrack.data.model.ParkingCharge>) {
    if (charges.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("All charges are paid!")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(charges) { charge ->
                ChargeItemCard(charge)
            }
        }
    }
}

@Composable
private fun OverdueInvoicesTab(invoices: List<com.example.parktrack.data.model.Invoice>) {
    if (invoices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No overdue invoices!")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(invoices) { invoice ->
                OverdueInvoiceCard(invoice)
            }
        }
    }
}

@Composable
private fun InvoiceItemCard(invoice: com.example.parktrack.data.model.Invoice) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = invoice.month,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invoice.totalSessions} sessions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Rs. ${String.format("%.2f", invoice.netAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = invoice.paymentStatus,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (invoice.isPaid) Color.Green else Color.Red
                    )
                }
            }
        }
    }
}

@Composable
private fun ChargeItemCard(charge: com.example.parktrack.data.model.ParkingCharge) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = charge.parkingLotName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = charge.vehicleNumber,
                        style = MaterialTheme.typography.bodySmall
                    )
                    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
                    Text(
                        text = dateFormat.format(charge.entryTime?.toDate() ?: Date()),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Rs. ${String.format("%.2f", charge.finalCharge)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    val hours = (charge.durationMinutes / 60).toInt()
                    val mins = (charge.durationMinutes % 60).toInt()
                    Text(
                        text = "${hours}h ${mins}m",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun OverdueInvoiceCard(invoice: com.example.parktrack.data.model.Invoice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = invoice.month,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "OVERDUE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Rs. ${String.format("%.2f", invoice.balanceDue)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
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

