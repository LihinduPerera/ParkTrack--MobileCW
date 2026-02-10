package com.example.parktrack.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.Invoice
import com.example.parktrack.data.model.ParkingCharge
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.data.model.User
import com.example.parktrack.data.model.calculateTierUpgradeFee
import com.example.parktrack.data.model.getPaymentStatusColor
import com.example.parktrack.data.model.getPaymentStatusDisplay
import com.example.parktrack.viewmodel.AdminBillingViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBillingManagementScreen(
    onBackClick: () -> Unit,
    viewModel: AdminBillingViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val selectedDriver by viewModel.selectedDriver.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val isProcessingPayment by viewModel.isProcessingPayment.collectAsStateWithLifecycle()
    val isUpdatingTier by viewModel.isUpdatingTier.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    // Dialog states
    var showPaymentDialog by remember { mutableStateOf<PaymentDialogData?>(null) }
    var showTierUpgradeDialog by remember { mutableStateOf<TierUpgradeDialogData?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing & Payment Management") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectedDriver != null) {
                        IconButton(onClick = { viewModel.clearSelectedDriver() }) {
                            Icon(Icons.Default.Search, contentDescription = "Search New")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when {
                selectedDriver != null -> {
                    // Show driver billing details
                    DriverBillingDetails(
                        driverInfo = selectedDriver!!,
                        isProcessingPayment = isProcessingPayment,
                        isUpdatingTier = isUpdatingTier,
                        onConfirmInvoicePayment = { invoice ->
                            showPaymentDialog = PaymentDialogData.InvoicePayment(invoice)
                        },
                        onConfirmChargePayment = { charge ->
                            showPaymentDialog = PaymentDialogData.ChargePayment(charge)
                        },
                        onUpgradeTier = { driver, currentTier ->
                            showTierUpgradeDialog = TierUpgradeDialogData(driver, currentTier)
                        },
                        onRefresh = { viewModel.refreshDriverInfo() }
                    )
                }
                else -> {
                    // Show search interface
                    DriverSearchInterface(
                        searchQuery = searchQuery,
                        searchResults = searchResults,
                        isLoading = isLoading,
                        onSearchQueryChange = { viewModel.searchDrivers(it) },
                        onDriverSelected = { driver ->
                            viewModel.loadDriverBillingInfo(driver)
                        }
                    )
                }
            }
        }

        // Payment Confirmation Dialog
        showPaymentDialog?.let { dialogData ->
            PaymentConfirmationDialog(
                dialogData = dialogData,
                onConfirm = { paymentMethod, notes ->
                    when (dialogData) {
                        is PaymentDialogData.InvoicePayment -> {
                            viewModel.confirmInvoicePayment(
                                invoiceId = dialogData.invoice.id,
                                amountPaid = dialogData.invoice.balanceDue,
                                paymentMethod = paymentMethod,
                                notes = notes
                            )
                        }
                        is PaymentDialogData.ChargePayment -> {
                            viewModel.confirmChargePayment(
                                chargeId = dialogData.charge.id,
                                paymentMethod = paymentMethod,
                                notes = notes
                            )
                        }
                    }
                    showPaymentDialog = null
                },
                onDismiss = { showPaymentDialog = null }
            )
        }

        // Tier Upgrade Dialog
        showTierUpgradeDialog?.let { dialogData ->
            TierUpgradeDialog(
                driver = dialogData.driver,
                currentTier = dialogData.currentTier,
                onConfirm = { newTier, paymentMethod, notes ->
                    viewModel.upgradeDriverTier(
                        driver = dialogData.driver,
                        newTier = newTier,
                        paymentMethod = paymentMethod,
                        notes = notes
                    )
                    showTierUpgradeDialog = null
                },
                onDismiss = { showTierUpgradeDialog = null }
            )
        }

        // Success Message Snackbar
        successMessage?.let { message ->
            LaunchedEffect(message) {
                // Auto dismiss after 3 seconds
                kotlinx.coroutines.delay(3000)
                viewModel.clearMessages()
            }
        }

        // Error Message Dialog
        errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = { viewModel.clearMessages() },
                title = { Text("Error") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearMessages() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}

@Composable
private fun DriverSearchInterface(
    searchQuery: String,
    searchResults: List<User>,
    isLoading: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onDriverSelected: (User) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Header
        Text(
            text = "Search Driver",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Enter driver email or name to search",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search by email or name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Search,
                keyboardType = KeyboardType.Email
            ),
            singleLine = true
        )

        // Loading Indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Search Results
        if (searchResults.isNotEmpty()) {
            Text(
                text = "Found ${searchResults.size} driver(s)",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(top = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(searchResults) { driver ->
                    DriverSearchResultCard(
                        driver = driver,
                        onClick = { onDriverSelected(driver) }
                    )
                }
            }
        } else if (searchQuery.isNotEmpty() && !isLoading) {
            // No results
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonSearch,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "No drivers found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Try a different search term",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun DriverSearchResultCard(
    driver: User,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = driver.name.ifEmpty { driver.fullName },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = driver.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (driver.vehicleNumber.isNotEmpty()) {
                    Text(
                        text = "Vehicle: ${driver.vehicleNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tier Badge
            TierBadge(tier = driver.subscriptionTier)

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TierBadge(tier: SubscriptionTier) {
    val (backgroundColor, textColor) = when (tier) {
        SubscriptionTier.NORMAL -> Color(0xFFE0E0E0) to Color(0xFF424242)
        SubscriptionTier.GOLD -> Color(0xFFFFF9C4) to Color(0xFFF57F17)
        SubscriptionTier.PLATINUM -> Color(0xFFECEFF1) to Color(0xFF455A64)
    }

    Surface(
        color = backgroundColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = tier.name,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DriverBillingDetails(
    driverInfo: com.example.parktrack.viewmodel.DriverBillingInfo,
    isProcessingPayment: Boolean,
    isUpdatingTier: Boolean,
    onConfirmInvoicePayment: (Invoice) -> Unit,
    onConfirmChargePayment: (ParkingCharge) -> Unit,
    onUpgradeTier: (User, SubscriptionTier) -> Unit,
    onRefresh: () -> Unit
) {
    val driver = driverInfo.user
    val invoices = driverInfo.invoices
    val unpaidCharges = driverInfo.unpaidCharges

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Driver Info Card
        DriverInfoCard(driver = driver)

        // Action Buttons
        ActionButtonsCard(
            driver = driver,
            isUpdatingTier = isUpdatingTier,
            onUpgradeTier = { onUpgradeTier(driver, driver.subscriptionTier) }
        )

        // Pending Payments Section
        if (unpaidCharges.isNotEmpty()) {
            PendingChargesSection(
                charges = unpaidCharges,
                isProcessingPayment = isProcessingPayment,
                onConfirmPayment = onConfirmChargePayment
            )
        }

        // Invoices Section
        if (invoices.isNotEmpty()) {
            InvoicesSection(
                invoices = invoices,
                isProcessingPayment = isProcessingPayment,
                onConfirmPayment = onConfirmInvoicePayment
            )
        }

        if (unpaidCharges.isEmpty() && invoices.isEmpty()) {
            NoPaymentsCard()
        }

        // Refresh Button
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Refresh Data")
        }
    }
}

@Composable
private fun DriverInfoCard(driver: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Driver Information",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = driver.name.ifEmpty { driver.fullName },
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                TierBadge(tier = driver.subscriptionTier)
            }

            Divider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem(
                    label = "Email",
                    value = driver.email,
                    modifier = Modifier.weight(1f)
                )
                InfoItem(
                    label = "Phone",
                    value = driver.phoneNumber.ifEmpty { "N/A" },
                    modifier = Modifier.weight(1f)
                )
            }

            if (driver.vehicleNumber.isNotEmpty()) {
                InfoItem(
                    label = "Vehicle",
                    value = driver.vehicleNumber
                )
            }
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun ActionButtonsCard(
    driver: User,
    isUpdatingTier: Boolean,
    onUpgradeTier: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Tier Upgrade Button (only show if not already Platinum)
            if (driver.subscriptionTier != SubscriptionTier.PLATINUM) {
                Button(
                    onClick = onUpgradeTier,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isUpdatingTier
                ) {
                    if (isUpdatingTier) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Processing...")
                    } else {
                        Icon(Icons.Default.Upgrade, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        val nextTier = when (driver.subscriptionTier) {
                            SubscriptionTier.NORMAL -> "Gold"
                            SubscriptionTier.GOLD -> "Platinum"
                            else -> ""
                        }
                        Text("Upgrade to $nextTier Tier")
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingChargesSection(
    charges: List<ParkingCharge>,
    isProcessingPayment: Boolean,
    onConfirmPayment: (ParkingCharge) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pending Charges",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Badge(
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Text("${charges.size}")
                }
            }

            charges.forEach { charge ->
                PendingChargeItem(
                    charge = charge,
                    isProcessingPayment = isProcessingPayment,
                    onConfirmPayment = { onConfirmPayment(charge) }
                )
            }
        }
    }
}

@Composable
private fun PendingChargeItem(
    charge: ParkingCharge,
    isProcessingPayment: Boolean,
    onConfirmPayment: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = charge.parkingLotName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Vehicle: ${charge.vehicleNumber}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "Entry: ${charge.entryTime?.let { dateFormat.format(it.toDate()) } ?: "N/A"}",
                    style = MaterialTheme.typography.bodySmall
                )
                val hours = (charge.durationMinutes / 60).toInt()
                val mins = (charge.durationMinutes % 60).toInt()
                Text(
                    text = "Duration: ${hours}h ${mins}m",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Rs. ${String.format("%.2f", charge.finalCharge)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )

                Button(
                    onClick = onConfirmPayment,
                    enabled = !isProcessingPayment,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.success
                    )
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Confirm Payment")
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoicesSection(
    invoices: List<Invoice>,
    isProcessingPayment: Boolean,
    onConfirmPayment: (Invoice) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Invoices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            invoices.forEach { invoice ->
                InvoiceItem(
                    invoice = invoice,
                    isProcessingPayment = isProcessingPayment,
                    onConfirmPayment = { onConfirmPayment(invoice) }
                )
            }
        }
    }
}

@Composable
private fun InvoiceItem(
    invoice: Invoice,
    isProcessingPayment: Boolean,
    onConfirmPayment: () -> Unit
) {
    val statusColor = Color(getPaymentStatusColor(invoice.paymentStatus))
    val isPending = invoice.paymentStatus == "PENDING" || invoice.paymentStatus == "PARTIAL"

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${invoice.totalSessions} sessions",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = getPaymentStatusDisplay(invoice.paymentStatus),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Total Amount",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Rs. ${String.format("%.2f", invoice.netAmount)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Balance Due",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = "Rs. ${String.format("%.2f", invoice.balanceDue)}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (invoice.balanceDue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.success
                    )
                }
            }

            if (isPending && invoice.balanceDue > 0) {
                Button(
                    onClick = onConfirmPayment,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessingPayment
                ) {
                    if (isProcessingPayment) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Confirm Payment (Rs. ${String.format("%.2f", invoice.balanceDue)})")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoPaymentsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.successContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.success
            )
            Text(
                text = "All Payments Up to Date",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.success
            )
            Text(
                text = "This driver has no pending charges or invoices",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PaymentConfirmationDialog(
    dialogData: PaymentDialogData,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    val title = when (dialogData) {
        is PaymentDialogData.InvoicePayment -> "Confirm Invoice Payment"
        is PaymentDialogData.ChargePayment -> "Confirm Charge Payment"
    }

    val amount = when (dialogData) {
        is PaymentDialogData.InvoicePayment -> dialogData.invoice.balanceDue
        is PaymentDialogData.ChargePayment -> dialogData.charge.finalCharge
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Amount to confirm: Rs. ${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Payment Method Selection
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodChip(
                        label = "Cash",
                        selected = paymentMethod == "CASH",
                        onClick = { paymentMethod = "CASH" }
                    )
                    PaymentMethodChip(
                        label = "Bank Transfer",
                        selected = paymentMethod == "BANK_TRANSFER",
                        onClick = { paymentMethod = "BANK_TRANSFER" }
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(paymentMethod, notes) }) {
                Text("Confirm Payment")
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
private fun PaymentMethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) }
    )
}

@Composable
private fun TierUpgradeDialog(
    driver: User,
    currentTier: SubscriptionTier,
    onConfirm: (SubscriptionTier, String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val nextTier = when (currentTier) {
        SubscriptionTier.NORMAL -> SubscriptionTier.GOLD
        SubscriptionTier.GOLD -> SubscriptionTier.PLATINUM
        SubscriptionTier.PLATINUM -> SubscriptionTier.PLATINUM
    }

    val upgradeFee = calculateTierUpgradeFee(currentTier, nextTier)
    var paymentMethod by remember { mutableStateOf("CASH") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Upgrade Tier") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Driver: ${driver.name}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Text(
                    text = "Current Tier: ${currentTier.name}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "New Tier: ${nextTier.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "Upgrade Fee: Rs. ${String.format("%.2f", upgradeFee)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.success
                )

                Divider()

                // Payment Method Selection
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PaymentMethodChip(
                        label = "Cash",
                        selected = paymentMethod == "CASH",
                        onClick = { paymentMethod = "CASH" }
                    )
                    PaymentMethodChip(
                        label = "Bank Transfer",
                        selected = paymentMethod == "BANK_TRANSFER",
                        onClick = { paymentMethod = "BANK_TRANSFER" }
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(nextTier, paymentMethod, notes) },
                enabled = currentTier != nextTier
            ) {
                Text("Confirm Upgrade")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Sealed class for payment dialog data
private sealed class PaymentDialogData {
    data class InvoicePayment(val invoice: Invoice) : PaymentDialogData()
    data class ChargePayment(val charge: ParkingCharge) : PaymentDialogData()
}

// Data class for tier upgrade dialog
private data class TierUpgradeDialogData(
    val driver: User,
    val currentTier: SubscriptionTier
)

// Extension for success color
private val ColorScheme.success: Color
    get() = Color(0xFF4CAF50)

private val ColorScheme.successContainer: Color
    get() = Color(0xFFE8F5E9)
