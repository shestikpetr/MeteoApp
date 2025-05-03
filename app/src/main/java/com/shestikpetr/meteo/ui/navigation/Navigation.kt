package com.shestikpetr.meteo.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shestikpetr.meteo.ui.MeteoViewModel
import com.shestikpetr.meteo.ui.screens.ChartScreen
import com.shestikpetr.meteo.ui.screens.MapScreen
import com.shestikpetr.meteo.ui.login.LoginScreen
import com.shestikpetr.meteo.ui.login.LoginViewModel
import com.shestikpetr.meteo.ui.splash.SplashScreen

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Login : Screen("login")
    data object Map : Screen("map") // Основной экран приложения
    data object Chart : Screen("chart")
}

@Composable
fun MeteoApp(
    viewModel: MeteoViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val mapUiState by viewModel.mapUiState.collectAsState()
    val chartUiState by viewModel.chartUiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route // Начинаем с экрана заставки
    ) {
        composable(Screen.Splash.route) {
            SplashScreen {
                // Проверяем, залогинен ли пользователь
                if (loginViewModel.checkLoggedIn()) {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                } else {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            MapScreen(
                selectedParameter = mapUiState.selectedParameter,
                latestSensorData = mapUiState.latestSensorData,
                isLoadingLatestData = mapUiState.isLoadingLatestData,
                onChangeMapParameter = { parameter ->
                    viewModel.changeMapParameter(parameter)
                },
                navController = navController
            )
        }

        composable(
            route = "${Screen.Chart.route}/{complexId}",
            arguments = listOf(navArgument("complexId") { type = NavType.StringType })
        ) { backStackEntry ->
            val complexId = backStackEntry.arguments?.getString("complexId") ?: return@composable

            ChartScreen(
                viewModel = viewModel,
                selectedChartParameter = chartUiState.selectedParameter,
                selectedDateRange = chartUiState.selectedDateRange,
                onChangeChartParameter = { parameter ->
                    viewModel.changeChartParameter(parameter)
                },
                onChangeDateRange = { dateRange ->
                    viewModel.changeDateRange(dateRange)
                },
                sensorData = chartUiState.sensorData,
                onFetchData = {
                    viewModel.getSensorData(complexId)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}