package com.example.parktrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.parktrack.data.model.Vehicle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleSelectionDialog(
    vehicles: List<Vehicle>,
    onDismiss: () -> Unit,
    onVehicleSelected: (Vehicle) -> Unit,
    isLoading: Boolean = false
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Vehicle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = MaterialTheme.typography.titleMedium.fontWeight
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (vehicles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No vehicles registered. Please add a vehicle first.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vehicles) { vehicle ->
                            VehicleSelectionItem(
                                vehicle = vehicle,
                                onVehicleSelected = onVehicleSelected,
                                onDismiss = onDismiss
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VehicleSelectionItem(
    vehicle: Vehicle,
    onVehicleSelected: (Vehicle) -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = {
            onVehicleSelected(vehicle)
            onDismiss()
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = vehicle.vehicleNumber,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = MaterialTheme.typography.titleSmall.fontWeight
            )
            Text(
                text = "${vehicle.vehicleModel} • ${vehicle.vehicleColor}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "Type: ${vehicle.vehicleType}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}