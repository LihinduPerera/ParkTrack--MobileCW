package com.example.parktrack.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.parktrack.data.model.SubscriptionTier
import com.example.parktrack.viewmodel.ParkingRateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingInfoScreen(
    onBackClick: () -> Unit,
    viewModel: ParkingRateViewModel = hiltViewModel()
) {
    val parkingRates by viewModel.currentRates.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.loadDefaultRates()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pricing Plans") },
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
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Header
            Text(
                text = "Choose Your Parking Plan",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Select the plan that best fits your parking needs",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Pricing Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NormalTierCard(
                    modifier = Modifier.weight(1f),
                    rates = parkingRates
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldTierCard(
                    modifier = Modifier.weight(1f),
                    rates = parkingRates
                )
                PlatinumTierCard(
                    modifier = Modifier.weight(1f),
                    rates = parkingRates
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Rate Types Information
            RateTypesInformationCard(rates = parkingRates)

            Spacer(modifier = Modifier.height(24.dp))

            // Billing Rules
            BillingRulesCard()
        }
    }
}

@Composable
private fun NormalTierCard(
    modifier: Modifier = Modifier,
    rates: com.example.parktrack.data.model.ParkingRate?
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "NORMAL",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = "Rs. ${rates?.normalRate ?: "100"}/hour",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Divider(modifier = Modifier.fillMaxWidth())

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Pay full hourly rate",
                    color = MaterialTheme.colorScheme.onSurface
                )
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Minimum charge: 1 hour",
                    color = MaterialTheme.colorScheme.onSurface
                )
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Daily cap: Rs. ${rates?.maxDailyPrice ?: "50.0"}",
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Perfect for occasional parkers",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoldTierCard(
    modifier: Modifier = Modifier,
    rates: com.example.parktrack.data.model.ParkingRate?
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFFD700),
                    Color(0xFFFFA500)
                )
            ).let { MaterialTheme.colorScheme.surface }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFFFD700),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "POPULAR",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "GOLD",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B)
            )

            Text(
                text = "Rs. ${rates?.goldRate ?: "80"}/hour",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB8860B)
            )

            Divider(modifier = Modifier.fillMaxWidth())

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.Star,
                    text = "First hour FREE",
                    color = Color(0xFFB8860B)
                )
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "20% discount on rates",
                    color = Color(0xFFB8860B)
                )
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Daily cap: Rs. ${rates?.maxDailyPrice ?: "40.0"}",
                    color = Color(0xFFB8860B)
                )
            }

            Text(
                text = "Best for daily commuters",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color(0xFFB8860B)
            )
        }
    }
}

@Composable
private fun PlatinumTierCard(
    modifier: Modifier = Modifier,
    rates: com.example.parktrack.data.model.ParkingRate?
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFE5E4E2),
                    Color(0xFFC0C0C0)
                )
            ).let { MaterialTheme.colorScheme.surface }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Color(0xFFC0C0C0),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "PREMIUM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Text(
                text = "PLATINUM",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF708090)
            )

            Text(
                text = "Rs. ${rates?.platinumRate ?: "60"}/hour",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF708090)
            )

            Divider(modifier = Modifier.fillMaxWidth())

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FeatureItem(
                    icon = Icons.Default.Star,
                    text = "First hour FREE",
                    color = Color(0xFF708090)
                )
                FeatureItem(
                    icon = Icons.Default.Star,
                    text = "40% discount on rates",
                    color = Color(0xFF708090)
                )
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Daily cap: Rs. ${rates?.maxDailyPrice ?: "30.0"}",
                    color = Color(0xFF708090)
                )
                FeatureItem(
                    icon = Icons.Default.CheckCircle,
                    text = "Best rates guaranteed",
                    color = Color(0xFF708090)
                )
            }

            Text(
                text = "Perfect for frequent users",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = Color(0xFF708090)
            )
        }
    }
}

@Composable
private fun FeatureItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = color
        )
    }
}

@Composable
private fun RateTypesInformationCard(
    rates: com.example.parktrack.data.model.ParkingRate?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Parking Rate Types",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RateTypeInfo(
                    title = "Normal Rate",
                    description = "Standard parking rate during regular hours",
                    price = "Rs. ${rates?.basePricePerHour ?: "10.0"}/hour"
                )
                
                RateTypeInfo(
                    title = "VIP Rate",
                    description = "Premium parking spots with enhanced services",
                    price = "${rates?.vipMultiplier ?: "1.5"}x normal rate"
                )
                
                RateTypeInfo(
                    title = "Hourly Rate",
                    description = "Flexible hourly billing",
                    price = "Rs. ${rates?.hourlyRate ?: "10.0"}/hour"
                )
                
                RateTypeInfo(
                    title = "Overnight Rate",
                    description = "Special rate for overnight parking (${rates?.overnightStartHour ?: "22"}:00 - ${rates?.overnightEndHour ?: "06"}:00)",
                    price = "Rs. ${rates?.overnightRate ?: "5.0"}/hour"
                )
            }
        }
    }
}

@Composable
private fun RateTypeInfo(
    title: String,
    description: String,
    price: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
        Text(
            text = price,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BillingRulesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Billing Rules",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BillingRule(
                    title = "Normal Users",
                    rule = "Charged for full hour even if parked for only 1 minute"
                )
                
                BillingRule(
                    title = "Gold & Platinum Users",
                    rule = "First hour is completely FREE - charges start after exceeding 1 hour"
                )
                
                BillingRule(
                    title = "Daily Caps",
                    rule = "Maximum daily charge applies per tier to prevent excessive billing"
                )
                
                BillingRule(
                    title = "Overnight Parking",
                    rule = "Special discounted rates apply during overnight hours"
                )
            }
        }
    }
}

@Composable
private fun BillingRule(
    title: String,
    rule: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = rule,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}