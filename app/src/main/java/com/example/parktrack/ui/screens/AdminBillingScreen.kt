package com.example.parktrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.calculateTierUpgradeFee
import com.example.parktrack.ui.components.ErrorDialog
import com.example.parktrack.viewmodel.AdminBillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillingScreen(
    onBackClick: () -> Unit,
    viewModel: AdminBillingViewModel = hiltViewModel()
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.selectedDriver.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsStateWithLifecycle()
    val isUpdatingTier by viewModel.isUpdatingTier.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            kotlinx.coroutines.delay(3000)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Billing") },
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
            // Search Section
            SearchSection(
                query = searchQuery,
                onQueryChange = { viewModel.searchDrivers(it) },
                onSearch = { keyboardController?.hide() }
            )

            // Content
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                    
                    selectedDriver != null -> {
                        DriverBillingDetails(
                            driverBillingInfo = selectedDriver!!,
                            onConfirmChargePayment = viewModel::confirmChargePayment,
                            onConfirmInvoicePayment = viewModel::confirmInvoicePayment,
                            onUpgradeTier = viewModel::upgradeDriverTier,
                            onBack = { viewModel.clearSelectedDriver() },
                            isProcessingPayment = isProcessingPayment,
                            isUpdatingTier = isUpdatingTier
                        )
                    }
                    
                    searchResults.isNotEmpty() -> {
                        DriverSearchResults(
                            drivers = searchResults,
                            onDriverSelected = { driver ->
                                viewModel.loadDriverBillingInfo(driver)
                            }
                        )
                    }
                    
                    else -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "Search for drivers by name or email" else "No drivers found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.Gray
                            )
                        }
                    }
                }
            }
        }
    }

    // Success Message
    successMessage?.let { message ->
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = { viewModel.clearMessages() }) {
                    Text("DISMISS")
                }
            }
        ) {
            Text(message)
        }
    }

    // Error Dialog
    errorMessage?.let { error ->
        ErrorDialog(
            title = "Error",
            message = error,
            onDismiss = { viewModel.clearMessages() }
        )
    }
}

@Composable
private fun SearchSection(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            label = { Text("Search drivers...") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = "Search")
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true
        )
    }
}

@Composable
private fun DriverSearchResults(
    drivers: List<com.example.parktrack.data.model.User>,
    onDriverSelected: (com.example.parktrack.data.model.User) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(drivers) { driver ->
            DriverSearchResultCard(
                driver = driver,
                onClick = { onDriverSelected(driver) }
            )
        }
    }
}

@Composable
private fun DriverSearchResultCard(
    driver: com.example.parktrack.data.model.User,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = driver.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = driver.email,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Tier: ${driver.subscriptionTier.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Icon(
                Icons.Default.ArrowForward,
                contentDescription = "Select",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DriverBillingDetails(
    driverBillingInfo: com.example.parktrack.viewmodel.DriverBillingInfo,
    onConfirmChargePayment: (String, String, String) -> Unit,
    onConfirmInvoicePayment: (String, Double, String, String) -> Unit,
    onUpgradeTier: (com.example.parktrack.data.model.User, com.example.parktrack.data.model.SubscriptionTier, String, String) -> Unit,
    onBack: () -> Unit,
    isProcessingPayment: Boolean,
    isUpdatingTier: Boolean
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Driver Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = driverBillingInfo.user.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = driverBillingInfo.user.email,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Current Tier: ${driverBillingInfo.user.subscriptionTier.name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            }
        }

        // Billing Tabs
        var selectedTab by remember { mutableIntStateOf(0) }
        
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Charges") }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Invoices") }
            )
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 },
                text = { Text("History") }
            )
        }

        // Tab Content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                0 -> {
                    items(driverBillingInfo.unpaidCharges) { charge ->
                        UnpaidChargeCard(
                            charge = charge,
                            onConfirmPayment = { paymentMethod, notes ->
                                onConfirmChargePayment(charge.id, paymentMethod, notes)
                            },
                            isProcessing = isProcessingPayment
                        )
                    }
                    
                    item {
                        TierUpgradeCard(
                            currentTier = driverBillingInfo.user.subscriptionTier,
                            onUpgrade = { newTier, paymentMethod, notes ->
                                onUpgradeTier(driverBillingInfo.user, newTier, paymentMethod, notes)
                            },
                            isProcessing = isUpdatingTier
                        )
                    }
                }
                
                1 -> {
                    items(driverBillingInfo.invoices) { invoice ->
                        InvoiceCard(
                            invoice = invoice,
                            onConfirmPayment = { amount, paymentMethod, notes ->
                                onConfirmInvoicePayment(invoice.id, amount, paymentMethod, notes)
                            },
                            isProcessing = isProcessingPayment
                        )
                    }
                }
                
                2 -> {
                    items(driverBillingInfo.paymentConfirmations) { confirmation ->
                        PaymentConfirmationCard(confirmation = confirmation)
                    }
                }
            }
        }
    }
}

@Composable
private fun UnpaidChargeCard(
    charge: com.example.parktrack.data.model.ParkingCharge,
    onConfirmPayment: (String, String) -> Unit,
    isProcessing: Boolean
) {
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (charge.isOverdue) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = charge.vehicleNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = charge.parkingLotName,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (charge.isOverdue) {
                        Text(
                            text = "OVERDUE - ${charge.overdueDays} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rs. ${String.format("%.2f", charge.finalCharge + charge.overdueCharge)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (charge.isOverdue) Color.Red else MaterialTheme.colorScheme.primary
                    )
                    
                    Button(
                        onClick = { showPaymentDialog = true },
                        enabled = !isProcessing,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        if (isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Confirm Payment")
                        }
                    }
                }
            }
        }
    }
    
    if (showPaymentDialog) {
        PaymentConfirmationDialog(
            title = "Confirm Charge Payment",
            amount = charge.finalCharge + charge.overdueCharge,
            onConfirm = { method, notes ->
                onConfirmPayment(method, notes)
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false },
            isProcessing = isProcessing
        )
    }
}

@Composable
private fun InvoiceCard(
    invoice: com.example.parktrack.data.model.Invoice,
    onConfirmPayment: (Double, String, String) -> Unit,
    isProcessing: Boolean
) {
    var showPaymentDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = invoice.month,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invoice.totalSessions} sessions",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Status: ${invoice.paymentStatus}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (invoice.paymentStatus) {
                            "PAID" -> Color.Green
                            "OVERDUE" -> Color.Red
                            else -> Color(0xFFFF9800)
                        }
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Rs. ${String.format("%.2f", invoice.balanceDue)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.balanceDue > 0) MaterialTheme.colorScheme.primary else Color.Green
                    )
                    
                    if (invoice.balanceDue > 0) {
                        Button(
                            onClick = { showPaymentDialog = true },
                            enabled = !isProcessing,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Confirm Payment")
                            }
                        }
                    }
                }
            }
        }
    }
    
    if (showPaymentDialog) {
        PaymentConfirmationDialog(
            title = "Confirm Invoice Payment",
            amount = invoice.balanceDue,
            onConfirm = { method, notes ->
                onConfirmPayment(invoice.balanceDue, method, notes)
                showPaymentDialog = false
            },
            onDismiss = { showPaymentDialog = false },
            isProcessing = isProcessing
        )
    }
}

@Composable
private fun TierUpgradeCard(
    currentTier: com.example.parktrack.data.model.SubscriptionTier,
    onUpgrade: (com.example.parktrack.data.model.SubscriptionTier, String, String) -> Unit,
    isProcessing: Boolean
) {
    var showUpgradeDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Tier Upgrade",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Current Tier: ${currentTier.name}",
                style = MaterialTheme.typography.bodyMedium
            )
            
            Button(
                onClick = { showUpgradeDialog = true },
                enabled = !isProcessing,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Upgrade Tier")
                }
            }
        }
    }
    
    if (showUpgradeDialog) {
        TierUpgradeDialog(
            currentTier = currentTier,
            onConfirm = onUpgrade,
            onDismiss = { showUpgradeDialog = false },
            isProcessing = isProcessing
        )
    }
}

@Composable
private fun PaymentConfirmationCard(
    confirmation: com.example.parktrack.data.model.PaymentConfirmation
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE8F5E8)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Payment Confirmed",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF2E7D32)
            )
            Text(
                text = "Amount: Rs. ${String.format("%.2f", confirmation.amount)}",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Method: ${confirmation.paymentMethod}",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Confirmed by: ${confirmation.confirmedByAdminName}",
                style = MaterialTheme.typography.bodySmall
            )
            if (confirmation.notes.isNotEmpty()) {
                Text(
                    text = "Notes: ${confirmation.notes}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun PaymentConfirmationDialog(
    title: String,
    amount: Double,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean
) {
    var selectedMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    text = "Amount: Rs. ${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text("Payment Method:", style = MaterialTheme.typography.labelLarge)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CASH", "CARD", "ONLINE").forEach { method ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (selectedMethod == method) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedMethod == method,
                                onClick = { selectedMethod = method }
                            )
                            Text(
                                text = method,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selectedMethod, notes) },
                enabled = !isProcessing
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Confirm Payment")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun TierUpgradeDialog(
    currentTier: com.example.parktrack.data.model.SubscriptionTier,
    onConfirm: (com.example.parktrack.data.model.SubscriptionTier, String, String) -> Unit,
    onDismiss: () -> Unit,
    isProcessing: Boolean
) {
    var selectedTier by remember { mutableStateOf<com.example.parktrack.data.model.SubscriptionTier?>(null) }
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }
    
    val availableTiers = listOf(
        com.example.parktrack.data.model.SubscriptionTier.GOLD,
        com.example.parktrack.data.model.SubscriptionTier.PLATINUM
    ).filter { it != currentTier }
    
    val upgradeFee = selectedTier?.let { 
        calculateTierUpgradeFee(currentTier, it) 
    } ?: 0.0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade Tier") },
        text = {
            Column {
                Text(
                    text = "Current Tier: ${currentTier.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text("Select New Tier:", style = MaterialTheme.typography.labelLarge)
                
                availableTiers.forEach { tier ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedTier == tier,
                            onClick = { selectedTier = tier }
                        )
                        Text(
                            text = tier.name,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                
                if (upgradeFee > 0) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Upgrade Fee: Rs. ${String.format("%.2f", upgradeFee)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Payment Method:", style = MaterialTheme.typography.labelLarge)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("CASH", "CARD", "ONLINE").forEach { method ->
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    if (paymentMethod == method) 
                                        MaterialTheme.colorScheme.primaryContainer 
                                    else MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = paymentMethod == method,
                                onClick = { paymentMethod = method }
                            )
                            Text(
                                text = method,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    selectedTier?.let { tier ->
                        onConfirm(tier, paymentMethod, notes)
                    }
                },
                enabled = !isProcessing && selectedTier != null
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Upgrade")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}