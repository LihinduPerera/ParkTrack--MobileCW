package com.example.parktrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parktrack.ui.admin.AdminDashboard
import com.example.parktrack.ui.admin.QRScannerScreen
import com.example.parktrack.ui.auth.LoginScreen
import com.example.parktrack.ui.auth.RegisterScreen
import com.example.parktrack.ui.driver.DriverDashboard
import com.example.parktrack.ui.screens.BillingScreen
import com.example.parktrack.ui.screens.BillingViewModel
import com.example.parktrack.viewmodel.AuthState
import com.example.parktrack.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object DriverDashboard : Screen("driver_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
    object QRScanner : Screen("qr_scanner")
    object Billing : Screen("billing")
    object Reports : Screen("reports")
}

@Composable
fun ParkTrackNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    authViewModel: AuthViewModel
) {
    val authState by authViewModel.authState.collectAsState()
    val isCheckingAuth by authViewModel.isCheckingAuth.collectAsState()
    val scope = rememberCoroutineScope()

    // Handle navigation based on auth state changes
    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val state = authState as AuthState.Authenticated
                val currentRoute = navController.currentDestination?.route
                // Navigate to appropriate dashboard if not already there
                when (state.user.role) {
                    com.example.parktrack.data.model.UserRole.DRIVER -> {
                        if (currentRoute != Screen.DriverDashboard.route) {
                            navController.navigate(Screen.DriverDashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                    com.example.parktrack.data.model.UserRole.ADMIN -> {
                        if (currentRoute != Screen.AdminDashboard.route) {
                            navController.navigate(Screen.AdminDashboard.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                val currentRoute = navController.currentDestination?.route
                // Only navigate to login if not already on an auth screen
                if (currentRoute != Screen.Login.route &&
                    currentRoute != Screen.Register.route &&
                    !isCheckingAuth) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {
                // Other states (Loading, Error, Idle) don't trigger navigation
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                navController = navController,
                viewModel = authViewModel,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }
        composable(Screen.Register.route) {
            RegisterScreen(
                navController = navController,
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route)
                }
            )
        }
        composable(Screen.DriverDashboard.route) {
            DriverDashboard(
                onLogout = {
                    scope.launch {
                        authViewModel.logout()
                    }
                },
                onViewBilling = {
                    navController.navigate(Screen.Billing.route)
                },
                onViewReports = {
                    navController.navigate(Screen.Reports.route)
                }
            )
        }
        composable(Screen.AdminDashboard.route) {
            AdminDashboard(
                onLogout = {
                    scope.launch {
                        authViewModel.logout()
                    }
                },
                onScanQRCode = {
                    navController.navigate(Screen.QRScanner.route)
                }
            )
        }
        composable(Screen.QRScanner.route) {
            QRScannerScreen(
                onBackPress = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Billing.route) {
            // You need to provide the ViewModel here.
            // Using 'viewModel()' requires 'androidx.lifecycle:lifecycle-viewmodel-compose'
            val billingViewModel: BillingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            BillingScreen(viewModel = billingViewModel)
        }
    }
}