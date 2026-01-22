
package com.example.parktrack.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.parktrack.data.model.UserRole
import com.example.parktrack.ui.components.AuthButton
import com.example.parktrack.ui.components.InputField
import com.example.parktrack.ui.theme.ErrorColor
import com.example.parktrack.ui.theme.PrimaryColor
import com.example.parktrack.ui.theme.SuccessColor
import com.example.parktrack.viewmodel.AuthState
import com.example.parktrack.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun RegisterScreen(
    navController: NavController,
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit = { navController.navigate("login") }
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf(UserRole.DRIVER) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var passwordMismatchError by remember { mutableStateOf(false) }
    var showContent by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    // Collect auth state as State
    val authState by viewModel.authState.collectAsState()
    val isCheckingAuth by viewModel.isCheckingAuth.collectAsState()

    LaunchedEffect(Unit) {
        showContent = true
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Loading -> {
                isLoading = true
                errorMessage = null
                successMessage = null
            }
            is AuthState.Authenticated -> {
                isLoading = false
                successMessage = "Registration successful!"
                // Show success message briefly
                scope.launch {
                    snackbarHostState.showSnackbar("Registration successful!")
                }
                // Navigation is handled by ParkTrackNavHost based on user role
            }
            is AuthState.Error -> {
                isLoading = false
                errorMessage = state.message
                successMessage = null
                // Show error message in snackbar
                scope.launch {
                    snackbarHostState.showSnackbar(state.message)
                }
            }
            is AuthState.Unauthenticated -> {
                isLoading = false
                // User is logged out - this is handled by navigation
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
        AnimatedVisibility(
            visible = showContent,
            enter = slideInVertically(initialOffsetY = { 1000 }, animationSpec = tween(800)) + fadeIn(animationSpec = tween(800))
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(32.dp))
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
            }
        }
        // Full Name Field
        InputField(
            value = fullName,
            onValueChange = {
                fullName = it
                errorMessage = null
            },
            label = "Full Name"
        )
        // Email Field
        InputField(
            value = email,
            onValueChange = {
                email = it
                errorMessage = null
            },
            label = "Email",
            keyboardType = KeyboardType.Email
        )
        // Phone Number Field
        InputField(
            value = phoneNumber,
            onValueChange = {
                phoneNumber = it
                errorMessage = null
            },
            label = "Phone Number",
            keyboardType = KeyboardType.Phone
        )
        // Password Field
        InputField(
            value = password,
            onValueChange = {
                password = it
                errorMessage = null
                passwordMismatchError = false
            },
            label = "Password",
            isPassword = true
        )
        // Confirm Password Field
        InputField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                errorMessage = null
                passwordMismatchError = false
            },
            label = "Confirm Password",
            isPassword = true
        )
        AnimatedVisibility(
            visible = passwordMismatchError,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Passwords do not match",
                color = ErrorColor,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Role Selection
        Text(
            text = "Select Role",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth()
        )
//        Spacer(modifier = Modifier.height(1.dp))
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            Row(
                modifier = Modifier
//                    .fillMaxWidth()
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
//                    .fillMaxWidth()
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
        AnimatedVisibility(
            visible = !errorMessage.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (!errorMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = errorMessage!!,
                    color = ErrorColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        AnimatedVisibility(
            visible = !successMessage.isNullOrEmpty(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (!successMessage.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = successMessage!!,
                    color = SuccessColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
//        Spacer(modifier = Modifier.height(24.dp))
        // Register Button
        AuthButton(
            text = "Create Account",
            onClick = {
                // Validate inputs
                if (fullName.isEmpty() || email.isEmpty() || phoneNumber.isEmpty() ||
                    password.isEmpty() || confirmPassword.isEmpty()) {
                    errorMessage = "Please fill in all fields"
                    return@AuthButton
                }
                if (password != confirmPassword) {
                    passwordMismatchError = true
                    errorMessage = "Passwords do not match"
                    return@AuthButton
                }
                // Additional validation
                if (password.length < 6) {
                    errorMessage = "Password must be at least 6 characters"
                    return@AuthButton
                }
                if (!email.contains("@")) {
                    errorMessage = "Please enter a valid email address"
                    return@AuthButton
                }
                // Clear any previous errors
                passwordMismatchError = false
                errorMessage = null
                // Call registration
                viewModel.register(email, password, fullName, phoneNumber, selectedRole)
            },
            isLoading = isLoading || isCheckingAuth,
            enabled = fullName.isNotEmpty() &&
                    email.isNotEmpty() &&
                    phoneNumber.isNotEmpty() &&
                    password.isNotEmpty() &&
                    confirmPassword.isNotEmpty() &&
                    !isLoading
        )
//        Spacer(modifier = Modifier.height(16.dp))
        // Sign In Link
        TextButton(
            onClick = onNavigateToLogin
        ) {
            Text(
                text = "Already have an account? Sign In",
                color = PrimaryColor
            )
        }
    }
    // Snackbar for messages
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
    )
}