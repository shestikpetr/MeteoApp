package com.shestikpetr.meteoapp.ui.screens.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.StationAllData
import com.shestikpetr.meteoapp.data.model.StationWithData
import com.shestikpetr.meteoapp.data.model.UserStationResponse
import com.shestikpetr.meteoapp.data.repository.StationDataAggregator
import com.shestikpetr.meteoapp.data.repository.StationRepository
import com.shestikpetr.meteoapp.ui.screens.main.components.BottomSheetContent
import com.shestikpetr.meteoapp.ui.screens.main.components.LoadingContent
import com.shestikpetr.meteoapp.ui.screens.main.components.OsmMapView
import com.shestikpetr.meteoapp.ui.screens.main.components.StationInfoCard
import com.shestikpetr.meteoapp.ui.theme.SkyBlue40
import com.shestikpetr.meteoapp.util.LocationProvider
import com.shestikpetr.meteoapp.util.SettingsManager
import com.shestikpetr.meteoapp.util.TokenStore
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onLogout: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val stationRepository = remember { StationRepository(tokenStore) }
    val stationDataAggregator = remember { StationDataAggregator(stationRepository) }
    val settingsManager = remember { SettingsManager(context) }
    val locationProvider = remember { LocationProvider(context) }
    val scope = rememberCoroutineScope()

    val hiddenStations by settingsManager.hiddenStations.collectAsState(initial = emptySet())
    val hiddenParameters by settingsManager.hiddenParameters.collectAsState(initial = emptySet<Int>())

    var stations by remember { mutableStateOf<List<UserStationResponse>>(emptyList()) }
    var allParameters by remember { mutableStateOf<List<ParameterMetadata>>(emptyList()) }
    var selectedParameter by remember { mutableStateOf<ParameterMetadata?>(null) }
    var stationsWithData by remember { mutableStateOf<List<StationWithData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var selectedStationData by remember { mutableStateOf<StationAllData?>(null) }
    var isLoadingStationData by remember { mutableStateOf(false) }
    var focusedStation by remember { mutableStateOf<StationWithData?>(null) }

    // User location
    var userLocation by remember { mutableStateOf<GeoPoint?>(null) }
    var initialCameraSet by remember { mutableStateOf(false) }

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            scope.launch {
                locationProvider.getLastKnownLocation()?.let { userLocation = it }
            }
        }
    }

    // Request location on start
    LaunchedEffect(Unit) {
        val hasFinePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarsePermission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFinePermission || hasCoarsePermission) {
            locationProvider.getLastKnownLocation()?.let { userLocation = it }
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Load stations and parameters
    LaunchedEffect(Unit) {
        isLoading = true
        val stationsResult = stationRepository.getUserStations()
        stationsResult.getOrNull()?.let { loadedStations ->
            stations = loadedStations
            val paramsResult = stationDataAggregator.getAllParameters(loadedStations)
            paramsResult.getOrNull()?.let { params ->
                allParameters = params
            }
            stationsWithData = stationDataAggregator.getStationsWithData(loadedStations, null)
        }
        isLoading = false
    }

    // Reload data when parameter changes
    LaunchedEffect(selectedParameter) {
        if (stations.isNotEmpty()) {
            isRefreshing = true
            stationsWithData = stationDataAggregator.getStationsWithData(stations, selectedParameter?.code)
            isRefreshing = false
        }
    }

    // Filter by hidden settings
    val visibleStationsWithData = remember(stationsWithData, hiddenStations) {
        stationsWithData.filter { it.stationNumber !in hiddenStations }
    }
    val visibleParameters = remember(allParameters, hiddenParameters) {
        allParameters.filter { it.code !in hiddenParameters }
    }

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 120.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetShadowElevation = 16.dp,
        sheetContent = {
            BottomSheetContent(
                parameters = visibleParameters,
                selectedParameter = selectedParameter,
                onParameterSelected = { param ->
                    selectedParameter = if (selectedParameter?.code == param.code) null else param
                },
                stations = visibleStationsWithData,
                onStationSelected = { station ->
                    focusedStation = station
                    scope.launch {
                        isLoadingStationData = true
                        selectedStationData = stationDataAggregator.getStationAllData(station)
                        isLoadingStationData = false
                    }
                },
                isLoading = isRefreshing
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                LoadingContent()
            } else {
                OsmMapView(
                    modifier = Modifier.fillMaxSize(),
                    stations = visibleStationsWithData,
                    selectedParameter = selectedParameter,
                    userLocation = userLocation,
                    initialCameraSet = initialCameraSet,
                    onInitialCameraSet = { initialCameraSet = true },
                    focusedStation = focusedStation,
                    onFocusHandled = { focusedStation = null },
                    onStationClick = { station ->
                        if (selectedStationData?.stationNumber == station.stationNumber) {
                            selectedStationData = null
                        } else {
                            scope.launch {
                                isLoadingStationData = true
                                selectedStationData = stationDataAggregator.getStationAllData(station)
                                isLoadingStationData = false
                            }
                        }
                    }
                )
            }

            // FAB Column
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Settings FAB
                SmallFloatingActionButton(
                    onClick = { onNavigateToSettings() },
                    containerColor = ComposeColor.White,
                    contentColor = SkyBlue40,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Настройки"
                    )
                }

                // Statistics FAB
                SmallFloatingActionButton(
                    onClick = { onNavigateToStatistics() },
                    containerColor = ComposeColor.White,
                    contentColor = SkyBlue40,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                        contentDescription = "Статистика"
                    )
                }
            }

            // Refresh FAB
            FloatingActionButton(
                onClick = {
                    scope.launch {
                        isRefreshing = true
                        try {
                            // 1. Обновляем список станций пользователя
                            val stationsResult = stationRepository.getUserStations()
                            val loadedStations = stationsResult.getOrNull() ?: return@launch
                            stations = loadedStations

                            // 2. Проходимся по каждой станции и обновляем параметры
                            val paramsResult = stationDataAggregator.getAllParameters(loadedStations)
                            paramsResult.getOrNull()?.let { params ->
                                allParameters = params
                            }

                            // 3. Обновляем данные станций на карте
                            stationsWithData = stationDataAggregator.getStationsWithData(
                                loadedStations,
                                selectedParameter?.code
                            )

                            // 4. Если открыта карточка станции — обновляем и её
                            selectedStationData?.let { currentData ->
                                val updatedStation = stationsWithData.find {
                                    it.stationNumber == currentData.stationNumber
                                }
                                selectedStationData = if (updatedStation != null) {
                                    stationDataAggregator.getStationAllData(updatedStation)
                                } else {
                                    null
                                }
                            }
                        } finally {
                            isRefreshing = false
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 140.dp),
                containerColor = SkyBlue40,
                contentColor = ComposeColor.White,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                val rotation by animateFloatAsState(
                    targetValue = if (isRefreshing) 360f else 0f,
                    label = "rotation"
                )
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Обновить",
                    modifier = Modifier.rotate(rotation)
                )
            }

            // Station info card with all parameters
            AnimatedVisibility(
                visible = selectedStationData != null || isLoadingStationData,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(16.dp)
            ) {
                if (isLoadingStationData) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(12.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = ComposeColor.White)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = SkyBlue40,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                } else {
                    selectedStationData?.let { stationData ->
                        val filteredStationData = remember(stationData, hiddenParameters) {
                            stationData.copy(
                                parameters = stationData.parameters.filter { it.code !in hiddenParameters }
                            )
                        }
                        StationInfoCard(
                            stationData = filteredStationData,
                            onClose = { selectedStationData = null },
                            onParameterClick = { onNavigateToStatistics() }
                        )
                    }
                }
            }
        }
    }
}
