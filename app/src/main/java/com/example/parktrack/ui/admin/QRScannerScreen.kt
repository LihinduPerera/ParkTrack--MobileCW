package com.example.parktrack.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.ui.components.CameraPreview
import com.example.parktrack.ui.components.ScanErrorDialog
import com.example.parktrack.ui.components.ScanSuccessDialog
import com.example.parktrack.utils.ParkingHelper
import com.example.parktrack.viewmodel.AdminScannerViewModel
import com.example.parktrack.viewmodel.ScanState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QRScannerScreen(
    onBackPress: () -> Unit,
    viewModel: AdminScannerViewModel = hiltViewModel()
) {
    val scanState by viewModel.scanState.collectAsStateWithLifecycle()
    val selectedGate by viewModel.selectedGate.collectAsStateWithLifecycle()
    val scanResultMessage by viewModel.scanResultMessage.collectAsStateWithLifecycle()
    val recentScans by viewModel.recentScans.collectAsStateWithLifecycle()
    val scannedDriver by viewModel.scannedDriver.collectAsStateWithLifecycle()
    val sessionType by viewModel.sessionType.collectAsStateWithLifecycle()
    val scannedSession by viewModel.scannedQRData.collectAsStateWithLifecycle()
    val scannedVehicleModel by viewModel.scannedVehicleModel.collectAsStateWithLifecycle()
    val scannedVehicleColor by viewModel.scannedVehicleColor.collectAsStateWithLifecycle()
    
    var showGateMenu by remember { mutableStateOf(false) }
    
    val gateOptions = listOf("Main Gate", "Exit Gate A", "Exit Gate B")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onBackPress) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
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
        ) {
            // Gate Selection Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Select Gate",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Box {
                    Button(
                        onClick = { showGateMenu = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(selectedGate, fontWeight = FontWeight.SemiBold)
                    }
                    
                    DropdownMenu(
                        expanded = showGateMenu,
                        onDismissRequest = { showGateMenu = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .align(Alignment.BottomStart)
                    ) {
                        gateOptions.forEach { gate ->
                            DropdownMenuItem(
                                text = { Text(gate) },
                                onClick = {
                                    viewModel.setGate(gate)
                                    showGateMenu = false
                                }
                            )
                        }
                    }
                }
            }
            
            // Camera Preview Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(300.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                CameraPreview(
                    onBarcodeDetected = { qrString ->
                        viewModel.processScannedQR(qrString)
                    },
                    isScanning = scanState != ScanState.PROCESSING,
                    modifier = Modifier.fillMaxSize()
                )
                
                // Scanning indicator
                if (scanState == ScanState.SCANNING) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(250.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                    )
                }
            }
            
            // Status Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .background(
                        color = when (scanState) {
                            ScanState.ERROR -> MaterialTheme.colorScheme.errorContainer
                            ScanState.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = when (scanState) {
                        ScanState.IDLE -> "Position QR code within frame"
                        ScanState.SCANNING -> "Scanning..."
                        ScanState.PROCESSING -> "Processing..."
                        ScanState.SUCCESS -> scanResultMessage
                        ScanState.ERROR -> scanResultMessage
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = when (scanState) {
                        ScanState.ERROR -> MaterialTheme.colorScheme.error
                        ScanState.SUCCESS -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (scanState == ScanState.SUCCESS || scanState == ScanState.ERROR) FontWeight.SemiBold else FontWeight.Normal
                )
            }
            
            // Recent Scans Section
            Text(
                text = "Recent Scans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (recentScans.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(60.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No scans yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentScans.forEach { session ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = session.driverName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = session.vehicleNumber,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = session.gateLocation,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Button(
                                        onClick = {},
                                        modifier = Modifier
                                            .height(32.dp)
                                            .padding(horizontal = 8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (session.status == "ACTIVE")
                                                MaterialTheme.colorScheme.primary
                                            else
                                                MaterialTheme.colorScheme.tertiary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary
                                        ),
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = if (session.status == "ACTIVE") "ENTRY" else "EXIT",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    
                                    Text(
                                        text = ParkingHelper.formatTimestamp(session.entryTime),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            // Bottom spacing
            Box(modifier = Modifier.height(16.dp))
        }
    }
    
    // Success Dialog
    if (scanState == ScanState.SUCCESS && scannedDriver != null) {
        // Create a dummy session for display
        val displaySession = scannedDriver?.let {
            com.example.parktrack.data.model.ParkingSession(
                driverId = it.id,
                driverName = it.fullName,
                vehicleNumber = scannedSession?.vehicleNumber ?: it.vehicleNumber,
                gateLocation = selectedGate,
                status = if (sessionType == "ENTRY") "ACTIVE" else "COMPLETED"
            )
        }
        
        if (displaySession != null) {
            ScanSuccessDialog(
                session = displaySession,
                sessionType = sessionType,
                driverName = scannedDriver!!.fullName,
                onDismiss = { viewModel.resetScanState() },
                vehicleModel = scannedVehicleModel,
                vehicleColor = scannedVehicleColor
            )
        }
    }
    
    // Error Dialog
    if (scanState == ScanState.ERROR) {
        ScanErrorDialog(
            errorMessage = scanResultMessage,
            onRetry = { viewModel.resetScanState() },
            onDismiss = { viewModel.resetScanState() }
        )
    }
}
