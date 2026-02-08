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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun AdminDashboard(
    onLogout: () -> Unit,
    onScanQRCode: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {

    val total by viewModel.totalScansToday.collectAsStateWithLifecycle()
    val active by viewModel.activeNow.collectAsStateWithLifecycle()
    val entries by viewModel.entriesToday.collectAsStateWithLifecycle()
    val exits by viewModel.exitsToday.collectAsStateWithLifecycle()
    val recent by viewModel.recentScans.collectAsStateWithLifecycle()
    val chart by viewModel.last6hChart.collectAsStateWithLifecycle()
    val refreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

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

                Text("Recent Scans", style = MaterialTheme.typography.titleMedium)

                recent.forEach { scan ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(scan.driverName)
                            Text(scan.vehicleNumber)
                            Text(scan.status)
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
