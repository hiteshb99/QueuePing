package com.university.queueping.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.university.queueping.ui.screens.RoleSelectionScreen
import com.university.queueping.ui.screens.customer.CustomerHomeScreen
import com.university.queueping.ui.screens.customer.QRScannerScreen
import com.university.queueping.ui.screens.customer.QueueConfirmationScreen
import com.university.queueping.ui.screens.customer.QueueStatusScreen
import com.university.queueping.ui.screens.customer.YourTurnScreen
import com.university.queueping.ui.screens.staff.CreateQueueScreen
import com.university.queueping.ui.screens.staff.QueueDashboardScreen
import com.university.queueping.ui.screens.staff.QueueQRScreen
import com.university.queueping.ui.screens.staff.StaffHomeScreen
import com.university.queueping.ui.screens.staff.StaffLoginScreen

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.RoleSelection.route
    ) {
        composable(Screen.RoleSelection.route) {
            RoleSelectionScreen(
                onNavigateToCustomer = { navController.navigate(Screen.CustomerHome.route) },
                onNavigateToStaff = { navController.navigate(Screen.StaffLogin.route) }
            )
        }

        composable(Screen.StaffLogin.route) {
            StaffLoginScreen(
                onLoginSuccess = { 
                    navController.navigate(Screen.StaffHome.route) {
                        popUpTo(Screen.RoleSelection.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.CustomerHome.route) {
            CustomerHomeScreen(
                onNavigateToScan = { navController.navigate(Screen.QRScanner.route) },
                onNavigateToQueueStatus = { queueId ->
                    navController.navigate(Screen.QueueStatus.createRoute(queueId))
                },
                onNavigateToConfirmation = { queueId ->
                    navController.navigate(Screen.QueueConfirmation.createRoute(queueId))
                },
                onSwitchRole = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.QRScanner.route) {
            QRScannerScreen(
                onScanSuccess = { scannedQueueId ->
                    navController.navigate(Screen.QueueConfirmation.createRoute(scannedQueueId)) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QueueConfirmation.route,
            arguments = listOf(navArgument("queueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val queueId = backStackEntry.arguments?.getString("queueId") ?: "queue_001"
            QueueConfirmationScreen(
                queueId = queueId,
                onJoinSuccess = { id ->
                    navController.navigate(Screen.QueueStatus.createRoute(id)) {
                        popUpTo(Screen.CustomerHome.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QueueStatus.route,
            arguments = listOf(navArgument("queueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val queueId = backStackEntry.arguments?.getString("queueId") ?: "queue_001"
            QueueStatusScreen(
                queueId = queueId,
                onNavigateToCalled = { id ->
                    navController.navigate(Screen.YourTurn.createRoute(id))
                },
                onNavigateHome = {
                    navController.navigate(Screen.CustomerHome.route) {
                        popUpTo(Screen.CustomerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Screen.YourTurn.route,
            arguments = listOf(navArgument("queueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val queueId = backStackEntry.arguments?.getString("queueId") ?: "queue_001"
            YourTurnScreen(
                queueId = queueId,
                onFinished = {
                    navController.navigate(Screen.CustomerHome.route) {
                        popUpTo(Screen.CustomerHome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.StaffHome.route) {
            StaffHomeScreen(
                onNavigateToCreateQueue = { navController.navigate(Screen.CreateQueue.route) },
                onNavigateToQueueDashboard = { queueId ->
                    navController.navigate(Screen.QueueDashboard.createRoute(queueId))
                },
                onNavigateToQueueQR = { queueId ->
                    navController.navigate(Screen.QueueQR.createRoute(queueId))
                },
                onSwitchRole = {
                    navController.navigate(Screen.RoleSelection.route) {
                        popUpTo(Screen.RoleSelection.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.CreateQueue.route) {
            CreateQueueScreen(
                onQueueCreated = { newQueueId ->
                    navController.navigate(Screen.QueueQR.createRoute(newQueueId)) {
                        popUpTo(Screen.StaffHome.route)
                    }
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QueueDashboard.route,
            arguments = listOf(navArgument("queueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val queueId = backStackEntry.arguments?.getString("queueId") ?: "queue_001"
            QueueDashboardScreen(
                queueId = queueId,
                onNavigateToQR = { id ->
                    navController.navigate(Screen.QueueQR.createRoute(id))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.QueueQR.route,
            arguments = listOf(navArgument("queueId") { type = NavType.StringType })
        ) { backStackEntry ->
            val queueId = backStackEntry.arguments?.getString("queueId") ?: "queue_001"
            QueueQRScreen(
                queueId = queueId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
