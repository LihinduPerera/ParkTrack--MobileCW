package com.example.parktrack.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.ui.components.ManualExitConfirmDialog
import com.example.parktrack.ui.viewmodels.AdminScannerViewModel
import com.example.parktrack.utils.ParkingHelper
import com.google.firebase.Timestamp

@Composable
fun AdminActiveSessionsScreen(
    viewModel: AdminScannerViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val activeSessions by viewModel.activeSessions.collectAsState()
    var selectedSessionForExit by remember { mutableStateOf<ParkingSession?>(null) }
    var showExitDialog by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            // Header
            TopAppBar(
                title = { Text("Active Parking Sessions") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
            
            // Session count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Sessions: ${activeSessions.size}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Badge(
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ),
                    content = {
                        Text(
                            text = "${activeSessions.size}",
                            modifier = Modifier.padding(4.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                )
            }
            
            // Sessions list or empty state
            if (activeSessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "No Active Parking Sessions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "All parking spaces are currently free",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(activeSessions) { session ->
                        ActiveSessionCardWithMenu(
                            session = session,
                            onManualExit = { selectedSession ->
                                selectedSessionForExit = selectedSession
                                showExitDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Manual exit confirmation dialog
    if (showExitDialog && selectedSessionForExit != null) {
        ManualExitConfirmDialog(
            driverName = selectedSessionForExit!!.driverName,
            vehicleNumber = selectedSessionForExit!!.vehicleNumber,
            onConfirm = {
                // Perform manual exit
                viewModel.manualExit(selectedSessionForExit!!.id)
                showExitDialog = false
                selectedSessionForExit = null
            },
            onDismiss = {
                showExitDialog = false
                selectedSessionForExit = null
            }
        )
    }
}

@Composable
private fun ActiveSessionCardWithMenu(
    session: ParkingSession,
    onManualExit: (ParkingSession) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Driver name
                Text(
                    text = session.driverName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Vehicle and gate
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Vehicle: ${session.vehicleNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Gate: ${session.gateLocation}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Entry time
                Text(
                    text = "Entry: ${ParkingHelper.formatTimestamp(session.entryTime ?: Timestamp.now())}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                // Duration
                val duration = calculateDurationString(session.entryTime ?: Timestamp.now())
                Text(
                    text = "Duration: $duration",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // Menu button
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(40.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(32.dp)
                ) {
                    Text("⋯", fontSize = 20.sp)
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Manual Exit") },
                        onClick = {
                            showMenu = false
                            onManualExit(session)
                        }
                    )
                }
            }
        }
    }
}

private fun calculateDurationString(entryTime: Timestamp): String {
    val now = Timestamp.now()
    val durationMs = now.toDate().time - entryTime.toDate().time
    val durationMinutes = (durationMs / 60000).toInt()
    
    return if (durationMinutes < 1) {
        "< 1m"
    } else if (durationMinutes < 60) {
        "${durationMinutes}m"
    } else {
        val hours = durationMinutes / 60
        val minutes = durationMinutes % 60
        if (minutes > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${hours}h"
        }
    }
}
