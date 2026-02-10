package com.example.parktrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.parktrack.data.model.ParkingSession
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.utils.ParkingHelper
import com.example.parktrack.viewmodel.ScanResultDetails

@Composable
fun ScanSuccessDialog(
    session: ParkingSession,
    sessionType: String, // "ENTRY" or "EXIT"
    driverName: String,
    onDismiss: () -> Unit,
    vehicleModel: String = "",
    vehicleColor: String = "",
    scanResultDetails: ScanResultDetails? = null,
    onMarkAsPaid: (() -> Unit)? = null,
    onMarkAsUnpaid: (() -> Unit)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Success icon
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF4CAF50) // Green color for success
                )
                
                // Title
                Text(
                    text = if (sessionType == "ENTRY") "Entry Recorded" else "Exit Recorded",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Driver Info Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Driver name
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Driver",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = driverName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                scanResultDetails?.driverEmail?.takeIf { it.isNotEmpty() }?.let {
                                    Text(
                                        text = it,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                        
                        // Vehicle number and details
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalParking,
                                contentDescription = "Vehicle",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "Vehicle: ${session.vehicleNumber}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (vehicleModel.isNotEmpty() || vehicleColor.isNotEmpty()) {
                                    Text(
                                        text = "${vehicleModel}${if (vehicleModel.isNotEmpty() && vehicleColor.isNotEmpty()) " • " else ""}${vehicleColor}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        
                        // Tier information
                        scanResultDetails?.let { details ->
                            Divider(modifier = Modifier.padding(vertical = 4.dp))
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CreditCard,
                                    contentDescription = "Tier",
                                    modifier = Modifier.size(20.dp),
                                    tint = when (details.subscriptionTier) {
                                        SubscriptionTier.NORMAL -> Color(0xFF9E9E9E)
                                        SubscriptionTier.GOLD -> Color(0xFFFFD700)
                                        SubscriptionTier.PLATINUM -> Color(0xFFE5E4E2)
                                    }
                                )
                                Column {
                                    Text(
                                        text = "Tier: ${details.subscriptionTier.name}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = when (details.subscriptionTier) {
                                            SubscriptionTier.NORMAL -> MaterialTheme.colorScheme.onSurface
                                            SubscriptionTier.GOLD -> Color(0xFFB8860B)
                                            SubscriptionTier.PLATINUM -> Color(0xFF6A5ACD)
                                        }
                                    )
                                    Text(
                                        text = details.feeDescription,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Gate and Time Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Gate location
                        Text(
                            text = "Gate: ${session.gateLocation}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        // Entry time
                        if (sessionType == "ENTRY") {
                            Text(
                                text = "Entry Time: ${ParkingHelper.formatTimestamp(session.entryTime)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        } else {
                            // Exit info with duration
                            scanResultDetails?.let { details ->
                                Text(
                                    text = "Entry Time: ${ParkingHelper.formatTimestamp(details.entryTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Exit Time: ${ParkingHelper.formatTimestamp(details.exitTime)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Duration: ${formatDuration(details.durationMinutes)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                // Fee and Payment Status Card - ONLY SHOW ON EXIT
                scanResultDetails?.let { details ->
                    if (sessionType == "EXIT") {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    details.parkingFee == 0.0 -> Color(0xFFE8F5E9) // Light green for free
                                    details.isPaid -> Color(0xFFE8F5E9) // Light green for paid
                                    else -> Color(0xFFFFF3E0) // Light orange for unpaid
                                }
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AttachMoney,
                                        contentDescription = "Fee",
                                        modifier = Modifier.size(24.dp),
                                        tint = when {
                                            details.parkingFee == 0.0 -> Color(0xFF4CAF50)
                                            details.isPaid -> Color(0xFF4CAF50)
                                            else -> Color(0xFFFF9800)
                                        }
                                    )
                                    Text(
                                        text = "Parking Fee: Rs. %.2f".format(details.parkingFee),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = when {
                                            details.parkingFee == 0.0 -> Color(0xFF2E7D32)
                                            details.isPaid -> Color(0xFF2E7D32)
                                            else -> Color(0xFFE65100)
                                        }
                                    )
                                }
                                
                                // Payment Status Badge
                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = when {
                                                details.parkingFee == 0.0 -> Color(0xFF4CAF50)
                                                details.isPaid -> Color(0xFF4CAF50)
                                                else -> Color(0xFFFF9800)
                                            },
                                            shape = MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = details.paymentStatus.uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                
                                // Show payment instruction for unpaid fees
                                if (!details.isPaid && details.parkingFee > 0) {
                                    Text(
                                        text = "Collect payment from driver",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action Buttons - Only show payment buttons on EXIT with unpaid fee
                if (sessionType == "EXIT" &&
                    scanResultDetails?.parkingFee != null && 
                    scanResultDetails.parkingFee > 0 && 
                    !scanResultDetails.isPaid) {
                    
                    // Show "Mark as Paid" button for unpaid charges
                    if (onMarkAsPaid != null) {
                        Button(
                            onClick = {
                                onMarkAsPaid()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Text("MARK AS PAID (Rs. ${String.format("%.2f", scanResultDetails.parkingFee)})")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    // Show "Mark as Unpaid (Pay Later)" button
                    if (onMarkAsUnpaid != null) {
                        Button(
                            onClick = {
                                onMarkAsUnpaid()
                                onDismiss()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800)
                            )
                        ) {
                            Text("MARK AS UNPAID (Pay Later)")
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
                
                // OK Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
fun ScanErrorDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large
                )
                .padding(24.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Error icon
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Error",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                
                // Title
                Text(
                    text = "Scan Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Error message
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(12.dp)
                )
                
                // Retry Button
                Button(
                    onClick = onRetry,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Try Again")
                }
                
                // Dismiss Button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors()
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

// Helper function to format duration
private fun formatDuration(minutes: Long): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}
