package com.example.parktrack.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.parktrack.viewmodel.AuthViewModel
import coil.compose.AsyncImage
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityProfileScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onAccountSettingsClick: () -> Unit,
    onPreferencesClick: () -> Unit
) {

val user by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    val currentUser = user // Create local variable for smart cast

    // Refresh user data when screen is displayed
    LaunchedEffect(Unit) {
        authViewModel.refreshCurrentUser()
    }
    
    // Gate assignment dialog state
    var showGateDialog by remember { mutableStateOf(false) }
    val availableGates = listOf("Main Gate", "Exit Gate A", "Exit Gate B", "Gate A - North", "Gate B - South")

    // 1. Image Picker Launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            authViewModel.updateProfileImage(it) {
                Toast.makeText(context, "Admin Profile Updated!", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 2. Profile Image Section
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = user?.profileImageUrl ?: android.R.drawable.ic_menu_gallery,
                    contentDescription = "Admin Photo",
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                    contentScale = ContentScale.Crop
                )

                IconButton(
                    onClick = { launcher.launch("image/*") },
                    modifier = Modifier
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
Spacer(modifier = Modifier.height(16.dp))
            Text(currentUser?.fullName ?: "Security Officer", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = if (currentUser?.badgeId?.isNotEmpty() == true) "ID: ${currentUser.badgeId}" 
                       else if (currentUser?.id?.isNotEmpty() == true) "ID: ${currentUser.id.take(8).uppercase()}" 
                       else "ID: GUARD-XXXX",
                style = MaterialTheme.typography.bodyMedium, 
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(24.dp))

// 2. Assigned Gate & Scan Stats
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoCard(
                    "Assigned Gate", 
                    currentUser?.assignedGate?.ifEmpty { "Not Assigned" } ?: "Not Assigned", 
                    Icons.Default.DirectionsCar, 
                    Modifier.weight(1f).clickable { showGateDialog = true }
                )
                InfoCard(
                    "Scans Today", 
                    currentUser?.scansToday?.toString() ?: "0", 
                    Icons.Default.QrCodeScanner, 
                    Modifier.weight(1f)
                )
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
                        modifier = Modifier.clickable { onPreferencesClick() }
                    )
                    HorizontalDivider()
                    ListItem(
                        headlineContent = { Text("Account Settings") },
                        supportingContent = { Text("Change password, email") },
                        leadingContent = { Icon(Icons.Default.ManageAccounts, null) },
                        modifier = Modifier.clickable { onAccountSettingsClick() }
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
    
    // Gate Assignment Dialog
    if (showGateDialog) {
        AlertDialog(
            onDismissRequest = { showGateDialog = false },
            title = { Text("Assign Gate") },
            text = {
                Column {
                    Text("Select your assigned gate:")
                    Spacer(modifier = Modifier.height(8.dp))
                    availableGates.forEach { gate ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    authViewModel.updateAssignedGate(gate) {
                                        Toast.makeText(context, "Gate assigned: $gate", Toast.LENGTH_SHORT).show()
                                        showGateDialog = false
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentUser?.assignedGate == gate,
                                onClick = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(gate)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showGateDialog = false }) {
                    Text("Done")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
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