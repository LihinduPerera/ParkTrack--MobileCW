package com.example.parktrack.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.parktrack.R
import com.example.parktrack.ui.components.AuthButton
import com.example.parktrack.ui.components.InputField
import com.example.parktrack.ui.theme.ErrorColor
import com.example.parktrack.ui.theme.PrimaryColor
import com.example.parktrack.viewmodel.AuthState
import com.example.parktrack.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = hiltViewModel()
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
        // Logo
        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = "App Logo",
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Title
        Text(
            text = "Welcome to ParkTrack",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = PrimaryColor
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Email Field
        InputField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        
        // Password Field
        InputField(
            value = password,
            onValueChange = { password = it },
            label = "Password",
            isPassword = true
        )
        
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
        
        // Login Button
        AuthButton(
            text = "Sign In",
            onClick = { viewModel.login(email, password) },
            isLoading = isLoading,
            enabled = email.isNotEmpty() && password.isNotEmpty()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Sign Up Link
        TextButton(
            onClick = { navController.navigate("register") }
        ) {
            Text(
                text = "Don't have an account? Sign Up",
                color = PrimaryColor
            )
        }
    }
}