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
import com.shestikpetr.meteo.config.utils.ValidationUtils
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ValidationUtilsViewModel @Inject constructor(
    val validationUtils: ValidationUtils
) : ViewModel()

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
            MapScreen(
                navController = navController,
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

            ChartScreen(
                chartViewModel = chartViewModel,
                selectedParameter = chartUiState.selectedParameter,
                availableParameters = chartUiState.availableParameters,
                selectedDateRange = chartUiState.selectedDateRange,
                onChangeParameter = { parameterConfig ->
                    chartViewModel.selectParameter(parameterConfig)
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
            val validationUtilsViewModel: ValidationUtilsViewModel = hiltViewModel()

            StationManagementScreen(
                validationUtils = validationUtilsViewModel.validationUtils,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}