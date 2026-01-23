package com.example.parktrack.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generic empty state composable
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = subtitle,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Empty state for no active parking sessions
 */
@Composable
fun NoActiveParkingSessionsEmpty(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.DirectionsCar,
        title = "No Active Parking Sessions",
        subtitle = "Sessions will appear here when drivers enter",
        modifier = modifier
    )
}

/**
 * Empty state for no recent scans
 */
@Composable
fun NoRecentScansEmpty(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.History,
        title = "No Recent Scans",
        subtitle = "Scanned sessions will appear here",
        modifier = modifier
    )
}

/**
 * Empty state for no parking history
 */
@Composable
fun NoParkingHistoryEmpty(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.DirectionsCar,
        title = "No Parking History",
        subtitle = "Your parking sessions will appear here",
        modifier = modifier
    )
}

/**
 * Empty state when not currently parked
 */
@Composable
fun NotCurrentlyParkedEmpty(modifier: Modifier = Modifier) {
    EmptyState(
        icon = Icons.Default.DirectionsCar,
        title = "Not Currently Parked",
        subtitle = "Generate an entry QR code to park",
        modifier = modifier.fillMaxHeight(0.5f)
    )
}
