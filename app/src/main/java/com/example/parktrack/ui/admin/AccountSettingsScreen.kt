package com.example.parktrack.ui.admin

import androidx.compose.foundation.clickable

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    onBackClick: () -> Unit,
    onChangePasswordClick: () -> Unit,
    onUpdateEmailClick: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account Settings") },
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
                headlineContent = { Text("Change Password") },
                leadingContent = { Icon(Icons.Default.Lock, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = androidx.compose.ui.Modifier.clickable { onChangePasswordClick() }
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text("Email Address") },
                supportingContent = { Text("Update your contact email") },
                leadingContent = { Icon(Icons.Default.Email, null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                modifier = Modifier.clickable { onUpdateEmailClick() }
            )
        }
    }
}