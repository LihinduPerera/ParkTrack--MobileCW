package com.example.parktrack.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.ui.components.EmptyState
import com.example.parktrack.utils.NavigationUtils
import com.example.parktrack.viewmodel.ParkingLotViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParkingLotMapScreen(
    onBackClick: () -> Unit,
    viewModel: ParkingLotViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val parkingLots by viewModel.parkingLots.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var selectedLotId by remember { mutableStateOf<String?>(null) }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(6.9271, 80.7789), 10f) // Sri Lanka center
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parking Lots") },
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
            // Tab bar
            TabRow(selectedTabIndex = selectedTabIndex) {
                Tab(
                    selected = selectedTabIndex == 0,
                    onClick = { selectedTabIndex = 0 },
                    text = { Text("Map") }
                )
                Tab(
                    selected = selectedTabIndex == 1,
                    onClick = { selectedTabIndex = 1 },
                    text = { Text("List") }
                )
            }

            when (selectedTabIndex) {
                0 -> {
                    // Map View
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (parkingLots.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.LocationOn,
                            title = "No Parking Lots",
                            subtitle = "No parking lots available in your area",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                         // Zoom to selected parking lot
                         LaunchedEffect(selectedLotId) {
                             selectedLotId?.let { lotId ->
                                 val selectedLot = parkingLots.find { it.id == lotId }
                                 selectedLot?.let { lot ->
                                     cameraPositionState.animate(
                                         CameraUpdateFactory.newLatLngZoom(
                                             LatLng(lot.latitude, lot.longitude),
                                             15f
                                         ),
                                         1000
                                     )
                                 }
                             }
                         }
                         
                         GoogleMap(
                             modifier = Modifier.fillMaxSize(),
                             cameraPositionState = cameraPositionState
                         ) {
                            parkingLots.forEach { lot ->
                                Marker(
                                    state = MarkerState(position = LatLng(lot.latitude, lot.longitude)),
                                    title = lot.name,
                                    snippet = "${lot.availableSpaces}/${lot.totalSpaces} spaces",
                                    onClick = {
                                        selectedLotId = lot.id
                                        true
                                    }
                                )
                            }
                        }

                        // Show details of selected lot
                        if (selectedLotId != null) {
                            val selectedLot = parkingLots.find { it.id == selectedLotId }
                            if (selectedLot != null) {
                                ParkingLotDetailsBottomSheet(
                                    lot = selectedLot,
                                    onDismiss = { selectedLotId = null }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    // List View
                    if (isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    } else if (parkingLots.isEmpty()) {
                        EmptyState(
                            icon = Icons.Default.LocationOn,
                            title = "No Parking Lots",
                            subtitle = "No parking lots available",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(parkingLots) { lot ->
                                ParkingLotCard(
                                    lot = lot,
                                    onClick = { selectedLotId = lot.id },
                                    onNavigateClick = {
                                        NavigationUtils.navigateToParkingLot(context, lot)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("Error") },
            text = { Text(errorMessage ?: "Unknown error") },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun ParkingLotCard(
    lot: com.example.parktrack.data.model.ParkingLot,
    onClick: () -> Unit,
    onNavigateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = lot.name,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = lot.address,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Available: ${lot.availableSpaces}/${lot.totalSpaces}",
                    style = MaterialTheme.typography.bodyMedium
                )
                LinearProgressIndicator(
                    progress = { lot.availableSpaces.toFloat() / lot.totalSpaces },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
            
            // Navigation Button
            Button(
                onClick = onNavigateClick,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Navigate",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Navigate")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ParkingLotDetailsBottomSheet(
    lot: com.example.parktrack.data.model.ParkingLot,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = lot.name,
                style = MaterialTheme.typography.headlineSmall
            )
            Divider()
            DetailRow("Address", lot.address)
            DetailRow("City", lot.city)
            DetailRow("Zip Code", lot.zipCode)
            DetailRow("Available Spaces", "${lot.availableSpaces}/${lot.totalSpaces}")
            DetailRow("Occupancy", "%.1f%%".format((lot.occupiedSpaces.toFloat() / lot.totalSpaces) * 100))
            DetailRow("Hours", if (lot.twentyFourHours) "24 Hours" else "${lot.openingTime} - ${lot.closingTime}")
            if (lot.hasEVCharging) {
                DetailRow("Features", "EV Charging Available")
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            // Navigation Button
            Button(
                onClick = {
                    NavigationUtils.navigateToParkingLot(context, lot)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = "Navigate",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Navigate to Parking Lot")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
