package com.shestikpetr.meteo.ui.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.shestikpetr.meteo.ui.map.MapViewModel
import com.shestikpetr.meteo.ui.chart.ChartViewModel
import com.shestikpetr.meteo.ui.screens.ChartScreen
import com.shestikpetr.meteo.ui.screens.MapScreen
import com.shestikpetr.meteo.ui.login.LoginScreen
import com.shestikpetr.meteo.ui.login.LoginViewModel
import com.shestikpetr.meteo.ui.stations.StationManagementScreen
import com.shestikpetr.meteo.ui.Parameters

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Map : Screen("map")
    data object Chart : Screen("chart")
    data object StationManagement : Screen("station_management")
}

@Composable
fun MeteoApp(
    mapViewModel: MapViewModel = hiltViewModel(),
    chartViewModel: ChartViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(),
    navController: NavHostController = rememberNavController()
) {
    val mapUiState by mapViewModel.uiState.collectAsState()
    val chartUiState by chartViewModel.uiState.collectAsState()

    // Определяем стартовый экран на основе статуса авторизации
    var startDestination by remember { mutableStateOf(Screen.Login.route) }

    // Проверяем статус авторизации асинхронно
    LaunchedEffect(Unit) {
        loginViewModel.checkLoggedIn { isLoggedIn ->
            startDestination = if (isLoggedIn) Screen.Map.route else Screen.Login.route
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // После входа загружаем станции пользователя
                    mapViewModel.loadUserStations()
                    navController.navigate(Screen.Map.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Map.route) {
            // Проверяем, загружены ли станции, если нет - загружаем
            LaunchedEffect(Unit) {
                if (mapUiState.userStations.isEmpty() && !mapUiState.isLoadingLatestData) {
                    mapViewModel.loadUserStations()
                }
            }

            // Convert ParameterConfig to legacy Parameters enum for UI compatibility
            val legacySelectedParameter = mapUiState.selectedParameter?.let { paramConfig ->
                when {
                    paramConfig.name.lowercase().contains("температур") ||
                    paramConfig.code.lowercase() == "t" ||
                    paramConfig.code == "4402" -> Parameters.TEMPERATURE

                    paramConfig.name.lowercase().contains("влажность") ||
                    paramConfig.code.lowercase() == "h" ||
                    paramConfig.code == "5402" -> Parameters.HUMIDITY

                    paramConfig.name.lowercase().contains("давление") ||
                    paramConfig.code.lowercase() == "p" ||
                    paramConfig.code == "700" -> Parameters.PRESSURE

                    else -> Parameters.TEMPERATURE // Default fallback
                }
            } ?: Parameters.TEMPERATURE

            MapScreen(
                selectedParameter = legacySelectedParameter,
                userStations = mapUiState.userStations,
                latestSensorData = mapUiState.latestSensorData,
                isLoadingLatestData = mapUiState.isLoadingLatestData,
                onChangeMapParameter = { parameter ->
                    mapViewModel.changeMapParameter(parameter)
                },
                onCameraZoomChange = { zoom ->
                    mapViewModel.updateCameraZoom(zoom)
                },
                navController = navController,
                onRefreshStations = {
                    mapViewModel.forceRefreshData()
                },
                onLogout = {
                    // Выполняем logout
                    loginViewModel.logout()

                    // Очищаем данные в ViewModels
                    mapViewModel.clearData()
                    chartViewModel.clearData()

                    // Переходим на экран логина
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Map.route) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                mapViewModel = mapViewModel
            )
        }

        composable(
            route = "${Screen.Chart.route}/{stationNumber}",
            arguments = listOf(navArgument("stationNumber") { type = NavType.StringType })
        ) { backStackEntry ->
            val stationNumber = backStackEntry.arguments?.getString("stationNumber") ?: return@composable

            // Load available parameters for this station
            LaunchedEffect(stationNumber) {
                chartViewModel.loadAvailableParameters(stationNumber)
            }

            // Convert ParameterConfig to legacy Parameters enum for UI compatibility
            val legacyChartParameter = chartUiState.selectedParameter?.let { paramConfig ->
                when {
                    paramConfig.name.lowercase().contains("температур") ||
                    paramConfig.code.lowercase() == "t" ||
                    paramConfig.code == "4402" -> Parameters.TEMPERATURE

                    paramConfig.name.lowercase().contains("влажность") ||
                    paramConfig.code.lowercase() == "h" ||
                    paramConfig.code == "5402" -> Parameters.HUMIDITY

                    paramConfig.name.lowercase().contains("давление") ||
                    paramConfig.code.lowercase() == "p" ||
                    paramConfig.code == "700" -> Parameters.PRESSURE

                    else -> Parameters.TEMPERATURE // Default fallback
                }
            } ?: Parameters.TEMPERATURE

            ChartScreen(
                chartViewModel = chartViewModel,
                selectedChartParameter = legacyChartParameter,
                selectedDateRange = chartUiState.selectedDateRange,
                onChangeChartParameter = { parameter ->
                    chartViewModel.changeChartParameter(parameter)
                },
                onChangeDateRange = { dateRange ->
                    chartViewModel.changeDateRange(dateRange)
                },
                sensorData = chartUiState.sensorData,
                onFetchData = {
                    chartViewModel.getSensorData(stationNumber)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        composable(Screen.StationManagement.route) {
            StationManagementScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}