package com.example.parktrack.ui.driver

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.parktrack.R
import com.example.parktrack.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBillingClick: () -> Unit,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    var notificationsEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Driver Profile") },
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Profile Picture Section
            Box(contentAlignment = Alignment.BottomEnd) {
                Image(
                    painter = painterResource(id = android.R.drawable.ic_menu_gallery), // Placeholder
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = { /* TODO: Image Picker Logic */ },
                    modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Statistics Display (VaultPark style)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                StatItem("Total Parks", "128")
                StatItem("Vault Points", "450")
                StatItem("Tier", "Gold")
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Personal Info / Settings List
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ListItem(
                        headlineContent = { Text("Personal Information") },
                        supportingContent = { Text("John Doe - john@example.com") },
                        leadingContent = { Icon(Icons.Default.Person, null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Notification Preferences") },
                        leadingContent = { Icon(Icons.Default.Notifications, null) },
                        trailingContent = {
                            Switch(checked = notificationsEnabled, onCheckedChange = { notificationsEnabled = it })
                        }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Change Password") },
                        leadingContent = { Icon(Icons.Default.Lock, null) },
                        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                        modifier = Modifier.clickable { /* TODO */ }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 4. Account Management & Logout
            TextButton(onClick = { /* TODO: Delete Account */ }) {
                Text("Delete Account", color = MaterialTheme.colorScheme.error)
            }

            Button(
                onClick = onLogoutClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("Logout")
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = Color(0xFFFFD700)) // Gold
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}