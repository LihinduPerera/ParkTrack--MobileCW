package com.example.parktrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.Invoice
import com.example.parktrack.data.model.ParkingCharge
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
    val overdueCharges by viewModel.overdueCharges.collectAsStateWithLifecycle()
    val paidCharges by viewModel.paidCharges.collectAsStateWithLifecycle()
    val overdueInvoices by viewModel.overdueSessions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val currentUser by userTierViewModel.currentUser.collectAsStateWithLifecycle()
    val isUserLoading by userTierViewModel.isLoading.collectAsStateWithLifecycle()

    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.refreshAllPaymentStatuses(userId)
            viewModel.loadDriverInvoices(userId)
            viewModel.loadCurrentMonthInvoice(userId)
            viewModel.loadOverdueInvoices(userId)
            viewModel.observeAllCharges(userId) // Add real-time observation
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (userId.isNotEmpty()) {
                                viewModel.refreshAllPaymentStatuses(userId)
                                viewModel.loadDriverInvoices(userId)
                                viewModel.loadCurrentMonthInvoice(userId)
                                viewModel.loadOverdueInvoices(userId)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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

            // Payment Summary Cards
            if (!isLoading) {
                PaymentSummaryCards(
                    paidCount = paidCharges.size,
                    unpaidCount = unpaidCharges.size,
                    overdueCount = overdueCharges.size,
                    totalUnpaidAmount = unpaidCharges.sumOf { it.finalCharge } + overdueCharges.sumOf { it.finalCharge + it.overdueCharge }
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
                    text = { Text("Paid (${paidCharges.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Unpaid (${unpaidCharges.size})") }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Overdue (${overdueCharges.size + overdueInvoices.size})") }
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
                    1 -> PaidChargesTab(paidCharges)
                    2 -> UnpaidChargesTab(unpaidCharges)
                    3 -> OverdueTab(overdueCharges, overdueInvoices)
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
private fun CurrentInvoiceCard(invoice: Invoice) {
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
                text = "Rs. ${String.format(Locale.getDefault(), "%.2f", invoice.netAmount)}",
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
                        "Rs. ${String.format(Locale.getDefault(), "%.2f", invoice.balanceDue)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun AllInvoicesTab(invoices: List<Invoice>) {
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
private fun UnpaidChargesTab(charges: List<ParkingCharge>) {
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
private fun PaidChargesTab(charges: List<ParkingCharge>) {
    if (charges.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No paid charges yet")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(charges) { charge ->
                PaidChargeCard(charge)
            }
        }
    }
}

@Composable
private fun OverdueTab(
    overdueCharges: List<ParkingCharge>,
    overdueInvoices: List<Invoice>
) {
    if (overdueCharges.isEmpty() && overdueInvoices.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No overdue items!")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Show overdue charges first
            items(overdueCharges) { charge ->
                OverdueChargeCard(charge)
            }
            // Then show overdue invoices
            items(overdueInvoices) { invoice ->
                OverdueInvoiceCard(invoice)
            }
        }
    }
}

@Composable
private fun InvoiceItemCard(invoice: Invoice) {
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
                        text = "Rs. ${String.format(Locale.getDefault(), "%.2f", invoice.netAmount)}",
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
private fun ChargeItemCard(charge: ParkingCharge) {
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
                        text = "Rs. ${String.format(Locale.getDefault(), "%.2f", charge.finalCharge)}",
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
private fun PaidChargeCard(charge: ParkingCharge) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
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
                    Text(
                        text = "Paid: ${charge.paymentMethod}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF2E7D32)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Rs. ${String.format(Locale.getDefault(), "%.2f", charge.finalCharge)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    val hours = (charge.durationMinutes / 60).toInt()
                    val mins = (charge.durationMinutes % 60).toInt()
                    Text(
                        text = "${hours}h ${mins}m",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "✓ PAID",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OverdueChargeCard(charge: ParkingCharge) {
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
                    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                    Text(
                        text = dateFormat.format(charge.entryTime?.toDate() ?: Date()),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "OVERDUE - ${charge.overdueDays} days",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Rs. ${String.format(Locale.getDefault(), "%.2f", charge.finalCharge + charge.overdueCharge)}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red
                    )
                    if (charge.overdueCharge > 0) {
                        Text(
                            text = "+ Rs. ${String.format(Locale.getDefault(), "%.2f", charge.overdueCharge)} overdue",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red
                        )
                    }
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
private fun OverdueInvoiceCard(invoice: Invoice) {
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
                        text = "${invoice.totalSessions} sessions • INVOICE OVERDUE",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Rs. ${String.format(Locale.getDefault(), "%.2f", invoice.balanceDue)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Red,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentSummaryCards(
    paidCount: Int,
    unpaidCount: Int,
    overdueCount: Int,
    totalUnpaidAmount: Double
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Paid Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = paidCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "Paid",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        // Unpaid Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = unpaidCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "Unpaid",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFFE65100)
                )
            }
        }

        // Overdue Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = overdueCount.toString(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Text(
                    text = "Overdue",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Red
                )
            }
        }
    }

    // Total Unpaid Amount
    if (totalUnpaidAmount > 0) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Amount Due",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Rs. ${String.format(Locale.getDefault(), "%.2f", totalUnpaidAmount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
            }
        }
    }
}
