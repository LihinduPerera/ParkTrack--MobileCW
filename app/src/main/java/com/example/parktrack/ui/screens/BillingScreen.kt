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
    initialTabIndex: Int = 0,
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
            viewModel.observeAllCharges(userId) // Real-time observation of charges
            viewModel.observeDriverInvoicesRealtime(userId) // Real-time observation of invoices
            viewModel.loadCurrentMonthInvoice(userId)
            viewModel.loadOverdueInvoices(userId)
            userTierViewModel.loadCurrentUser()
        }
    }

    var selectedTabIndex by remember { mutableIntStateOf(initialTabIndex) }

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
            // Compact User Tier Card
            if (!isUserLoading && currentUser != null) {
                UserTierCard(
                    tier = currentUser!!.subscriptionTier,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    onViewPricing = onViewPricing
                )
            }

            // Payment Summary Cards
            if (!isLoading) {
                PaymentSummaryCards(
                    paidCount = paidCharges.size,
                    unpaidCount = unpaidCharges.size,
                    overdueCount = overdueCharges.size
                )
            }

            // Compact Tab bar with 4 tabs
            TabRow(
                selectedTabIndex = selectedTabIndex, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("All", style = MaterialTheme.typography.labelSmall) }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("Paid (${paidCharges.size})", style = MaterialTheme.typography.labelSmall) }
                )
                Tab(
                    selected = selectedTabIndex == 2,
                    onClick = { selectedTabIndex = 2 },
                    text = { Text("Unpaid (${unpaidCharges.size})", style = MaterialTheme.typography.labelSmall) }
                )
                Tab(
                    selected = selectedTabIndex == 3,
                    onClick = { selectedTabIndex = 3 },
                    text = { Text("Overdue (${overdueCharges.size})", style = MaterialTheme.typography.labelSmall) }
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = invoice.month,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${invoice.totalSessions} sessions",
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs. ${String.format(Locale.getDefault(), "%.2f", invoice.netAmount)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = invoice.paymentStatus,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (invoice.isPaid) Color(0xFF2E7D32) else Color.Red
                )
            }
        }
    }
}

@Composable
private fun ChargeItemCard(charge: ParkingCharge) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = charge.parkingLotName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = charge.vehicleNumber,
                    style = MaterialTheme.typography.labelSmall
                )
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                Text(
                    text = dateFormat.format(charge.entryTime?.toDate() ?: Date()),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs. ${String.format(Locale.getDefault(), "%.2f", charge.finalCharge)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                val hours = (charge.durationMinutes / 60).toInt()
                val mins = (charge.durationMinutes % 60).toInt()
                Text(
                    text = "${hours}h ${mins}m",
                    style = MaterialTheme.typography.labelSmall
                )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = charge.parkingLotName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = charge.vehicleNumber,
                    style = MaterialTheme.typography.labelSmall
                )
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                Text(
                    text = dateFormat.format(charge.entryTime?.toDate() ?: Date()),
                    style = MaterialTheme.typography.labelSmall
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs. ${String.format(Locale.getDefault(), "%.2f", charge.finalCharge)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                val hours = (charge.durationMinutes / 60).toInt()
                val mins = (charge.durationMinutes % 60).toInt()
                Text(
                    text = "${hours}h ${mins}m",
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = "PAID",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.Bold
                )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = charge.parkingLotName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = charge.vehicleNumber,
                    style = MaterialTheme.typography.labelSmall
                )
                val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
                Text(
                    text = "${dateFormat.format(charge.entryTime?.toDate() ?: Date())} • ${charge.overdueDays}d overdue",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs. ${String.format(Locale.getDefault(), "%.2f", charge.finalCharge + charge.overdueCharge)}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                val hours = (charge.durationMinutes / 60).toInt()
                val mins = (charge.durationMinutes % 60).toInt()
                Text(
                    text = "${hours}h ${mins}m",
                    style = MaterialTheme.typography.labelSmall
                )
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = invoice.month,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${invoice.totalSessions} sessions • OVERDUE",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs. ${String.format(Locale.getDefault(), "%.2f", invoice.balanceDue)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Red,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PaymentSummaryCards(
    paidCount: Int,
    unpaidCount: Int,
    overdueCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Paid Card
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = paidCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Text(
                    text = "Paid",
                    style = MaterialTheme.typography.labelSmall,
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
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = unpaidCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE65100)
                )
                Text(
                    text = "Unpaid",
                    style = MaterialTheme.typography.labelSmall,
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
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = overdueCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.Red
                )
                Text(
                    text = "Overdue",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Red
                )
            }
        }
    }
}
