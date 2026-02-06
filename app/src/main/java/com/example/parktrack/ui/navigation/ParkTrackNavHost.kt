package com.example.parktrack.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.parktrack.ui.screens.BillingScreen
import com.example.parktrack.ui.screens.BillingViewModel
import com.example.parktrack.viewmodel.AuthViewModel




@Composable
fun ParkTrackNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    NavHost(
        navController = navController,
        startDestination = Routes.PROFILE
    ) {
        composable(route = "driver_dashboard") {
            DriverDashboard(
                onLogout = {
                    navController.navigate("login") { popUpTo(0) }
                },
                onViewBilling = {
                    navController.navigate(Routes.BILLING)
                },
                onViewReports = {
                    navController.navigate(Routes.REPORTS)
                }
            )
        }

        // PROFILE SCREEN
        composable(Routes.PROFILE) {
            ProfileScreen(
                authViewModel = authViewModel,
                onBillingClick = {
                    navController.navigate(Routes.BILLING)
                }
            )
        }

        // BILLING SCREEN
        composable(Routes.BILLING) {
            val billingViewModel = BillingViewModel()
            BillingScreen(viewModel = billingViewModel)
        }

        // REPORTS SCREEN
        composable(Routes.REPORTS) {
            ReportsScreen()
        }
    }
}

@Composable
fun DriverDashboard(
    onLogout: () -> Unit,
    onViewBilling: () -> Unit,
    onViewReports: () -> Unit
) {

}


@Composable
fun ReportsScreen() {
    TODO("Not yet implemented")
}

@Composable
fun ProfileScreen(authViewModel: AuthViewModel, onBillingClick: () -> Unit) {

}


object Routes {
    const val PROFILE = "profile"
    const val BILLING = "billing"
    const val REPORTS = "reports"
    const val DRIVER_DASHBOARD = "driver_dashboard"

}

