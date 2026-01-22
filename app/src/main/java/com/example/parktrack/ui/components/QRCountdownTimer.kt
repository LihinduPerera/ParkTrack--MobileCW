package com.example.parktrack.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun QRCountdownTimer(
    countdown: Int,
    modifier: Modifier = Modifier
) {
    val isLowTime = countdown <= 10
    val color = if (isLowTime) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { countdown / 30f },
            modifier = Modifier,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
            strokeWidth = 4.dp
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = "Valid for:",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            
            AnimatedContent(
                targetState = countdown,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300)))
                },
                label = "countdown animation"
            ) { count ->
                Text(
                    text = "$count s",
                    style = MaterialTheme.typography.headlineSmall,
                    color = color
                )
            }
        }
    }
}
