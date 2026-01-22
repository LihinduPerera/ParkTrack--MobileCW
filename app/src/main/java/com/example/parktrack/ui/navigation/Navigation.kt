package com.example.parktrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parktrack.ui.admin.AdminDashboard
import com.example.parktrack.ui.auth.LoginScreen
import com.example.parktrack.ui.auth.RegisterScreen
import com.example.parktrack.ui.driver.DriverDashboard
import com.example.parktrack.viewmodel.AuthState
import com.example.parktrack.viewmodel.AuthViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Register : Screen("register")
    object DriverDashboard : Screen("driver_dashboard")
    object AdminDashboard : Screen("admin_dashboard")
}

@Composable
fun ParkTrackNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route
) {
    // Create the AuthViewModel using hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()

    // Collect the authState as State
    val authState by authViewModel.authState.collectAsState()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Authenticated -> {
                val state = authState as AuthState.Authenticated
                when (state.user.role) {
                    com.example.parktrack.data.model.UserRole.DRIVER -> {
                        navController.navigate(Screen.DriverDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                    com.example.parktrack.data.model.UserRole.ADMIN -> {
                        navController.navigate(Screen.AdminDashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                }
            }
            is AuthState.Unauthenticated -> {
                // If we're already at login, don't navigate
                if (navController.currentDestination?.route != Screen.Login.route) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            else -> {}
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }

        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }

        composable(Screen.DriverDashboard.route) {
            DriverDashboard()
        }

        composable(Screen.AdminDashboard.route) {
            AdminDashboard()
        }
    }
}