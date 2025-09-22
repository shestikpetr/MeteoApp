package com.shestikpetr.meteo.ui.screens

import android.util.Log
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.shestikpetr.meteo.data.StationWithLocation
import com.shestikpetr.meteo.ui.map.MapViewModel
import com.shestikpetr.meteo.ui.Parameters
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
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    selectedParameter: Parameters,
    userStations: List<StationWithLocation>,
    latestSensorData: Map<String, Double>,
    isLoadingLatestData: Boolean,
    onChangeMapParameter: (Parameters) -> Unit,
    onCameraZoomChange: (Float) -> Unit,
    navController: NavController,
    onRefreshStations: () -> Unit,
    onLogout: () -> Unit,
    mapViewModel: MapViewModel
) {
    // State for error messages
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Обработка состояния данных
    LaunchedEffect(userStations, isLoadingLatestData) {
        errorMessage = if (userStations.isEmpty() && !isLoadingLatestData) {
            "Не удалось загрузить данные метеостанций."
        } else {
            null
        }
    }

    // Initialize camera position based on available stations
    val initialPosition = remember(userStations) {
        if (userStations.isNotEmpty()) {
            CameraPosition(
                Point(userStations[0].latitude, userStations[0].longitude),
                zoom = 15.0f,
                azimuth = 0.0f,
                tilt = 0.0f
            )
        } else {
            // Default position if no stations available
            CameraPosition(
                Point(56.460337, 84.961591),
                zoom = 15.0f,
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
        onCameraZoomChange(cameraPositionState.position.zoom)
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
                selectedParameter = selectedParameter,
                onChangeParameter = onChangeMapParameter,
                userStations = userStations,
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
                navigationBarsPadding = navigationBarsPadding
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
                // Получаем список станций с кешированными данными для текущего параметра
                val cachedStations = mapViewModel.getCachedStationsForParameter(selectedParameter)

                Log.d("MapScreen", "Кешированные станции для $selectedParameter: $cachedStations")

                // Display stations with clustering based on zoom level
                ClusteredMapView(
                    stations = userStations,
                    zoomLevel = cameraPositionState.position.zoom,
                    selectedParameter = selectedParameter,
                    latestSensorData = latestSensorData,
                    cachedStations = cachedStations,
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
                    Log.d("MapScreen", "Выход из системы")
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

            // Refresh button at top right
            IconButton(
                onClick = { onChangeMapParameter(selectedParameter) },
                modifier = Modifier
                    .padding(end = 16.dp, top = statusBarPadding + 8.dp)
                    .align(Alignment.TopEnd)
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

            // Error message snackbar
            errorMessage?.let { message ->
                Snackbar(
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomCenter),
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
                            Text("OK")
                        }
                    },
                    dismissAction = {
                        IconButton(onClick = { errorMessage = null }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Закрыть"
                            )
                        }
                    }
                ) {
                    Text(message)
                }
            }

            // Loading indicator
            if (isLoadingLatestData) {
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

            errorMessage?.let { message ->
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
                            onClick = { onRefreshStations() },
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
                            Text("Повторить загрузку")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MapBottomSheet(
    bottomSheetState: SheetState,
    selectedParameter: Parameters,
    onChangeParameter: (Parameters) -> Unit,
    userStations: List<StationWithLocation>,
    selectedPoint: StationWithLocation?,
    onStationSelected: (StationWithLocation) -> Unit,
    navigationBarsPadding: Dp
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
                            "Параметр: ${selectedParameter.name}",
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

                    ParametersDropdownMenuMap(
                        selectedParameter = selectedParameter,
                        onChangeParameter = onChangeParameter,
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
                        stations = userStations,
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
                text = selectedStation?.name ?: "Выберите метеостанцию на карте",
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
                text = { Text("Нет доступных метеостанций") },
                onClick = { expanded = false },
                enabled = false
            )
        } else {
            stations.forEach { station ->
                DropdownMenuItem(
                    text = { Text(station.name) },
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
    selectedParameter: Parameters,
    latestSensorData: Map<String, Double>,
    onMarkerClick: (String) -> Unit,
    cachedStations: Set<String> = emptySet()
) {
    // Принудительное обновление - создаем ключ который изменяется при обновлении данных
    val updateKey by remember(latestSensorData, selectedParameter) {
        mutableLongStateOf(System.currentTimeMillis())
    }

    // Добавим отладочные логи
    LaunchedEffect(latestSensorData, updateKey) {
        Log.d("ClusteredMapView", "Данные обновлены (key=$updateKey): $latestSensorData")
        Log.d("ClusteredMapView", "Размер данных: ${latestSensorData.size}")
        Log.d("ClusteredMapView", "Кешированные станции: $cachedStations")
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
        cachedStations,
        updateKey // Добавляем updateKey
    ) {
        Log.d("ClusteredMapView", "Пересчитываем кластеры (updateKey=$updateKey)")
        Log.d("ClusteredMapView", "Текущие данные: $latestSensorData")

        if (clusterThreshold > 0.0) {
            createClusters(
                stations,
                clusterThreshold,
                latestSensorData,
                selectedParameter,
                cachedStations
            )
        } else {
            // Each station as separate cluster
            stations.map { station ->
                val value = latestSensorData[station.stationNumber] ?: 0.0
                val isCached = cachedStations.contains(station.stationNumber)
                Log.d(
                    "ClusteredMapView",
                    "Создаем кластер для ${station.stationNumber} со значением $value (кешировано: $isCached)"
                )
                ClusterInfo(
                    stations = listOf(station),
                    latitude = station.latitude,
                    longitude = station.longitude,
                    averageValue = value,
                    parameter = selectedParameter,
                    isCachedData = isCached
                )
            }
        }
    }

    Log.d("ClusteredMapView", "Отображаем ${clusters.size} кластеров")
    clusters.forEach { cluster ->
        Log.d(
            "ClusteredMapView",
            "Кластер: станции=${cluster.stations.map { it.stationNumber }}, значение=${cluster.averageValue}, кешировано=${cluster.isCachedData}"
        )
    }

    // Display clusters on map - используем updateKey для принудительного обновления
    clusters.forEachIndexed { _, cluster ->
        // Создаем уникальный ключ для каждого маркера включая updateKey
        val markerKey =
            "${cluster.stations.joinToString { it.stationNumber }}_${cluster.averageValue}_${selectedParameter.name}_${cluster.isCachedData}_$updateKey"

        key(markerKey) {
            val placemarkState = rememberPlacemarkState(Point(cluster.latitude, cluster.longitude))

            Placemark(
                state = placemarkState,
                icon = imageProvider(
                    size = DpSize(if (cluster.stations.size > 1) 100.dp else 80.dp, 40.dp)
                ) {
                    ClusterMarker(
                        cluster = cluster,
                        isCachedData = cluster.isCachedData
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
    val parameter: Parameters,
    val isCachedData: Boolean = false // Новое поле для индикации кешированных данных
)

// Create clusters from stations based on proximity
private fun createClusters(
    stations: List<StationWithLocation>,
    threshold: Double,
    latestSensorData: Map<String, Double>,
    parameter: Parameters,
    cachedStations: Set<String>
): List<ClusterInfo> {
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
                Log.d("createClusters", "Станция ${station.stationNumber}: значение $value")
            }
        }
        val avgValue = if (values.isNotEmpty()) values.average() else 0.0

        // Проверяем, есть ли в кластере кешированные данные
        val hasAnyCachedData = cluster.any { station ->
            cachedStations.contains(station.stationNumber)
        }

        Log.d(
            "createClusters",
            "Кластер из ${cluster.size} станций, среднее значение: $avgValue, есть кеш: $hasAnyCachedData"
        )

        ClusterInfo(
            stations = cluster,
            latitude = center.first,
            longitude = center.second,
            averageValue = avgValue,
            parameter = parameter,
            isCachedData = hasAnyCachedData
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
    cluster: ClusterInfo,
    isCachedData: Boolean = false
) {
    // Логируем каждую перерисовку маркера
    Log.d(
        "ClusterMarker",
        "Перерисовка маркера: станции=${cluster.stations.map { it.stationNumber }}, " +
                "значение=${cluster.averageValue}, кешировано=$isCachedData, время=${System.currentTimeMillis()}"
    )

    // Calculate color based on value
    val markerColor = getColorForValue(
        value = cluster.averageValue,
        parameter = cluster.parameter
    )

    // Цвет рамки - если данные кешированы, делаем рамку немного другого оттенка
    val borderColor = if (isCachedData) {
        markerColor.copy(alpha = 0.8f) // Немного прозрачнее для кешированных данных
    } else {
        markerColor
    }

    // Цвет фона - для кешированных данных делаем немного сероватым
    val backgroundColor = if (isCachedData) {
        Color.White.copy(alpha = 0.95f)
    } else {
        Color.White
    }

    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(
                backgroundColor,
                RoundedCornerShape(12.dp)
            )
            .border(
                2.dp,
                borderColor,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Основной текст со значением
            Text(
                text = if (cluster.averageValue > 0.001) {
                    String.format(Locale.getDefault(), "%.1f", cluster.averageValue) +
                            " ${cluster.parameter.getUnit()}"
                } else if (cluster.averageValue < -0.001) {
                    String.format(Locale.getDefault(), "%.1f", cluster.averageValue) +
                            " ${cluster.parameter.getUnit()}"
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
private fun getColorForValue(value: Double, parameter: Parameters): Color {
    // Define color ranges based on parameter type
    return when (parameter) {
        Parameters.TEMPERATURE -> {
            when {
                value < -10.0 -> Color(0xFF1E88E5) // Cold blue
                value < 0.0 -> Color(0xFF42A5F5)   // Blue
                value < 10.0 -> Color(0xFF26C6DA)  // Cyan
                value < 20.0 -> Color(0xFF66BB6A)  // Green
                value < 30.0 -> Color(0xFFFFA726)  // Orange
                else -> Color(0xFFEF5350)          // Red - hot
            }
        }

        Parameters.HUMIDITY -> {
            when {
                value < 20.0 -> Color(0xFFEF5350)  // Red - dry
                value < 40.0 -> Color(0xFFFFA726)  // Orange
                value < 60.0 -> Color(0xFF66BB6A)  // Green
                value < 80.0 -> Color(0xFF26C6DA)  // Cyan
                else -> Color(0xFF1E88E5)          // Blue - humid
            }
        }

        Parameters.PRESSURE -> {
            when {
                value < 980.0 -> Color(0xFF7E57C2) // Purple - low pressure
                value < 1000.0 -> Color(0xFF5C6BC0) // Indigo
                value < 1013.0 -> Color(0xFF42A5F5) // Blue
                value < 1020.0 -> Color(0xFF26A69A) // Teal
                value < 1030.0 -> Color(0xFFFFB74D) // Amber
                else -> Color(0xFFEF5350)          // Red - high pressure
            }
        }
    }
}

@Composable
fun ParametersDropdownMenuMap(
    selectedParameter: Parameters,
    onChangeParameter: (Parameters) -> Unit,
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
                text = when (selectedParameter) {
                    Parameters.TEMPERATURE -> "Температура (°C)"
                    Parameters.HUMIDITY -> "Влажность (%)"
                    Parameters.PRESSURE -> "Давление (гПа)"
                },
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
        Parameters.entries.forEach { parameter ->
            val parameterText = when (parameter) {
                Parameters.TEMPERATURE -> "Температура (°C)"
                Parameters.HUMIDITY -> "Влажность (%)"
                Parameters.PRESSURE -> "Давление (гПа)"
            }

            DropdownMenuItem(
                text = {
                    Text(
                        parameterText,
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

@Composable
private fun Modifier.rotate(degrees: Float): Modifier {
    val rotation = animateFloatAsState(
        targetValue = degrees,
        animationSpec = tween(300),
        label = "rotation"
    )
    return this.graphicsLayer(rotationZ = rotation.value)
}