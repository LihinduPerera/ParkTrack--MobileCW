package com.example.parktrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    onBackClick: () -> Unit,
    onUpgradeSubscription: (SubscriptionTier) -> Unit,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val currentTier by viewModel.currentTier.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadCurrentSubscription()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subscription Plans") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Current Subscription Status
            CurrentSubscriptionCard(currentTier)

            // Subscription Plans
            Text(
                text = "Choose Your Plan",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            SubscriptionPlans(
                currentTier = currentTier,
                onUpgradeSubscription = onUpgradeSubscription,
                isLoading = isLoading
            )
        }

        if (errorMessage != null) {
            ErrorDialog(
                title = "Error",
                message = errorMessage ?: "Unknown error",
                onDismiss = { viewModel.clearError() }
            )
        }
    }
}

@Composable
private fun CurrentSubscriptionCard(currentTier: SubscriptionTier) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when (currentTier) {
                SubscriptionTier.NORMAL -> Color(0xFF2196F3)
                SubscriptionTier.GOLD -> Color(0xFFFFD700)
                SubscriptionTier.PLATINUM -> Color(0xFFE5E4E2)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Current Plan",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (currentTier) {
                        SubscriptionTier.NORMAL -> Icons.Default.CheckCircle
                        SubscriptionTier.GOLD -> Icons.Default.Star
                        SubscriptionTier.PLATINUM -> Icons.Default.Star
                    },
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = currentTier.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SubscriptionPlans(
    currentTier: SubscriptionTier,
    onUpgradeSubscription: (SubscriptionTier) -> Unit,
    isLoading: Boolean
) {
    val plans = listOf(
        SubscriptionPlan(
            tier = SubscriptionTier.NORMAL,
            name = "Normal",
            price = "Pay as you park",
            features = listOf(
                "Full hourly rate charged",
                "No minimum duration discount",
                "Standard parking rates",
                "Pay for every minute (rounded to hour)"
            ),
            color = Color(0xFF2196F3),
            isPopular = false
        ),
        SubscriptionPlan(
            tier = SubscriptionTier.GOLD,
            name = "Gold",
            price = "20% discount",
            features = listOf(
                "First hour FREE",
                "20% discount on hourly rates",
                "VIP parking access",
                "Priority customer support"
            ),
            color = Color(0xFFFFD700),
            isPopular = true
        ),
        SubscriptionPlan(
            tier = SubscriptionTier.PLATINUM,
            name = "Platinum",
            price = "40% discount",
            features = listOf(
                "First hour FREE",
                "40% discount on hourly rates",
                "VIP parking access",
                "Premium customer support",
                "Monthly billing options"
            ),
            color = Color(0xFFE5E4E2),
            isPopular = false
        )
    )

    plans.forEach { plan ->
        SubscriptionPlanCard(
            plan = plan,
            isCurrentPlan = plan.tier == currentTier,
            onUpgradeSubscription = onUpgradeSubscription,
            isLoading = isLoading
        )
    }
}

@Composable
private fun SubscriptionPlanCard(
    plan: SubscriptionPlan,
    isCurrentPlan: Boolean,
    onUpgradeSubscription: (SubscriptionTier) -> Unit,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (plan.isPopular) 8.dp else 4.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Plan Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = plan.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = plan.price,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                if (plan.isPopular) {
                    Surface(
                        modifier = Modifier.clip(RoundedCornerShape(16.dp)),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "POPULAR",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features
            plan.features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = feature,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = { onUpgradeSubscription(plan.tier) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCurrentPlan && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCurrentPlan) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            ) {
                if (isCurrentPlan) {
                    Text("CURRENT PLAN")
                } else if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("UPGRADE TO ${plan.name.uppercase()}")
                }
            }
        }
    }
}

@Composable
private fun ErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

private data class SubscriptionPlan(
    val tier: SubscriptionTier,
    val name: String,
    val price: String,
    val features: List<String>,
    val color: Color,
    val isPopular: Boolean
)