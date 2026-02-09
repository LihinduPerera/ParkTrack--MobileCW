package com.example.parktrack.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.parktrack.ui.admin.AdminDashboard
import com.example.parktrack.ui.admin.QRScannerScreen
import com.example.parktrack.ui.admin.SecurityProfileScreen
import com.example.parktrack.ui.auth.LoginScreen
import com.example.parktrack.ui.auth.RegisterScreen
import com.example.parktrack.ui.driver.DriverDashboard
import com.example.parktrack.ui.onboarding.OnboardingScreen
import com.example.parktrack.ui.screens.BillingScreen
import com.example.parktrack.ui.screens.ReportsScreen
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
    object Profile : Screen("profile")
    object SecurityProfile : Screen("security_profile")
    object Onboarding : Screen("onboarding")
    object ChangePassword : Screen("change_password")
    object PersonalInfo : Screen("personal_info")
    object AccountSettings : Screen("account_settings")
    object UpdateEmail : Screen("update_email")
    object Preferences : Screen("preferences")
    object VehicleManagement : Screen("vehicle_management")
    object ParkingLotMap : Screen("parking_lot_map")
    object AddParkingLot : Screen("add_parking_lot")
    object ParkingLotManagement : Screen("parking_lot_management")
}


@Composable
fun ParkTrackNavHost(
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val authState by authViewModel.authState.collectAsState()
    val isCheckingAuth by authViewModel.isCheckingAuth.collectAsState()

    // 1. SharedPreferences to track if they've finished current onboarding session
    val sharedPref = remember {
        context.getSharedPreferences("parktrack_prefs", android.content.Context.MODE_PRIVATE)
    }

    // determine start destination based on whether they are authenticated or not.
    // If not authenticated,  always start at Onboarding.
    val startDestination = if (authState is AuthState.Authenticated) {
        // This part is  handled by the LaunchedEffect below,
        Screen.Login.route
    } else {
        Screen.Onboarding.route
    }


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
                // navigate to onboarding and then login
                if (currentRoute != Screen.Login.route &&
                    currentRoute != Screen.Register.route &&
                    currentRoute != Screen.Onboarding.route &&
                    !isCheckingAuth) {
                    navController.navigate(Screen.Onboarding.route) {
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
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                // When finished, go to Login
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }
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
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                },
                onNavigateToVehicles = {
                    navController.navigate(Screen.VehicleManagement.route)
                },
                onNavigateToParkingLots = {
                    navController.navigate(Screen.ParkingLotMap.route)
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
                },
                onNavigateToProfile = { navController.navigate(Screen.SecurityProfile.route) },
                onNavigateToReports = { navController.navigate(Screen.Reports.route) },
                onAddParkingLot = { navController.navigate(Screen.ParkingLotManagement.route) }
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
            BillingScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Profile.route) {
            com.example.parktrack.ui.driver.ProfileScreen(
                authViewModel = authViewModel,
                onBillingClick = { navController.navigate(Screen.Billing.route) },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = { scope.launch { authViewModel.logout() } },
                onChangePasswordClick = { navController.navigate(Screen.ChangePassword.route) },
                onPersonalInfoClick = { navController.navigate(Screen.PersonalInfo.route) },
                onPreferencesClick = { navController.navigate(Screen.Preferences.route) }

            )
        }

        composable(Screen.ChangePassword.route) {
            // Replace with your actual ChangePasswordScreen composable
            com.example.parktrack.ui.driver.ChangePasswordScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.SecurityProfile.route) {
            SecurityProfileScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = { scope.launch { authViewModel.logout() } },
                onAccountSettingsClick = { navController.navigate(Screen.AccountSettings.route) },
                onPreferencesClick = { navController.navigate(Screen.Preferences.route) }
            )
        }

        composable(Screen.PersonalInfo.route) {
            com.example.parktrack.ui.driver.PersonalInfoScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.AccountSettings.route) {
            com.example.parktrack.ui.admin.AccountSettingsScreen(
                onBackClick = { navController.popBackStack() },
                onChangePasswordClick = { navController.navigate(Screen.ChangePassword.route) },
                onUpdateEmailClick = { navController.navigate(Screen.UpdateEmail.route) }
            )
        }

        composable(Screen.UpdateEmail.route) {
            com.example.parktrack.ui.admin.UpdateEmailScreen(
                authViewModel = authViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Preferences.route) {
            com.example.parktrack.ui.admin.PreferencesScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.VehicleManagement.route) {
            val userId = (authState as? AuthState.Authenticated)?.user?.id ?: ""
            com.example.parktrack.ui.driver.VehicleManagementScreen(
                driverId = userId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ParkingLotMap.route) {
            com.example.parktrack.ui.screens.ParkingLotMapScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Reports.route) {
            ReportsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.ParkingLotManagement.route) {
            com.example.parktrack.ui.screens.ParkingLotManagementScreen(
                onBackClick = { navController.popBackStack() },
                onParkingLotOperationComplete = { 
                    // Navigate back to admin dashboard after successful operation
                    navController.popBackStack()
                }
            )
        }
    }
}