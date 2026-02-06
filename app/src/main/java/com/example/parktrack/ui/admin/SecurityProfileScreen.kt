package com.example.parktrack.ui.admin

import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.vector.ImageVector


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.parktrack.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security Profile") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Guard Information Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Shield, contentDescription = null,modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Officer John Smith", style = MaterialTheme.typography.headlineSmall)
            Text("ID: GUARD-9920", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Assigned Gate & Scan Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard("Assigned Gate", "Gate A - North", Icons.Default.DirectionsCar, Modifier.weight(1f))
                InfoCard("Scans Today", "42", Icons.Default.QrCodeScanner, Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Settings & Management
            Text("Management", modifier = Modifier.align(Alignment.Start), style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)) {
                Column {
                    ListItem(
                        headlineContent = { Text("Preferences") },
                        supportingContent = { Text("Dark mode, vibration feedback") },
                        leadingContent = { Icon(Icons.Default.Settings, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Account Settings") },
                        supportingContent = { Text("Change password, email") },
                        leadingContent = { Icon(Icons.Default.ManageAccounts, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Logout
            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun InfoCard(title: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(title, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge)
        }
    }
}