package com.example.parktrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ParkingHistoryCard(
    session: com.example.parktrack.data.model.ParkingSession,
    modifier: Modifier = Modifier
) {
    val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Session ID: ${session.id.takeLast(5)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = session.status,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            val entryStr = session.entryTime?.toDate()?.let { dateFormatter.format(it) } ?: "N/A"
            Text(
                text = "Entered: $entryStr",
                style = MaterialTheme.typography.bodySmall
            )

            session.exitTime?.let { timestamp ->
                val exitStr = dateFormatter.format(timestamp.toDate())
                Text(
                    text = "Exited: $exitStr",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}