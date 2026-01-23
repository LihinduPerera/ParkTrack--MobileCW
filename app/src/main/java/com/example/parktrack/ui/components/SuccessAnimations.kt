package com.example.parktrack.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Success checkmark animation - scales up and pulses
 */
@Composable
fun SuccessCheckmarkAnimation(
    modifier: Modifier = Modifier,
    isVisible: Boolean = true,
    backgroundColor: Color = MaterialTheme.colorScheme.primary
) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.55f,
                    stiffness = 200f
                )
            )
        } else {
            scale.animateTo(0f)
        }
    }
    
    if (isVisible) {
        Box(
            modifier = modifier
                .size(60.dp)
                .scale(scale.value)
                .background(backgroundColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                modifier = Modifier.size(36.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * QR code fade-in animation
 */
@Composable
fun QRCodeFadeInAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val alpha = remember { Animatable(0f) }
    val scale = remember { Animatable(0.8f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseOutQuart)
        )
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(300, easing = EaseInOutQuad)
        )
    }
    
    Box(
        modifier = modifier
            .scale(scale.value)
            .graphicsLayer { this.alpha = alpha.value }
    ) {
        content()
    }
}

/**
 * Success message with animation
 */
@Composable
fun AnimatedSuccessMessage(
    message: String,
    isVisible: Boolean = true,
    onAnimationComplete: () -> Unit = {}
) {
    val alpha = remember { Animatable(0f) }
    val offsetY = remember { Animatable(20f) }
    
    LaunchedEffect(isVisible) {
        if (isVisible) {
            alpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(300, easing = EaseInOutQuad)
            )
            offsetY.animateTo(
                targetValue = 0f,
                animationSpec = tween(300, easing = EaseOutQuart)
            )
            
            // Auto-dismiss after 3 seconds
            kotlinx.coroutines.delay(3000)
            onAnimationComplete()
        } else {
            alpha.animateTo(0f)
            offsetY.animateTo(20f)
        }
    }
    
    if (isVisible && alpha.value > 0) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .graphicsLayer {
                    this.alpha = alpha.value
                    translationY = offsetY.value
                }
                .background(
                    MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(12.dp)
        ) {
            androidx.compose.material3.Text(
                text = message,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Pulse animation for active session indicator
 */
@Composable
fun PulseAnimation(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val scale = remember { Animatable(1f) }
    
    LaunchedEffect(Unit) {
        while (true) {
            scale.animateTo(
                targetValue = 1.1f,
                animationSpec = tween(600, easing = EaseInOutQuad)
            )
            scale.animateTo(
                targetValue = 1f,
                animationSpec = tween(600, easing = EaseInOutQuad)
            )
        }
    }
    
    Box(
        modifier = modifier.scale(scale.value),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/**
 * Status indicator with animated color transition
 */
@Composable
fun AnimatedStatusIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val color by animateColorAsState(
        targetValue = if (isActive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300, easing = EaseInOutQuad),
        label = "status_color"
    )
    
    Box(
        modifier = modifier
            .size(12.dp)
            .background(color, shape = CircleShape)
    )
}

/**
 * Loading spinner animation
 */
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val infiniteTransition = rememberInfiniteTransition(label = "spinner")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "spinner_rotation"
    )
    
    Box(
        modifier = modifier
            .size(24.dp)
            .graphicsLayer { rotationZ = rotation },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.fillMaxSize(),
            color = color,
            strokeWidth = 2.dp
        )
    }
}

/**
 * Countdown timer animation with color change near end
 */
@Composable
fun CountdownTimerAnimation(
    durationMs: Long,
    warningThresholdMs: Long = 5000,
    onComplete: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "countdown")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "countdown_scale"
    )
    
    val isWarning = durationMs <= warningThresholdMs
    val color = if (isWarning) Color(0xFFFF9800) else MaterialTheme.colorScheme.primary
    
    Box(
        modifier = Modifier
            .scale(scale)
            .background(color, CircleShape)
            .size(8.dp)
    )
}
