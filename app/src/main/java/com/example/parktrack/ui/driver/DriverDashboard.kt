package com.example.parktrack.ui.driver

import QuickStatsSection
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.ui.components.EnhancedParkingStatusCard
import com.example.parktrack.ui.components.ParkingHistoryCard
import com.example.parktrack.ui.components.QRCodeDialog
import com.example.parktrack.ui.components.VehicleSelectionDialog
import com.example.parktrack.ui.dashboard.DriverStatsSection
import com.example.parktrack.ui.dashboard.RecentActivityCompact
import com.example.parktrack.viewmodel.DriverDashboardViewModel
import com.example.parktrack.viewmodel.DriverQRViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboard(
    onLogout: () -> Unit,
    onViewBilling: () -> Unit,
    onViewReports: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToVehicles: (() -> Unit)? = null,
    onNavigateToParkingLots: (() -> Unit)? = null,
    viewModel: DriverQRViewModel = hiltViewModel(),
    dashboardViewModel: DriverDashboardViewModel = hiltViewModel()

) {
    var showContent by remember { mutableStateOf(false) }
    // state for navigation
    var selectedTab by remember { mutableIntStateOf(0) }

    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val activeSession by viewModel.activeSession.collectAsStateWithLifecycle()
    val showQRDialog by viewModel.showQRDialog.collectAsStateWithLifecycle()
    val qrCodeBitmap by viewModel.qrCodeBitmap.collectAsStateWithLifecycle()
    val qrCountdown by viewModel.qrCountdown.collectAsStateWithLifecycle()
    val qrCodeData by viewModel.qrCodeData.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val parkingDuration by viewModel.parkingDuration.collectAsStateWithLifecycle()
    val hasActiveSession by viewModel.hasActiveSession.collectAsStateWithLifecycle()
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedVehicle by viewModel.selectedVehicle.collectAsStateWithLifecycle()
    val showVehicleSelection by viewModel.showVehicleSelection.collectAsStateWithLifecycle()

    val previousSessions by dashboardViewModel.previousSessions.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        showContent = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Driver Dashboard" else "Parking History") },
                        actions = { //  'actions' BLOCK
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.QrCode, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.History, null) },
                    label = { Text("History") }
                )
            }
        }
    ) { paddingValues ->
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            )
            {
//                AnimatedVisibility(
//                    visible = showContent,
//                    enter = scaleIn(animationSpec = tween(600)) + fadeIn(animationSpec = tween(600))
//                ) {
//                    Card(
//                        modifier = Modifier.fillMaxWidth(),
//                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
//                        shape = MaterialTheme.shapes.large
//                    ) {
//                        Column(
//                            modifier = Modifier.padding(16.dp),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//                            Text(
//                                text = "Welcome, Driver!",
//                                style = MaterialTheme.typography.titleLarge,
//                                modifier = Modifier.padding(bottom = 8.dp)
//                            )
//                            Text(
//                                text = currentUser?.fullName ?: "Loading...",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
//                            )
//                            Text(
//                                text = "Your parking management dashboard",
//                                style = MaterialTheme.typography.bodyMedium,
//                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
//                            )
//                        }
//                    }
//                }

                // Parking Status Card
                EnhancedParkingStatusCard(
                    session = activeSession,
                    duration = parkingDuration,
                    modifier = Modifier.fillMaxWidth()
                )


                DriverStatsSection(
                    viewModel = dashboardViewModel,
                    modifier = Modifier.fillMaxWidth()
                )

                QuickStatsSection(
                    viewModel = dashboardViewModel,
                    modifier = Modifier.fillMaxWidth()
                )

                RecentActivityCompact(
                    viewModel = dashboardViewModel,
                    modifier = Modifier.fillMaxWidth()
                )

                // Selected Vehicle Display (if any)
                if (selectedVehicle != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
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
                                    text = "Selected Vehicle",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = selectedVehicle!!.vehicleNumber,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = MaterialTheme.typography.titleSmall.fontWeight
                                )
                                Text(
                                    text = "${selectedVehicle!!.vehicleModel} • ${selectedVehicle!!.vehicleColor}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            TextButton(
                                onClick = { viewModel.showVehicleSelection() }
                            ) {
                                Text("Change")
                            }
                        }
                    }
                }
                
                // Generate QR Code Button
                Button(
                    onClick = {
                        val qrType = if (activeSession != null) "EXIT" else "ENTRY"
                        if (selectedVehicle == null) {
                            viewModel.showVehicleSelection()
                        } else {
                            viewModel.generateQRCode(qrType)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    enabled = !isLoading && vehicles.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code",
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .align(Alignment.CenterVertically)
                        )
                    }
                    Text(
                        text = if (selectedVehicle == null) {
                            "Select Vehicle First"
                        } else if (activeSession != null) {
                            "Generate Exit QR Code"
                        } else {
                            "Generate Entry QR Code"
                        },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // --- QUICK ACTIONS SECTION ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Vehicles Button
                    OutlinedButton(
                        onClick = { onNavigateToVehicles?.invoke() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("My Vehicles", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
                    }

                    // Parking Lots Button
                    OutlinedButton(
                        onClick = { onNavigateToParkingLots?.invoke() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Parking Lots", fontSize = androidx.compose.material3.MaterialTheme.typography.labelSmall.fontSize)
                    }
                }
                // --- END QUICK ACTIONS ---

                // Billing Button (Gold Theme)
                Button(
                    onClick = onViewBilling,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = androidx.compose.ui.graphics.Color(0xFFFFD700), // VaultGold
                        contentColor = androidx.compose.ui.graphics.Color.Black
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "View Billing & Invoices",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // Reports Button (Platinum/Surface Theme)
                Button(
                    onClick = onViewReports,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text(
                        text = "Parking Session Reports",
                        modifier = Modifier.padding(vertical = 8.dp),
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                // --- END OF NEW SECTION ---

                // Logout Button
                Button(
                    onClick = onLogout,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Logout")
                }
            }



        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(previousSessions) { session ->
                    ParkingHistoryCard(session = session)
                }
            }
        }
    }

    if (showQRDialog && qrCodeBitmap != null) {
        QRCodeDialog(
            bitmap = qrCodeBitmap,
            countdown = qrCountdown,
            qrString = qrCodeData,
            onDismiss = { viewModel.closeQRDialog() }
        )
    }
    
    if (showVehicleSelection) {
        VehicleSelectionDialog(
            vehicles = vehicles,
            onDismiss = { viewModel.hideVehicleSelection() },
            onVehicleSelected = { vehicle ->
                viewModel.selectVehicle(vehicle)
                // Auto-generate QR after vehicle selection
                val qrType = if (activeSession != null) "EXIT" else "ENTRY"
                viewModel.generateQRCodeWithVehicle(vehicle, qrType)
            },
            isLoading = false
        )
    }
}