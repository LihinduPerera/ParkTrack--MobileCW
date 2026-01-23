package com.example.parktrack.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Generic error dialog
 */
@Composable
fun ErrorDialog(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.Error,
    onDismiss: () -> Unit,
    primaryButtonText: String = "Retry",
    onPrimaryClick: () -> Unit = onDismiss,
    secondaryButtonText: String? = "Cancel",
    onSecondaryClick: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = "Error",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Text(
                text = message,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onPrimaryClick,
                modifier = Modifier.fillMaxWidth(0.4f)
            ) {
                Text(primaryButtonText)
            }
        },
        dismissButton = if (secondaryButtonText != null) {
            {
                OutlinedButton(
                    onClick = onSecondaryClick ?: onDismiss,
                    modifier = Modifier.fillMaxWidth(0.4f)
                ) {
                    Text(secondaryButtonText)
                }
            }
        } else {
            null
        }
    )
}

/**
 * Camera permission denied error dialog
 */
@Composable
fun CameraPermissionDeniedDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    
    ErrorDialog(
        title = "Camera Permission Required",
        message = "The camera permission is needed to scan QR codes. Please enable it in app settings.",
        icon = Icons.Default.Error,
        onDismiss = onDismiss,
        primaryButtonText = "Open Settings",
        onPrimaryClick = {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = android.net.Uri.fromParts("package", context.packageName, null)
                }
            )
            onDismiss()
        },
        secondaryButtonText = "Cancel",
        onSecondaryClick = onDismiss
    )
}

/**
 * Network error dialog
 */
@Composable
fun NetworkErrorDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = "No Internet Connection",
        message = "Please check your internet connection and try again.",
        icon = Icons.Default.SignalCellularAlt,
        onDismiss = onDismiss,
        primaryButtonText = "Retry",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Cancel",
        onSecondaryClick = onDismiss
    )
}

/**
 * Session creation failed dialog
 */
@Composable
fun SessionCreationFailedDialog(
    errorMessage: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = "Failed to Record Entry",
        message = errorMessage.ifEmpty { "Unable to record the parking entry. Please try again." },
        onDismiss = onDismiss,
        primaryButtonText = "Retry",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Cancel",
        onSecondaryClick = onDismiss
    )
}

/**
 * QR generation failed dialog
 */
@Composable
fun QRGenerationFailedDialog(
    errorMessage: String = "Unable to generate QR code",
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = "Failed to Generate QR Code",
        message = errorMessage,
        onDismiss = onDismiss,
        primaryButtonText = "Try Again",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Cancel",
        onSecondaryClick = onDismiss
    )
}

/**
 * QR scanning failed dialog
 */
@Composable
fun QRScanFailedDialog(
    errorMessage: String = "Unable to scan QR code",
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = "Scan Failed",
        message = errorMessage,
        onDismiss = onDismiss,
        primaryButtonText = "Try Again",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Close",
        onSecondaryClick = onDismiss
    )
}

/**
 * Session not found error dialog
 */
@Composable
fun SessionNotFoundDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = "Session Not Found",
        message = "The parking session was not found. Please verify the QR code and try again.",
        onDismiss = onDismiss,
        primaryButtonText = "Scan Again",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Close",
        onSecondaryClick = onDismiss
    )
}

/**
 * Invalid QR code error dialog
 */
@Composable
fun InvalidQRCodeDialog(
    errorMessage: String = "QR code is invalid or expired",
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ErrorDialog(
        title = "Invalid QR Code",
        message = errorMessage,
        onDismiss = onDismiss,
        primaryButtonText = "Scan Again",
        onPrimaryClick = onRetry,
        secondaryButtonText = "Close",
        onSecondaryClick = onDismiss
    )
}
