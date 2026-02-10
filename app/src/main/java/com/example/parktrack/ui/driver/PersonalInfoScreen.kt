package com.example.parktrack.ui.driver

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.parktrack.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalInfoScreen(
    authViewModel: AuthViewModel,
    onBackClick: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    var showEditDialog by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf("") }
    var editLabel by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf(EditType.NONE) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personal Information") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Display User Data from AuthViewModel - Now Editable
            EditableInfoField(
                label = "Full Name",
                value = currentUser?.fullName?.takeIf { it.isNotBlank() } ?: currentUser?.name?.takeIf { it.isNotBlank() } ?: "Not set",
                onEditClick = {
                    editType = EditType.FULL_NAME
                    editLabel = "Full Name"
                    editField = currentUser?.fullName ?: ""
                    showEditDialog = true
                }
            )
            
            InfoField(label = "Email Address", value = currentUser?.email ?: "Loading...")
            InfoField(label = "Account Role", value = currentUser?.role?.name ?: "Driver")
            InfoField(label = "Phone Number", value = currentUser?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not set")

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                "Tap on any field with the edit icon to update your information.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Edit Dialog
        if (showEditDialog) {
            AlertDialog(
                onDismissRequest = { showEditDialog = false },
                title = { Text("Update $editLabel") },
                text = {
                    OutlinedTextField(
                        value = editField,
                        onValueChange = { editField = it },
                        label = { Text(editLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            when (editType) {
                                EditType.FULL_NAME -> {
                                    authViewModel.updateFullName(editField,
                                        onSuccess = {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Full name updated successfully!")
                                            }
                                        },
                                        onError = { error ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Error: $error")
                                            }
                                        }
                                    )
                                }
                                else -> {}
                            }
                            showEditDialog = false
                        }
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

enum class EditType {
    NONE,
    FULL_NAME,
    PHONE_NUMBER
}

@Composable
fun InfoField(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.bodyLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
fun EditableInfoField(label: String, value: String, onEditClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEditClick),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
        Icon(
            imageVector = Icons.Default.Edit,
            contentDescription = "Edit $label",
            tint = MaterialTheme.colorScheme.primary
        )
    }
    HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
}