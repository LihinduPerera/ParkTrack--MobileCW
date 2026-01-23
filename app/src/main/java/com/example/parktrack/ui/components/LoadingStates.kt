package com.example.parktrack.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect for loading placeholders
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(4.dp)
) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val shimmerPosition = transition.animateFloat(
        initialValue = -1000f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "shimmer_position"
    )
    
    Box(
        modifier = modifier
            .background(shimmerColors[0], shape)
            .alpha(0.7f)
    )
}

/**
 * Skeleton loader for user profile card
 */
@Composable
fun UserProfileSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar skeleton
        ShimmerEffect(
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Name skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Email skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Skeleton loader for parking status card
 */
@Composable
fun ParkingStatusSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Title skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(16.dp)
        )
        
        // Content skeleton
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            ShimmerEffect(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            ShimmerEffect(
                modifier = Modifier
                    .weight(1f)
                    .height(12.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Status line skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp)
        )
    }
}

/**
 * Skeleton loader for session card
 */
@Composable
fun SessionCardSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Header skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(16.dp)
        )
        
        // Two line skeleton
        repeat(3) {
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Skeleton loader for QR dialog
 */
@Composable
fun QRCodeDialogSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Title skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(20.dp)
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // QR placeholder skeleton
        ShimmerEffect(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Timer skeleton
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(14.dp)
                .align(Alignment.CenterHorizontally)
        )
    }
}

/**
 * Shimmer list loader
 */
@Composable
fun ShimmerListLoader(
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        repeat(itemCount) {
            SessionCardSkeleton()
            if (it < itemCount - 1) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
