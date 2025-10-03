package com.shestikpetr.meteo.ui.screens

import com.shestikpetr.meteo.common.logging.MeteoLogger
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.ui.map.MapViewModel
import com.shestikpetr.meteo.ui.map.MapUiState
import com.shestikpetr.meteo.ui.navigation.Screen
import kotlinx.coroutines.launch
import ru.sulgik.mapkit.compose.Placemark
import ru.sulgik.mapkit.compose.YandexMap
import ru.sulgik.mapkit.compose.YandexMapsComposeExperimentalApi
import ru.sulgik.mapkit.compose.imageProvider
import ru.sulgik.mapkit.compose.rememberCameraPositionState
import ru.sulgik.mapkit.compose.rememberPlacemarkState
import ru.sulgik.mapkit.geometry.Point
import ru.sulgik.mapkit.map.CameraPosition
import com.shestikpetr.meteo.localization.compose.stringResource
import com.shestikpetr.meteo.localization.interfaces.StringKey
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    navController: NavController,
    onLogout: () -> Unit,
    mapViewModel: MapViewModel
) {
    val logger = MeteoLogger.forTag("MapScreen")

    // Collect state from ViewModel
    val mapUiState by mapViewModel.uiState.collectAsState()

    // Bottom sheet states
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showAddStationSheet by remember { mutableStateOf(false) }

    // Load data on first composition
    LaunchedEffect(Unit) {
        mapViewModel.loadUserStations()
    }

    // Handle error messages
    LaunchedEffect(mapUiState.userStations, mapUiState.isLoadingLatestData) {
        // Error handling can be managed through mapUiState.errorMessage
    }

    // Initialize camera position based on available stations
    val initialPosition = remember(mapUiState.userStations) {
        if (mapUiState.userStations.isNotEmpty()) {
            CameraPosition(
                Point(mapUiState.userStations[0].latitude, mapUiState.userStations[0].longitude),
                zoom = mapUiState.cameraPosZoom,
                azimuth = 0.0f,
                tilt = 0.0f
            )
        } else {
            // Default position if no stations available
            CameraPosition(
                Point(56.460337, 84.961591),
                zoom = mapUiState.cameraPosZoom,
                azimuth = 0.0f,
                tilt = 0.0f
            )
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = initialPosition
    }

    // Track camera zoom changes
    LaunchedEffect(cameraPositionState.position.zoom) {
        mapViewModel.updateCameraZoom(cameraPositionState.position.zoom)
    }

    var selectedPoint by remember { mutableStateOf<StationWithLocation?>(null) }
    val scope = rememberCoroutineScope()

    // Create drawer state for the bottom sheet
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true
    )

    // Calculate insets for status and navigation bars
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarsPadding =
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // Create scaffold with bottom sheet
    BottomSheetScaffold(
        scaffoldState = BottomSheetScaffoldState(bottomSheetState, SnackbarHostState()),
        sheetPeekHeight = 64.dp + navigationBarsPadding,
        sheetContent = {
            MapBottomSheet(
                bottomSheetState = bottomSheetState,
                mapUiState = mapUiState,
                selectedPoint = selectedPoint,
                onStationSelected = { station ->
                    selectedPoint = station
                    cameraPositionState.position = CameraPosition(
                        Point(station.latitude, station.longitude),
                        zoom = 18f,
                        azimuth = 0f,
                        tilt = 0f
                    )
                    scope.launch {
                        bottomSheetState.partialExpand()
                    }
                },
                navigationBarsPadding = navigationBarsPadding,
                mapViewModel = mapViewModel
            )
        },
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetShadowElevation = 8.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetDragHandle = null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            YandexMap(
                cameraPositionState = cameraPositionState,
                modifier = Modifier.fillMaxSize()
            ) {
                // Display stations with clustering based on zoom level
                ClusteredMapView(
                    stations = mapUiState.userStations,
                    zoomLevel = cameraPositionState.position.zoom,
                    selectedParameter = mapUiState.selectedParameter,
                    latestSensorData = mapUiState.latestSensorData,
                    onMarkerClick = { stationId ->
                        // Navigate to chart screen when station marker is clicked
                        navController.navigate("${Screen.Chart.route}/$stationId")
                    }
                )
            }

            // Status bar overlay for better visibility
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarPadding)
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            IconButton(
                onClick = {
                    logger.d("User logout requested")
                    onLogout()
                },
                modifier = Modifier
                    .padding(start = 16.dp, top = statusBarPadding + 8.dp)
                    .size(40.dp)
                    .shadow(4.dp, CircleShape)
                    .background(
                        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Выйти",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            // Right side buttons column
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 16.dp, top = statusBarPadding + 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings button
                IconButton(
                    onClick = { showSettingsSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Настройки",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Add station button
                IconButton(
                    onClick = { showAddStationSheet = true },
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Добавить станцию",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }

                // Refresh button
                IconButton(
                    onClick = { mapViewModel.forceRefreshData() },
                    modifier = Modifier
                        .size(40.dp)
                        .shadow(4.dp, CircleShape)
                        .background(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Обновить данные",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Loading indicator
            if (mapUiState.isLoadingLatestData) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 100.dp)
                        .shadow(4.dp, RoundedCornerShape(24.dp))
                        .background(
                            MaterialTheme.colorScheme.surface,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Обновление данных...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            mapUiState.errorMessage?.let { message ->
                Card(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.8f)
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            message,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { mapViewModel.forceRefreshData() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Обновить",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(StringKey.RetryLoad))
                        }
                    }
                }
            }
        }
    }

    // Settings bottom sheet
    if (showSettingsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            SettingsBottomSheetContent(
                onDismiss = { showSettingsSheet = false }
            )
        }
    }

    // Add station bottom sheet
    if (showAddStationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddStationSheet = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            AddStationBottomSheetContent(
                onDismiss = { showAddStationSheet = false },
                onAddStation = { stationNumber, customName ->
                    // TODO: Implement station addition logic
                    showAddStationSheet = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapBottomSheet(
    bottomSheetState: SheetState,
    mapUiState: MapUiState,
    selectedPoint: StationWithLocation?,
    onStationSelected: (StationWithLocation) -> Unit,
    navigationBarsPadding: Dp,
    mapViewModel: MapViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = navigationBarsPadding)
            .defaultMinSize(minHeight = 400.dp)
    ) {
        // Sheet handle and title
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Title based on sheet state
                if (bottomSheetState.currentValue == SheetValue.Expanded) {
                    Text(
                        "Настройки карты",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Параметр: ${mapUiState.selectedParameter?.name ?: "Не выбран"}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Content only visible in expanded state
        if (bottomSheetState.currentValue == SheetValue.Expanded) {
            Spacer(modifier = Modifier.height(8.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Parameter selection section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Выбрать параметр",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    DynamicParametersDropdownMenuMap(
                        selectedParameter = mapUiState.selectedParameter,
                        availableParameters = mapUiState.availableParameters,
                        onChangeParameter = { parameterConfig ->
                            mapViewModel.selectParameter(parameterConfig)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Station selection section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Выбрать метеостанцию",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    StationSelector(
                        stations = mapUiState.userStations,
                        selectedStation = selectedPoint,
                        onStationSelected = onStationSelected
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend/info section
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Информация",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "• Нажмите на маркер станции для просмотра графика",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "• При отдалении камеры близкие станции объединяются в кластеры",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        "• Цвет маркера отражает значение выбранного параметра",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun StationSelector(
    stations: List<StationWithLocation>,
    selectedStation: StationWithLocation?,
    onStationSelected: (StationWithLocation) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                MaterialTheme.shapes.medium
            )
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = true }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = selectedStation?.getDisplayName() ?: "Выберите метеостанцию на карте",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (stations.isEmpty()) {
            DropdownMenuItem(
                text = { Text(stringResource(StringKey.NoStationsAvailable)) },
                onClick = { expanded = false },
                enabled = false
            )
        } else {
            stations.forEach { station ->
                DropdownMenuItem(
                    text = { Text(station.getDisplayName()) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Place,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    onClick = {
                        onStationSelected(station)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(YandexMapsComposeExperimentalApi::class)
@Composable
fun ClusteredMapView(
    stations: List<StationWithLocation>,
    zoomLevel: Float,
    selectedParameter: ParameterConfig?,
    latestSensorData: Map<String, Double>,
    onMarkerClick: (String) -> Unit
) {
    val logger = MeteoLogger.forTag("ClusteredMapView")

    // Принудительное обновление - создаем ключ который изменяется при обновлении данных
    val updateKey by remember(latestSensorData, selectedParameter) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    // Добавим отладочные логи
    LaunchedEffect(latestSensorData, updateKey) {
        logger.d("Данные обновлены (key=$updateKey): $latestSensorData")
        logger.d("Размер данных: ${latestSensorData.size}")
        logger.d("Выбранный параметр: ${selectedParameter?.name}")
    }

    // Threshold for clustering based on zoom level
    val clusterThreshold = when {
        zoomLevel < 10f -> 0.05
        zoomLevel < 13f -> 0.02
        else -> 0.0 // No clustering at high zoom levels
    }

    // Group stations into clusters - ВАЖНО: добавляем updateKey для принудительного пересчета
    val clusters = remember(
        stations,
        clusterThreshold,
        latestSensorData,
        selectedParameter,
        updateKey // Добавляем updateKey
    ) {
        logger.d("Пересчитываем кластеры (updateKey=$updateKey)")
        logger.d("Текущие данные: $latestSensorData")

        if (clusterThreshold > 0.0 && selectedParameter != null) {
            createClusters(
                stations,
                clusterThreshold,
                latestSensorData,
                selectedParameter
            )
        } else {
            // Each station as separate cluster
            stations.map { station ->
                val value = latestSensorData[station.stationNumber] ?: 0.0
                logger.d("Создаем кластер для ${station.stationNumber} со значением $value")
                ClusterInfo(
                    stations = listOf(station),
                    latitude = station.latitude,
                    longitude = station.longitude,
                    averageValue = value,
                    parameter = selectedParameter
                )
            }
        }
    }

    logger.d("Отображаем ${clusters.size} кластеров")
    clusters.forEach { cluster ->
        logger.d("Кластер: станции=${cluster.stations.map { it.stationNumber }}, значение=${cluster.averageValue}")
    }

    // Display clusters on map - используем updateKey для принудительного обновления
    clusters.forEachIndexed { _, cluster ->
        // Создаем уникальный ключ для каждого маркера включая updateKey
        val markerKey =
            "${cluster.stations.joinToString { it.stationNumber }}_${cluster.averageValue}_${selectedParameter?.name}_$updateKey"

        key(markerKey) {
            val placemarkState = rememberPlacemarkState(Point(cluster.latitude, cluster.longitude))

            Placemark(
                state = placemarkState,
                icon = imageProvider(
                    size = DpSize(if (cluster.stations.size > 1) 100.dp else 80.dp, 40.dp)
                ) {
                    ClusterMarker(
                        cluster = cluster
                    )
                },
                onTap = {
                    if (cluster.stations.size == 1) {
                        onMarkerClick(cluster.stations.first().stationNumber)
                    }
                    true
                }
            )
        }
    }
}

data class ClusterInfo(
    val stations: List<StationWithLocation>,
    val latitude: Double,
    val longitude: Double,
    val averageValue: Double,
    val parameter: ParameterConfig?
)

// Create clusters from stations based on proximity
private fun createClusters(
    stations: List<StationWithLocation>,
    threshold: Double,
    latestSensorData: Map<String, Double>,
    parameter: ParameterConfig
): List<ClusterInfo> {
    val logger = MeteoLogger.forTag("createClusters")
    val clusters = mutableListOf<MutableList<StationWithLocation>>()

    for (station in stations) {
        var addedToExistingCluster = false

        for (cluster in clusters) {
            val clusterCenter = calculateClusterCenter(cluster)

            if (calculateDistance(
                    station.latitude, station.longitude,
                    clusterCenter.first, clusterCenter.second
                ) <= threshold
            ) {
                cluster.add(station)
                addedToExistingCluster = true
                break
            }
        }

        if (!addedToExistingCluster) {
            clusters.add(mutableListOf(station))
        }
    }

    return clusters.map { cluster ->
        val center = calculateClusterCenter(cluster)
        val values = cluster.mapNotNull { station ->
            latestSensorData[station.stationNumber]?.also { value ->
                logger.d("Станция ${station.stationNumber}: значение $value")
            }
        }
        val avgValue = if (values.isNotEmpty()) values.average() else 0.0

        logger.d("Кластер из ${cluster.size} станций, среднее значение: $avgValue")

        ClusterInfo(
            stations = cluster,
            latitude = center.first,
            longitude = center.second,
            averageValue = avgValue,
            parameter = parameter
        )
    }
}

// Calculate cluster center as average of station coordinates
private fun calculateClusterCenter(stations: List<StationWithLocation>): Pair<Double, Double> {
    val sumLat = stations.sumOf { it.latitude }
    val sumLon = stations.sumOf { it.longitude }
    return Pair(sumLat / stations.size, sumLon / stations.size)
}

// Calculate approximate distance between two points
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return sqrt((lat1 - lat2).pow(2) + (lon1 - lon2).pow(2))
}

@Composable
private fun ClusterMarker(
    cluster: ClusterInfo
) {
    val logger = MeteoLogger.forTag("ClusterMarker")

    // Логируем каждую перерисовку маркера
    logger.d("Перерисовка маркера: станции=${cluster.stations.map { it.stationNumber }}, " +
                "значение=${cluster.averageValue}, время=${System.currentTimeMillis()}")

    // Calculate color based on value
    val markerColor = getColorForValue(
        value = cluster.averageValue,
        parameter = cluster.parameter
    )

    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(
                Color.White,
                RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                markerColor,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Основной текст со значением
            Text(
                text = if (cluster.averageValue > 0.001) {
                    String.format(Locale.getDefault(), "%.1f", cluster.averageValue) +
                            " ${cluster.parameter?.unit ?: ""}"
                } else if (cluster.averageValue < -0.001) {
                    String.format(Locale.getDefault(), "%.1f", cluster.averageValue) +
                            " ${cluster.parameter?.unit ?: ""}"
                } else {
                    "0.0"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            // Счетчик станций для кластеров
            if (cluster.stations.size > 1) {
                Text(
                    text = "(${cluster.stations.size} точек)",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun getColorForValue(value: Double, parameter: ParameterConfig?): Color {
    // Define color ranges based on parameter type
    return when {
        parameter?.name?.lowercase()?.contains("температур") == true ||
        parameter?.code == "4402" || parameter?.code?.lowercase() == "t" -> {
            when {
                value < -10.0 -> Color(0xFF1E88E5) // Cold blue
                value < 0.0 -> Color(0xFF42A5F5)   // Blue
                value < 10.0 -> Color(0xFF26C6DA)  // Cyan
                value < 20.0 -> Color(0xFF66BB6A)  // Green
                value < 30.0 -> Color(0xFFFFA726)  // Orange
                else -> Color(0xFFEF5350)          // Red - hot
            }
        }

        parameter?.name?.lowercase()?.contains("влажность") == true ||
        parameter?.code == "5402" || parameter?.code?.lowercase() == "h" -> {
            when {
                value < 20.0 -> Color(0xFFEF5350)  // Red - dry
                value < 40.0 -> Color(0xFFFFA726)  // Orange
                value < 60.0 -> Color(0xFF66BB6A)  // Green
                value < 80.0 -> Color(0xFF26C6DA)  // Cyan
                else -> Color(0xFF1E88E5)          // Blue - humid
            }
        }

        parameter?.name?.lowercase()?.contains("давление") == true ||
        parameter?.code == "700" || parameter?.code?.lowercase() == "p" -> {
            when {
                value < 980.0 -> Color(0xFF7E57C2) // Purple - low pressure
                value < 1000.0 -> Color(0xFF5C6BC0) // Indigo
                value < 1013.0 -> Color(0xFF42A5F5) // Blue
                value < 1020.0 -> Color(0xFF26A69A) // Teal
                value < 1030.0 -> Color(0xFFFFB74D) // Amber
                else -> Color(0xFFEF5350)          // Red - high pressure
            }
        }

        else -> {
            // Default color scheme for unknown parameters
            when {
                value < 25.0 -> Color(0xFF1E88E5)  // Blue
                value < 50.0 -> Color(0xFF26C6DA)  // Cyan
                value < 75.0 -> Color(0xFF66BB6A)  // Green
                else -> Color(0xFFFFA726)          // Orange
            }
        }
    }
}


@Composable
fun DynamicParametersDropdownMenuMap(
    selectedParameter: ParameterConfig?,
    availableParameters: List<ParameterConfig>,
    onChangeParameter: (ParameterConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surface)
            .clickable { expanded = true }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedParameter?.displayText ?: "Выберите параметр",
                style = MaterialTheme.typography.bodyMedium
            )

            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                modifier = Modifier
                    .size(20.dp)
                    .rotate(270f)
            )
        }
    }

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .background(MaterialTheme.colorScheme.surface)
    ) {
        if (availableParameters.isEmpty()) {
            DropdownMenuItem(
                text = { Text(stringResource(StringKey.ParametersNotAvailable)) },
                onClick = { expanded = false },
                enabled = false
            )
        } else {
            availableParameters.forEach { parameter ->
                DropdownMenuItem(
                    text = {
                        Text(
                            parameter.displayText,
                            fontWeight = if (parameter == selectedParameter) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onChangeParameter(parameter)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = if (parameter == selectedParameter)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun Modifier.rotate(degrees: Float): Modifier {
    val rotation = animateFloatAsState(
        targetValue = degrees,
        animationSpec = tween(300),
        label = "rotation"
    )
    return this.graphicsLayer(rotationZ = rotation.value)
}

@Composable
private fun SettingsBottomSheetContent(
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .defaultMinSize(minHeight = 300.dp)
    ) {
        // Title
        Text(
            "Настройки приложения",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Empty state message
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Настройки в разработке",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Здесь будут отображаться настройки приложения",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Close button
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(StringKey.Close))
        }
    }
}

@Composable
private fun AddStationBottomSheetContent(
    onDismiss: () -> Unit,
    onAddStation: (String, String?) -> Unit
) {
    var stationNumber by remember { mutableStateOf("") }
    var customName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .defaultMinSize(minHeight = 400.dp)
    ) {
        // Title
        Text(
            "Добавить станцию",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Station Number Input
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Номер станции",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = stationNumber,
                    onValueChange = { if (it.length <= 8 && it.all { char -> char.isDigit() }) stationNumber = it },
                    label = { Text(stringResource(StringKey.EightDigitNumber)) },
                    placeholder = { Text("12345678") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    "Название (опционально)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = customName,
                    onValueChange = { customName = it },
                    label = { Text(stringResource(StringKey.UserCustomName)) },
                    placeholder = { Text(stringResource(StringKey.MyStationPlaceholder)) },
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cancel button
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                enabled = !isLoading
            ) {
                Text(stringResource(StringKey.Cancel))
            }

            // Add button
            Button(
                onClick = {
                    if (stationNumber.length == 8) {
                        isLoading = true
                        onAddStation(
                            stationNumber,
                            if (customName.isBlank()) null else customName
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = !isLoading && stationNumber.length == 8
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(stringResource(StringKey.Add))
                }
            }
        }
    }
}