package com.example.parktrack.ui.admin
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.ui.admin.components.ActivityChart
import com.example.parktrack.ui.admin.components.StatsRow
import com.example.parktrack.viewmodel.AdminDashboardViewModel
import com.example.parktrack.viewmodel.AdminQRHistoryViewModel
import com.example.parktrack.data.model.EnrichedParkingSession
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit,
    onScanQRCode: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToReports: () -> Unit,
    onAddParkingLot: () -> Unit,
    onNavigateToBillingManagement: () -> Unit = {},
    viewModel: AdminDashboardViewModel = hiltViewModel(),
    qrHistoryViewModel: AdminQRHistoryViewModel = hiltViewModel()
) {
    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }

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

    // QR History states
    val allScans by qrHistoryViewModel.allScans.collectAsStateWithLifecycle()
    val isLoading by qrHistoryViewModel.isLoading.collectAsStateWithLifecycle()
    val filterType by qrHistoryViewModel.filterType.collectAsStateWithLifecycle()
    val searchQuery by qrHistoryViewModel.searchQuery.collectAsStateWithLifecycle()
    var showFilterMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (selectedTab == 0) "Admin Dashboard" else "QR Scan History") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile")
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
            // Home Tab
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

                    Button(onClick = onNavigateToBillingManagement, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)) {
                        Icon(Icons.Default.Payments, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Manage Payments & Tiers")
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
        } else {
            // History Tab
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { qrHistoryViewModel.setSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search by driver, phone, or vehicle...") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true
                )

                // Filter Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = filterType == AdminQRHistoryViewModel.ScanFilter.ALL,
                        onClick = { qrHistoryViewModel.setFilter(AdminQRHistoryViewModel.ScanFilter.ALL) },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = filterType == AdminQRHistoryViewModel.ScanFilter.ACTIVE,
                        onClick = { qrHistoryViewModel.setFilter(AdminQRHistoryViewModel.ScanFilter.ACTIVE) },
                        label = { Text("Active") }
                    )
                    FilterChip(
                        selected = filterType == AdminQRHistoryViewModel.ScanFilter.COMPLETED,
                        onClick = { qrHistoryViewModel.setFilter(AdminQRHistoryViewModel.ScanFilter.COMPLETED) },
                        label = { Text("Completed") }
                    )

                    Box {
                        FilterChip(
                            selected = filterType == AdminQRHistoryViewModel.ScanFilter.ENTRY_TODAY ||
                                     filterType == AdminQRHistoryViewModel.ScanFilter.EXIT_TODAY,
                            onClick = { showFilterMenu = true },
                            label = { Text("Today") }
                        )
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Entries Today") },
                                onClick = {
                                    qrHistoryViewModel.setFilter(AdminQRHistoryViewModel.ScanFilter.ENTRY_TODAY)
                                    showFilterMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Exits Today") },
                                onClick = {
                                    qrHistoryViewModel.setFilter(AdminQRHistoryViewModel.ScanFilter.EXIT_TODAY)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Total Scans", allScans.size.toString())
                    StatItem("Active", allScans.count { it.status == "ACTIVE" }.toString())
                    StatItem("Completed", allScans.count { it.status == "COMPLETED" }.toString())
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Scans List
                val filteredScans = qrHistoryViewModel.getFilteredScans()

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (filteredScans.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "No scans found",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                if (searchQuery.isNotEmpty()) "Try adjusting your search"
                                else "No QR scans in history",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredScans) { scan ->
                            QRScanHistoryCard(scan = scan)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun QRScanHistoryCard(
    scan: EnrichedParkingSession,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (scan.status) {
                "ACTIVE" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                "COMPLETED" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Session: ${scan.id.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                StatusChip(status = scan.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Driver Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Driver",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = scan.driverName.ifEmpty { "Unknown" },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (scan.driverPhoneNumber.isNotEmpty()) {
                        Text(
                            text = scan.driverPhoneNumber,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Vehicle",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = scan.vehicleNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (scan.vehicleModel.isNotEmpty()) {
                        Text(
                            text = scan.vehicleModel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider()
            Spacer(modifier = Modifier.height(8.dp))

            // Time Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Entry",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = scan.entryTime?.toDate()?.let { dateFormatter.format(it) } ?: "N/A",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                scan.exitTime?.let { exitTime ->
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Exit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = dateFormatter.format(exitTime.toDate()),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Duration for completed sessions
            if (scan.status == "COMPLETED" && scan.durationMinutes > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = "Duration: ${formatDuration(scan.durationMinutes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Admin info
            if (scan.adminName.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Scanned by: ${scan.adminName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun StatusChip(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "ACTIVE" -> Pair(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
        "COMPLETED" -> Pair(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer
        )
        else -> Pair(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    Surface(
        color = backgroundColor,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.labelSmall,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatDuration(minutes: Long): String {
    return when {
        minutes < 60 -> "$minutes min"
        minutes < 1440 -> {
            val hours = minutes / 60
            val mins = minutes % 60
            if (mins > 0) "$hours hr $mins min" else "$hours hr"
        }
        else -> {
            val days = minutes / 1440
            val hours = (minutes % 1440) / 60
            if (hours > 0) "$days day $hours hr" else "$days day"
        }
    }
}
