package com.example.parktrack.ui.admin

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBackClick: () -> Unit
) {
    var isDarkMode by remember { mutableStateOf(false) }
    var isVibrationEnabled by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Preferences") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text("Dark Mode") },
                supportingContent = { Text("Force dark theme across the app") },
                leadingContent = { Icon(Icons.Default.DarkMode, null) },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { isDarkMode = it }
                    )
                }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Vibration Feedback") },
                supportingContent = { Text("Vibrate on successful QR scans") },
                leadingContent = { Icon(Icons.Default.Vibration, null) },
                trailingContent = {
                    Switch(
                        checked = isVibrationEnabled,
                        onCheckedChange = { isVibrationEnabled = it }
                    )
                }
            )
        }
    }
}