package com.example.parktrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.utils.DateTimeFormatter

/**
 * Session details dialog for viewing full parking session information
 */
@Composable
fun SessionDetailsDialog(
    session: ParkingSession,
    currentDuration: String = "",
    onDismiss: () -> Unit,
    onManualExit: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .background(MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.large
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Session Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Driver Section
                    DetailSection(
                        title = "Driver",
                        items = listOf(
                            "Name" to session.driverName,
                            "ID" to (session.driverId.take(8) + "...")
                        )
                    )
                    
                    Divider()
                    
                    // Vehicle Section
                    DetailSection(
                        title = "Vehicle",
                        items = listOf(
                            "Registration" to session.vehicleNumber
                        )
                    )
                    
                    Divider()
                    
                    // Parking Section
                    DetailSection(
                        title = "Parking",
                        items = listOf(
                            "Gate" to session.gateLocation,
                            "Status" to session.status
                        )
                    )
                    
                    Divider()
                    
                    // Time Section
                    DetailSection(
                        title = "Time",
                        items = listOf(
                            "Entry" to DateTimeFormatter.formatFullDateTime(session.entryTime!!),
                            "Duration" to currentDuration
                        ) + if (session.exitTime != null) {
                            listOf("Exit" to DateTimeFormatter.formatFullDateTime(session.exitTime!!))
                        } else {
                            emptyList()
                        }
                    )
                    
                    if (session.exitTime == null) {
                        Divider()
                        
                        // Scanned By Section
                        DetailSection(
                            title = "Scanned By",
                            items = listOf(
                                "Admin" to session.adminName,
                                "Admin ID" to (session.scannedByAdminId.take(8) + "...")
                            )
                        )
                    }
                    
                    // Action Buttons
                    if (onManualExit != null && session.status == "ACTIVE") {
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Button(
                            onClick = {
                                onManualExit()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text("Manual Exit")
                        }
                    }
                    
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            "Close",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Reusable detail section in session dialog
 */
@Composable
private fun DetailSection(
    title: String,
    items: List<Pair<String, String>>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                )
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { (label, value) ->
                DetailItem(label = label, value = value)
            }
        }
    }
}

/**
 * Single detail item row
 */
@Composable
private fun DetailItem(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
