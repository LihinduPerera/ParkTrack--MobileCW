package com.example.parktrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HighlightOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.parktrack.data.model.ParkingSession

@Composable
fun ParkingStatusIndicator(
    session: ParkingSession?,
    modifier: Modifier = Modifier
) {
    val isParked = session != null && session.status == "ACTIVE"
    val statusColor = if (isParked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val statusText = if (isParked) "Parked" else "Not Parked"
    val icon = if (isParked) Icons.Default.CheckCircle else Icons.Default.HighlightOff
    
    Row(
        modifier = modifier
            .background(
                color = statusColor.copy(alpha = 0.1f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = statusText,
            tint = statusColor,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelMedium,
                color = statusColor
            )
            
            if (isParked && session != null) {
                Text(
                    text = "Gate: ${session.gateLocation}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
