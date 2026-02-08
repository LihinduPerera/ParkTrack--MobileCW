package com.example.parktrack.ui.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parktrack.ui.components.ParkingHistoryCard
import com.example.parktrack.viewmodel.DriverDashboardViewModel

@Composable
fun RecentActivityCompact(
    viewModel: DriverDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val sessions by viewModel.recentThreeSessions.collectAsState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        Text(
            text = "Recent Activity",
            style = MaterialTheme.typography.titleMedium
        )

        if (sessions.isEmpty()) {
            Text(
                text = "No recent sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            sessions.forEach { session ->
                ParkingHistoryCard(
                    session = session,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
