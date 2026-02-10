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
import com.example.parktrack.viewmodel.AddParkingLotViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddParkingLotScreen(
    onBackClick: () -> Unit,
    onParkingLotAdded: () -> Unit,
    viewModel: AddParkingLotViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val isSuccess by viewModel.isSuccess.collectAsStateWithLifecycle()
    
    // Form fields
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
    
    // Form validation
    val isFormValid = name.isNotEmpty() && 
                     totalSpaces.isNotEmpty() && 
                     selectedLocation != null &&
                     address.isNotEmpty() &&
                     city.isNotEmpty()
    
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
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Parking Lot") },
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
            // Map for selecting location
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    onMapClick = { latLng ->
                        selectedLocation = latLng
                        getAddressFromLocation(latLng)
                    }
                ) {
                    selectedLocation?.let { location ->
                        Marker(
                            state = MarkerState(position = location),
                            title = "Parking Lot Location",
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
                    Text(
                        text = if (selectedLocation == null) 
                            "Tap on map to select location" 
                        else 
                            "Tap again to change location",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                // Clear location button
                if (selectedLocation != null) {
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
            
            // Form fields
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Parking Lot Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                // Total Spaces
                OutlinedTextField(
                    value = totalSpaces,
                    onValueChange = { 
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            totalSpaces = it
                        }
                    },
                    label = { Text("Total Spaces *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
                
                // Address (auto-filled from map)
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
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
                        onValueChange = { city = it },
                        label = { Text(" City *") },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading
                    )
                    
                    // Zip Code
                    OutlinedTextField(
                        value = zipCode,
                        onValueChange = { zipCode = it },
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
                        onCheckedChange = { twentyFourHours = it },
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
                            onValueChange = { openingTime = it },
                            label = { Text("Opening Time") },
                            modifier = Modifier.weight(1f),
                            enabled = !isLoading
                        )
                        OutlinedTextField(
                            value = closingTime,
                            onValueChange = { closingTime = it },
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
                        onCheckedChange = { hasEVCharging = it },
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
                        onCheckedChange = { hasDisabledParking = it },
                        enabled = !isLoading
                    )
                }
                
                // Save Button
                Button(
                    onClick = {
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
                    modifier = Modifier.fillMaxWidth(),
                    enabled = isFormValid && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adding...")
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Parking Lot")
                    }
                }
            }
        }
    }
    
    // Handle success
    LaunchedEffect(isSuccess) {
        if (isSuccess) {
            viewModel.clearSuccess()
            onParkingLotAdded()
        }
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