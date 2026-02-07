package com.example.parktrack.ui.dashboard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.parktrack.viewmodel.DriverDashboardViewModel

@Composable
fun DriverStatsSection(
    viewModel: DriverDashboardViewModel,
    modifier: Modifier = Modifier
) {
    val sessionsToday by viewModel.todaySessionsCount.collectAsState()
    val totalMinutesToday by viewModel.todayTotalMinutes.collectAsState()

    val hours = totalMinutesToday / 60
    val remainingMinutes = totalMinutesToday % 60

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "Today Sessions",
            value = sessionsToday.toString(),
            modifier = Modifier.weight(1f)
        )

        StatCard(
            title = "Parked Today",
            value = "${hours}h ${remainingMinutes}m",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
