package com.example.parktrack.ui.admin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.ui.admin.components.ActivityChart
import com.example.parktrack.ui.admin.components.StatsRow
import com.example.parktrack.viewmodel.AdminDashboardViewModel
import com.example.parktrack.data.model.EnrichedParkingSession

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit,
    onScanQRCode: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAddParkingLot: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {

    val total by viewModel.totalScansToday.collectAsStateWithLifecycle()
    val active by viewModel.activeNow.collectAsStateWithLifecycle()
    val entries by viewModel.entriesToday.collectAsStateWithLifecycle()
    val exits by viewModel.exitsToday.collectAsStateWithLifecycle()
    val recent by viewModel.recentScans.collectAsStateWithLifecycle()
    val chart by viewModel.last6hChart.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isInitializingData by viewModel.isInitializingData.collectAsStateWithLifecycle()

    val pullState = rememberPullRefreshState(
        refreshing = refreshing,
        onRefresh = { viewModel.refresh() }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .pullRefresh(pullState)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                StatsRow(total, active, entries, exits)

                ActivityChart(chart)

                Button(onClick = onScanQRCode, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCode, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Scan QR Code")
                }

                Button(onClick = onAddParkingLot, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Parking Lot")
                }

                OutlinedButton(onClick = onNavigateToReports, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Assessment, null)
                    Spacer(Modifier.width(8.dp))
                    Text("View Reports")
                }

                Button(
                    onClick = { viewModel.initializeSampleData() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isInitializingData
                ) {
                    if (isInitializingData) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Initializing...")
                    } else {
                        Icon(Icons.Default.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Initialize Sample Data")
                    }
                }

                Text("Recent Scans", style = MaterialTheme.typography.titleMedium)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        if (recent.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "No recent scans",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                recent.forEach { scan ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                        )
                                    ) {
                                        Column(Modifier.padding(8.dp)) {
                                            val driverInfo = if (scan.driverPhoneNumber.isNotEmpty()) {
                                                "${scan.driverName} - ${scan.driverPhoneNumber}"
                                            } else {
                                                scan.driverName
                                            }
                                            val vehicleInfo = if (scan.vehicleModel.isNotEmpty()) {
                                                "${scan.vehicleNumber} - ${scan.vehicleModel}"
                                            } else {
                                                scan.vehicleNumber
                                            }
                                            
                                            Text(
                                                driverInfo, 
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                vehicleInfo, 
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                            Text(
                                                scan.status, 
                                                style = MaterialTheme.typography.bodySmall,
                                                color = when(scan.status) {
                                                    "ACTIVE" -> MaterialTheme.colorScheme.primary
                                                    "COMPLETED" -> MaterialTheme.colorScheme.secondary
                                                    else -> MaterialTheme.colorScheme.onSurface
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                    Text("Logout")
                }
            }

            PullRefreshIndicator(
                refreshing = refreshing,
                state = pullState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    }
}
