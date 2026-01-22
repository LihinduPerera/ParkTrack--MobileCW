package com.example.parktrack.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.parktrack.data.model.UserRole
import com.example.parktrack.ui.components.AuthButton
import com.example.parktrack.ui.components.InputField
import com.example.parktrack.ui.theme.ErrorColor
import com.example.parktrack.ui.theme.PrimaryColor
import com.example.parktrack.viewmodel.AuthState
import com.example.parktrack.viewmodel.AuthViewModel

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.DRIVER) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(viewModel.authState) {
        when (val state = viewModel.authState.value) {
            is AuthState.Loading -> {
                isLoading = true
                errorMessage = null
            }
            is AuthState.Authenticated -> {
                isLoading = false
                // Navigation will be handled by the main app
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = state.message
            }
            else -> {
                isLoading = false
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Title
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Join ParkTrack today",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Full Name Field
        InputField(
            value = fullName,
            onValueChange = { fullName = it },
            label = "Full Name"
        )
        
        // Email Field
        InputField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        
        // Phone Number Field
        InputField(
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = "Phone Number",
            keyboardType = KeyboardType.Phone
        )
        
        // Password Field
        InputField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true
        )
        
        // Confirm Password Field
        InputField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = "Confirm Password",
            isPassword = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Role Selection
        Text(
            text = "Select Role",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedRole == UserRole.DRIVER,
                        onClick = { selectedRole = UserRole.DRIVER },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedRole == UserRole.DRIVER,
                    onClick = null
                )
                Text(
                    text = "Driver",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = selectedRole == UserRole.ADMIN,
                        onClick = { selectedRole = UserRole.ADMIN },
                        role = Role.RadioButton
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedRole == UserRole.ADMIN,
                    onClick = null
                )
                Text(
                    text = "Admin",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        
        // Error Message
        if (!errorMessage.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = errorMessage!!,
                color = ErrorColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Register Button
        AuthButton(
            text = "Create Account",
            onClick = {
                if (password != confirmPassword) {
                    errorMessage = "Passwords do not match"
                    return@AuthButton
                }
                viewModel.register(email, password, fullName, phoneNumber, selectedRole)
            },
            isLoading = isLoading,
            enabled = fullName.isNotEmpty() && 
                     email.isNotEmpty() && 
                     phoneNumber.isNotEmpty() && 
                     password.isNotEmpty() && 
                     confirmPassword.isNotEmpty()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sign In Link
        TextButton(
            onClick = { navController.navigate("login") }
        ) {
            Text(
                text = "Already have an account? Sign In",
                color = PrimaryColor
            )
        }
    }
}