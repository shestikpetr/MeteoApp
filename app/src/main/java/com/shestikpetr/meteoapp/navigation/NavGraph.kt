package com.shestikpetr.meteoapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.shestikpetr.meteoapp.ui.screens.auth.AuthScreen
import com.shestikpetr.meteoapp.ui.screens.main.MainScreen
import com.shestikpetr.meteoapp.ui.screens.settings.SettingsScreen
import com.shestikpetr.meteoapp.ui.screens.statistics.StatisticsScreen
import com.shestikpetr.meteoapp.util.TokenManager

sealed class Screen(val route: String) {
    data object Auth : Screen("auth")
    data object Main : Screen("main")
    data object Statistics : Screen("statistics")
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = if (tokenManager.isLoggedIn()) {
            Screen.Main.route
        } else {
            Screen.Auth.route
        }
    }

    if (startDestination != null) {
        NavHost(
            navController = navController,
            startDestination = startDestination!!
        ) {
            composable(Screen.Auth.route) {
                AuthScreen(
                    onAuthSuccess = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.Auth.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Main.route) {
                MainScreen(
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    },
                    onNavigateToStatistics = {
                        navController.navigate(Screen.Statistics.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.Statistics.route) {
                StatisticsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = {
                        navController.popBackStack()
                    },
                    onLogout = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(Screen.Main.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
