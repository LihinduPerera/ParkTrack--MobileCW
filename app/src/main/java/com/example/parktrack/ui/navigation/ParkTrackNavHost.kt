package com.example.parktrack.ui.navigation

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.parktrack.ui.screens.BillingScreen
import com.example.parktrack.ui.screens.BillingViewModel
import com.example.parktrack.viewmodel.AuthViewModel
import kotlinx.coroutines.launch


@SuppressLint("ViewModelConstructorInComposable")
@Composable
fun ParkTrackNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel
) {
    val scope = rememberCoroutineScope()
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
                },
                onBackClick = { navController.popBackStack() },
                onLogoutClick = { scope.launch { authViewModel.logout() } }
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
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onBillingClick: () -> Unit,
    onBackClick: () -> Boolean,
    onLogoutClick: () -> Unit
) {

}


object Routes {
    const val PROFILE = "profile"
    const val BILLING = "billing"
    const val REPORTS = "reports"
    const val DRIVER_DASHBOARD = "driver_dashboard"

}

