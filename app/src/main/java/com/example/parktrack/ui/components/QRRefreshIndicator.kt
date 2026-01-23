package com.example.parktrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * QR code refresh countdown indicator
 * Shows circular progress bar with countdown (30 seconds)
 */
@Composable
fun QRRefreshIndicator(
    remainingSeconds: Int,
    totalSeconds: Int = 30,
    modifier: Modifier = Modifier
) {
    val progress = remainingSeconds.toFloat() / totalSeconds.toFloat()
    
    // Color changes from green → yellow → red
    val color = when {
        remainingSeconds > 10 -> Color(0xFF4CAF50) // Green
        remainingSeconds > 5 -> Color(0xFFFFC107)  // Yellow/Orange
        else -> Color(0xFFF44336)                   // Red
    }
    
    // Capture theme color before Canvas (Canvas is not a @Composable context)
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    
    Box(
        modifier = modifier.size(80.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(80.dp)) {
            // Background circle
            drawCircle(
                color = surfaceVariantColor,
                radius = size.minDimension / 2 - 4.dp.toPx(),
                style = Stroke(width = 4.dp.toPx())
            )
            
            // Progress arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx())
            )
        }
        
        // Countdown text
        Text(
            text = "$remainingSeconds",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

/**
 * Animated QR code refresh animation
 */
@Composable
fun QRRefreshAnimation(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "qr_refresh")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "qr_rotation"
    )
    
    if (isRefreshing) {
        Box(
            modifier = modifier
                .size(48.dp)
                .graphicsLayer { rotationZ = rotation },
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    }
}

/**
 * QR expiration warning indicator (red dot when < 5 seconds)
 */
@Composable
fun QRExpirationWarning(
    remainingSeconds: Int,
    modifier: Modifier = Modifier
) {
    val shouldShow = remainingSeconds <= 5 && remainingSeconds > 0
    val scale by animateFloatAsState(
        targetValue = if (shouldShow) 1f else 0f,
        animationSpec = tween(300),
        label = "expiration_warning"
    )
    
    Box(
        modifier = modifier
            .size(12.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(
                color = Color(0xFFF44336),
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}
