package com.example.parktrack.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parktrack.viewmodel.ThemeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    themeViewModel: ThemeViewModel,
    onBackClick: () -> Unit
) {
    val isDarkMode by themeViewModel.isDarkMode.collectAsState()
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
                supportingContent = { Text("Toggle between dark and light theme") },
                leadingContent = { Icon(Icons.Default.DarkMode, null) },
                trailingContent = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { themeViewModel.setDarkMode(it) }
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