package com.example.parktrack.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.ParkingRate
import com.example.parktrack.data.model.RateType
import com.example.parktrack.viewmodel.ParkingRateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateConfigurationScreen(
    onBackClick: () -> Unit,
    viewModel: ParkingRateViewModel = hiltViewModel()
) {
    val parkingLots by viewModel.parkingLots.collectAsStateWithLifecycle()
    val currentRates by viewModel.currentRates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val successMessage by viewModel.successMessage.collectAsStateWithLifecycle()

    var selectedParkingLot by remember { mutableStateOf("") }
    var selectedRateType by remember { mutableStateOf(RateType.NORMAL) }

    LaunchedEffect(Unit) {
        viewModel.loadParkingLots()
    }

    LaunchedEffect(selectedParkingLot, selectedRateType) {
        if (selectedParkingLot.isNotEmpty()) {
            viewModel.loadRatesForLot(selectedParkingLot, selectedRateType)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rate Configuration") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Parking Lot Selection
            ParkingLotSelector(
                parkingLots = parkingLots,
                selectedLot = selectedParkingLot,
                onLotSelected = { selectedParkingLot = it }
            )

            // Rate Type Selection
            RateTypeSelector(
                selectedType = selectedRateType,
                onTypeSelected = { selectedRateType = it }
            )

            // Rate Configuration Form
            if (selectedParkingLot.isNotEmpty()) {
                RateConfigurationForm(
                    rate = currentRates,
                    isLoading = isLoading,
                    onSaveRate = { rateConfig ->
                        viewModel.saveRate(
                            parkingLotId = selectedParkingLot,
                            rateType = selectedRateType,
                            rateConfig = rateConfig
                        )
                    }
                )
            }

            // Rate Preview
            if (currentRates != null) {
                RatePreviewCard(rate = currentRates!!)
            }
        }

        // Messages
        errorMessage?.let {
            LaunchedEffect(it) {
                viewModel.clearError()
            }
            ErrorDialog(
                title = "Error",
                message = it,
                onDismiss = { viewModel.clearError() }
            )
        }

        successMessage?.let {
            LaunchedEffect(it) {
                viewModel.clearSuccess()
            }
            SuccessDialog(
                title = "Success",
                message = it,
                onDismiss = { viewModel.clearSuccess() }
            )
        }
    }
}

@Composable
private fun ParkingLotSelector(
    parkingLots: List<String>,
    selectedLot: String,
    onLotSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = selectedLot,
        onValueChange = { },
        readOnly = true,
        label = { Text("Select Parking Lot") },
        trailingIcon = {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false }
    ) {
        parkingLots.forEach { lot ->
            DropdownMenuItem(
                text = { Text(lot) },
                onClick = {
                    onLotSelected(lot)
                    expanded = false
                }
            )
        }
    }
}

@Composable
private fun RateTypeSelector(
    selectedType: RateType,
    onTypeSelected: (RateType) -> Unit
) {
    val rateTypes = RateType.values()

    Column {
        Text(
            text = "Rate Type",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rateTypes.forEach { type ->
                FilterChip(
                    selected = selectedType == type,
                    onClick = { onTypeSelected(type) },
                    label = { Text(type.name) }
                )
            }
        }
    }
}

@Composable
private fun RateConfigurationForm(
    rate: ParkingRate?,
    isLoading: Boolean,
    onSaveRate: (ParkingRate) -> Unit
) {
    var basePricePerHour by remember(rate) { mutableStateOf(rate?.basePricePerHour?.toString() ?: "10.0") }
    var normalRate by remember(rate) { mutableStateOf(rate?.normalRate?.toString() ?: "10.0") }
    var goldRate by remember(rate) { mutableStateOf(rate?.goldRate?.toString() ?: "8.0") }
    var platinumRate by remember(rate) { mutableStateOf(rate?.platinumRate?.toString() ?: "6.0") }
    var maxDailyPrice by remember(rate) { mutableStateOf(rate?.maxDailyPrice?.toString() ?: "50.0") }
    var vipMultiplier by remember(rate) { mutableStateOf(rate?.vipMultiplier?.toString() ?: "1.5") }
    var overnightRate by remember(rate) { mutableStateOf(rate?.overnightRate?.toString() ?: "5.0") }
    var overnightStartHour by remember(rate) { mutableStateOf(rate?.overnightStartHour?.toString() ?: "22") }
    var overnightEndHour by remember(rate) { mutableStateOf(rate?.overnightEndHour?.toString() ?: "6") }

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Rate Configuration",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            // Base Rates
            Text(
                text = "Base Rates",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            
            OutlinedTextField(
                value = basePricePerHour,
                onValueChange = { basePricePerHour = it },
                label = { Text("Base Price Per Hour") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = maxDailyPrice,
                onValueChange = { maxDailyPrice = it },
                label = { Text("Maximum Daily Price") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Subscription Tier Rates
            Text(
                text = "Subscription Tier Rates",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = normalRate,
                onValueChange = { normalRate = it },
                label = { Text("Normal Tier Rate (per hour)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = goldRate,
                onValueChange = { goldRate = it },
                label = { Text("Gold Tier Rate (per hour)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = platinumRate,
                onValueChange = { platinumRate = it },
                label = { Text("Platinum Tier Rate (per hour)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Special Rates
            Text(
                text = "Special Rates",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value = vipMultiplier,
                onValueChange = { vipMultiplier = it },
                label = { Text("VIP Multiplier") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = overnightRate,
                onValueChange = { overnightRate = it },
                label = { Text("Overnight Rate (per hour)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = overnightStartHour,
                    onValueChange = { overnightStartHour = it },
                    label = { Text("Overnight Start (24h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = overnightEndHour,
                    onValueChange = { overnightEndHour = it },
                    label = { Text("Overnight End (24h)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }

            // Save Button
            Button(
                onClick = {
                    val rateConfig = ParkingRate(
                        id = rate?.id ?: "",
                        parkingLotId = rate?.parkingLotId ?: "",
                        rateType = rate?.rateType ?: RateType.NORMAL,
                        basePricePerHour = basePricePerHour.toDoubleOrNull() ?: 10.0,
                        maxDailyPrice = maxDailyPrice.toDoubleOrNull() ?: 50.0,
                        normalRate = normalRate.toDoubleOrNull() ?: 10.0,
                        goldRate = goldRate.toDoubleOrNull() ?: 8.0,
                        platinumRate = platinumRate.toDoubleOrNull() ?: 6.0,
                        vipMultiplier = vipMultiplier.toDoubleOrNull() ?: 1.5,
                        overnightRate = overnightRate.toDoubleOrNull() ?: 5.0,
                        overnightStartHour = overnightStartHour.toIntOrNull() ?: 22,
                        overnightEndHour = overnightEndHour.toIntOrNull() ?: 6,
                        isActive = true
                    )
                    onSaveRate(rateConfig)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Rate Configuration")
                }
            }
        }
    }
}

@Composable
private fun RatePreviewCard(rate: ParkingRate) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Rate Preview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text("Normal: Rs. ${rate.normalRate}/hour")
            Text("Gold: Rs. ${rate.goldRate}/hour (First hour free)")
            Text("Platinum: Rs. ${rate.platinumRate}/hour (First hour free)")
            Text("VIP: ${rate.vipMultiplier}x multiplier")
            Text("Overnight: Rs. ${rate.overnightRate}/hour (${rate.overnightStartHour}:00 - ${rate.overnightEndHour}:00)")
            Text("Daily Cap: Rs. ${rate.maxDailyPrice}")
        }
    }
}

@Composable
private fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun SuccessDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}