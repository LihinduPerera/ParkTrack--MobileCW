package com.example.parktrack.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.parktrack.data.model.SubscriptionTier

@Composable
fun UserTierCard(
    tier: SubscriptionTier,
    modifier: Modifier = Modifier,
    onViewPricing: () -> Unit = {}
) {
    val (tierColor, tierGradient, tierName) = when (tier) {
        SubscriptionTier.NORMAL -> Triple(
            Color(0xFF616161), // Gray
            Brush.horizontalGradient(colors = listOf(Color(0xFF757575), Color(0xFF616161))),
            "Normal"
        )
        SubscriptionTier.GOLD -> Triple(
            Color(0xFFFFD700), // Gold
            Brush.horizontalGradient(colors = listOf(Color(0xFFFFD700), Color(0xFFFFA500))),
            "Gold"
        )
        SubscriptionTier.PLATINUM -> Triple(
            Color(0xFFC0C0C0), // Silver
            Brush.horizontalGradient(colors = listOf(Color(0xFFE5E4E2), Color(0xFFC0C0C0))),
            "Platinum"
        )
    }

    val (tierBenefits, tierColorName) = when (tier) {
        SubscriptionTier.NORMAL -> listOf(
            "Rs 100/hour",
            "Rs 100 minimum on exit",
            "Hours rounded UP"
        ) to "Gray"
        SubscriptionTier.GOLD -> listOf(
            "Rs 80/hour",
            "First hour FREE",
            "Completed hours only"
        ) to "Gold"
        SubscriptionTier.PLATINUM -> listOf(
            "Rs 60/hour",
            "First hour FREE",
            "Completed hours only"
        ) to "Silver"
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(tierGradient)
                .padding(20.dp)
        ) {
            // Tier Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Plan",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = tierName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Benefits
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tierBenefits.forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                        )
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Pricing Button
            OutlinedButton(
                onClick = onViewPricing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, Color.White.copy(alpha = 0.5f)
                )
            ) {
                Text(
                    text = "View All Pricing Plans",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}