package com.example.parktrack.ui.screens

import android.location.Address
import android.location.Geocoder
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.ParkingLot
import com.example.parktrack.viewmodel.AddParkingLotViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingLotManagementScreen(
    onBackClick: () -> Unit,
    onParkingLotOperationComplete: () -> Unit,
    viewModel: AddParkingLotViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isSuccess by viewModel.isSuccess.collectAsStateWithLifecycle()
    val parkingLots by viewModel.parkingLots.collectAsStateWithLifecycle()
    val selectedParkingLot by viewModel.selectedParkingLot.collectAsStateWithLifecycle()
    val isEditMode by viewModel.isEditMode.collectAsStateWithLifecycle()
    
    // Form states
    var name by remember { mutableStateOf("") }
    var totalSpaces by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var zipCode by remember { mutableStateOf("") }
    var openingTime by remember { mutableStateOf("06:00") }
    var closingTime by remember { mutableStateOf("23:59") }
    var twentyFourHours by remember { mutableStateOf(false) }
    var hasEVCharging by remember { mutableStateOf(false) }
    var hasDisabledParking by remember { mutableStateOf(false) }
    
    // Map state
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.9271, 80.7789), 12f) // Colombo
    }
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    
    // Dialog states
    var showDeleteConfirmation by remember { mutableStateOf<ParkingLot?>(null) }
    
    // Form validation
    val isFormValid = name.isNotEmpty() && 
                     totalSpaces.isNotEmpty() && 
                     selectedLocation != null &&
                     address.isNotEmpty() &&
                     city.isNotEmpty()
    
    // Load selected parking lot data when editing
    LaunchedEffect(selectedParkingLot) {
        val currentLot = selectedParkingLot
        currentLot?.let { lot ->
            name = lot.name
            totalSpaces = lot.totalSpaces.toString()
            address = lot.address
            city = lot.city
            state = lot.state
            zipCode = lot.zipCode
            openingTime = lot.openingTime
            closingTime = lot.closingTime
            twentyFourHours = lot.twentyFourHours
            hasEVCharging = lot.hasEVCharging
            hasDisabledParking = lot.hasDisabledParking
            selectedLocation = LatLng(lot.latitude, lot.longitude)
        }
    }
    
    // Get address from coordinates
    fun getAddressFromLocation(lat: LatLng): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: List<Address>? = geocoder.getFromLocation(lat.latitude, lat.longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val returnedAddress = addresses[0]
                val addressParts = mutableListOf<String>()
                
                for (i in 0..returnedAddress.maxAddressLineIndex) {
                    addressParts.add(returnedAddress.getAddressLine(i))
                }
                
                // Auto-fill form fields
                address = addressParts.joinToString(", ")
                city = returnedAddress.locality ?: ""
                state = returnedAddress.adminArea ?: ""
                zipCode = returnedAddress.postalCode ?: ""
                
                return addressParts.joinToString(", ")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return "Unknown location"
    }
    
    // Clear form
    fun clearForm() {
        name = ""
        totalSpaces = ""
        address = ""
        city = ""
        state = ""
        zipCode = ""
        openingTime = "06:00"
        closingTime = "23:59"
        twentyFourHours = false
        hasEVCharging = false
        hasDisabledParking = false
        selectedLocation = null
        viewModel.clearSelection()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(if (isEditMode) "Edit Parking Lot" else "Manage Parking Lots") 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isEditMode) {
                        IconButton(onClick = { clearForm() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Edit")
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
        ) {
            // Map for selecting/viewing locations
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        if (!isEditMode) {
                            selectedLocation = latLng
                            getAddressFromLocation(latLng)
                        }
                    }
                ) {
                    // Show existing parking lots
                    val selectedLotId = selectedParkingLot?.id
                    parkingLots.forEach { lot ->
                        Marker(
                            state = MarkerState(position = LatLng(lot.latitude, lot.longitude)),
                            title = lot.name,
                            snippet = "${lot.availableSpaces}/${lot.totalSpaces} spaces",
                            onClick = {
                                viewModel.selectParkingLot(lot)
                                true
                            },
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                if (selectedLotId == lot.id) 
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_RED
                                else 
                                    com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_GREEN
                            )
                        )
                    }
                    
                    // Show new location marker (only when adding)
                    if (selectedLocation != null && !isEditMode) {
                        Marker(
                            state = MarkerState(position = selectedLocation!!),
                            title = "New Parking Lot Location",
                            snippet = address.ifEmpty { "Selected location" },
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                            )
                        )
                    }
                }
                
                // Instructions overlay
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    val selectedLot = selectedParkingLot
                    Text(
                        text = if (isEditMode) 
                            "Editing: ${selectedLot?.name ?: "Unknown"}" 
                        else if (selectedLocation == null) 
                            "Tap on map to add new parking lot" 
                        else 
                            "Tap again to change location",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Clear location button (only when adding)
                if (selectedLocation != null && !isEditMode) {
                    IconButton(
                        onClick = { 
                            selectedLocation = null
                            address = ""
                            city = ""
                            state = ""
                            zipCode = ""
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Clear location")
                    }
                }
            }
            
            // Form or Selected Parking Lot Info
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Show selected parking lot info or form
                val currentLot = selectedParkingLot
                if (isEditMode && currentLot != null) {
                    // Selected parking lot info with actions
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = currentLot.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            
                            Text("Address: ${currentLot.address}")
                            Text("City: ${currentLot.city}")
                            Text("Spaces: ${currentLot.availableSpaces}/${currentLot.totalSpaces}")
                            Text("Hours: ${if (currentLot.twentyFourHours) "24 Hours" else "${currentLot.openingTime} - ${currentLot.closingTime}"}")
                            
                            if (currentLot.hasEVCharging) {
                                Text("✓ EV Charging Available")
                            }
                            if (currentLot.hasDisabledParking) {
                                Text("✓ Disabled Parking Available")
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        // Form will auto-populate via LaunchedEffect
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Edit")
                                }
                                
                                Button(
                                    onClick = { showDeleteConfirmation = currentLot },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Delete")
                                }
                            }
                        }
                    }
                    
                    // Edit form
                    ParkingLotForm(
                        name = name,
                        onNameChange = { name = it },
                        totalSpaces = totalSpaces,
                        onTotalSpacesChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                totalSpaces = it
                            }
                        },
                        address = address,
                        onAddressChange = { address = it },
                        city = city,
                        onCityChange = { city = it },
                        state = state,
                        onStateChange = { state = it },
                        zipCode = zipCode,
                        onZipCodeChange = { zipCode = it },
                        openingTime = openingTime,
                        onOpeningTimeChange = { openingTime = it },
                        closingTime = closingTime,
                        onClosingTimeChange = { closingTime = it },
                        twentyFourHours = twentyFourHours,
                        onTwentyFourHoursChange = { twentyFourHours = it },
                        hasEVCharging = hasEVCharging,
                        onHasEVChargingChange = { hasEVCharging = it },
                        hasDisabledParking = hasDisabledParking,
                        onHasDisabledParkingChange = { hasDisabledParking = it },
                        isLoading = isLoading,
                        isFormValid = isFormValid,
                        onSubmit = {
                            viewModel.updateParkingLot(
                                id = currentLot.id,
                                name = name,
                                totalSpaces = totalSpaces.toIntOrNull() ?: 0,
                                latitude = selectedLocation?.latitude ?: currentLot.latitude,
                                longitude = selectedLocation?.longitude ?: currentLot.longitude,
                                address = address,
                                city = city,
                                state = state,
                                zipCode = zipCode,
                                openingTime = if (twentyFourHours) "00:00" else openingTime,
                                closingTime = if (twentyFourHours) "23:59" else closingTime,
                                twentyFourHours = twentyFourHours,
                                hasEVCharging = hasEVCharging,
                                hasDisabledParking = hasDisabledParking
                            )
                        },
                        submitText = "Update Parking Lot",
                        enabled = true
                    )
                } else {
                    // Add new parking lot form
                    ParkingLotForm(
                        name = name,
                        onNameChange = { name = it },
                        totalSpaces = totalSpaces,
                        onTotalSpacesChange = { 
                            if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                                totalSpaces = it
                            }
                        },
                        address = address,
                        onAddressChange = { address = it },
                        city = city,
                        onCityChange = { city = it },
                        state = state,
                        onStateChange = { state = it },
                        zipCode = zipCode,
                        onZipCodeChange = { zipCode = it },
                        openingTime = openingTime,
                        onOpeningTimeChange = { openingTime = it },
                        closingTime = closingTime,
                        onClosingTimeChange = { closingTime = it },
                        twentyFourHours = twentyFourHours,
                        onTwentyFourHoursChange = { twentyFourHours = it },
                        hasEVCharging = hasEVCharging,
                        onHasEVChargingChange = { hasEVCharging = it },
                        hasDisabledParking = hasDisabledParking,
                        onHasDisabledParkingChange = { hasDisabledParking = it },
                        isLoading = isLoading,
                        isFormValid = isFormValid,
                        onSubmit = {
                            viewModel.addParkingLot(
                                name = name,
                                totalSpaces = totalSpaces.toIntOrNull() ?: 0,
                                latitude = selectedLocation!!.latitude,
                                longitude = selectedLocation!!.longitude,
                                address = address,
                                city = city,
                                state = state,
                                zipCode = zipCode,
                                openingTime = if (twentyFourHours) "00:00" else openingTime,
                                closingTime = if (twentyFourHours) "23:59" else closingTime,
                                twentyFourHours = twentyFourHours,
                                hasEVCharging = hasEVCharging,
                                hasDisabledParking = hasDisabledParking
                            )
                        },
                        submitText = "Add Parking Lot",
                        enabled = selectedLocation != null
                    )
                }
            }
        }
    }
    
    // Handle success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.clearSuccess()
            if (!isEditMode) {
                clearForm()
            } else {
                onParkingLotOperationComplete()
            }
        }
    }

    // Delete confirmation dialog
    showDeleteConfirmation?.let { parkingLot ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = { Text("Delete Parking Lot") },
            text = { 
                Text("Are you sure you want to delete \"${parkingLot.name}\"? This action cannot be undone.") 
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteParkingLot(parkingLot)
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirmation = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Error message dialog
    errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ParkingLotForm(
    name: String,
    onNameChange: (String) -> Unit,
    totalSpaces: String,
    onTotalSpacesChange: (String) -> Unit,
    address: String,
    onAddressChange: (String) -> Unit,
    city: String,
    onCityChange: (String) -> Unit,
    state: String,
    onStateChange: (String) -> Unit,
    zipCode: String,
    onZipCodeChange: (String) -> Unit,
    openingTime: String,
    onOpeningTimeChange: (String) -> Unit,
    closingTime: String,
    onClosingTimeChange: (String) -> Unit,
    twentyFourHours: Boolean,
    onTwentyFourHoursChange: (Boolean) -> Unit,
    hasEVCharging: Boolean,
    onHasEVChargingChange: (Boolean) -> Unit,
    hasDisabledParking: Boolean,
    onHasDisabledParkingChange: (Boolean) -> Unit,
    isLoading: Boolean,
    isFormValid: Boolean,
    onSubmit: () -> Unit,
    submitText: String,
    enabled: Boolean = true
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Name
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Parking Lot Name *") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        // Total Spaces
        OutlinedTextField(
            value = totalSpaces,
            onValueChange = onTotalSpacesChange,
            label = { Text("Total Spaces *") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        // Address (auto-filled from map)
        OutlinedTextField(
            value = address,
            onValueChange = onAddressChange,
            label = { Text("Address *") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // City
            OutlinedTextField(
                value = city,
                onValueChange = onCityChange,
                label = { Text(" City *") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
            
            // Zip Code
            OutlinedTextField(
                value = zipCode,
                onValueChange = onZipCodeChange,
                label = { Text("Zip Code") },
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            )
        }
        
        // Time Settings
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("24 Hours")
            Switch(
                checked = twentyFourHours,
                onCheckedChange = onTwentyFourHoursChange,
                enabled = !isLoading
            )
        }
        
        if (!twentyFourHours) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = openingTime,
                    onValueChange = onOpeningTimeChange,
                    label = { Text("Opening Time") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
                OutlinedTextField(
                    value = closingTime,
                    onValueChange = onClosingTimeChange,
                    label = { Text("Closing Time") },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading
                )
            }
        }
        
        // Amenities
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("EV Charging")
            Switch(
                checked = hasEVCharging,
                onCheckedChange = onHasEVChargingChange,
                enabled = !isLoading
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Disabled Parking")
            Switch(
                checked = hasDisabledParking,
                onCheckedChange = onHasDisabledParkingChange,
                enabled = !isLoading
            )
        }
        
        // Submit Button
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth(),
            enabled = isFormValid && !isLoading && enabled
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing...")
            } else {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(submitText)
            }
        }
    }
}