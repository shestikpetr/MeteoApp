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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.StationAllData
import com.shestikpetr.meteoapp.data.model.StationWithData
import com.shestikpetr.meteoapp.data.model.UserStationResponse
import com.shestikpetr.meteoapp.data.repository.StationDataAggregator
import com.shestikpetr.meteoapp.data.repository.StationRepository
import com.shestikpetr.meteoapp.ui.map.MapMarkerRenderer
import com.shestikpetr.meteoapp.ui.theme.SkyBlue40
import com.shestikpetr.meteoapp.ui.theme.SkyBlue80
import com.shestikpetr.meteoapp.ui.theme.SkyBlueDark
import com.shestikpetr.meteoapp.ui.util.formatParameterValue
import com.shestikpetr.meteoapp.ui.util.getParameterIcon
import com.shestikpetr.meteoapp.util.LocationProvider
import com.shestikpetr.meteoapp.util.SettingsManager
import com.shestikpetr.meteoapp.util.TokenManager
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
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
    val tokenManager = remember { TokenManager(context) }
    val stationRepository = remember { StationRepository(tokenManager) }
    val stationDataAggregator = remember { StationDataAggregator(stationRepository) }
    val settingsManager = remember { SettingsManager(context) }
    val locationProvider = remember { LocationProvider(context) }
    val scope = rememberCoroutineScope()

    val hiddenStations by settingsManager.hiddenStations.collectAsState(initial = emptySet())
    val hiddenParameters by settingsManager.hiddenParameters.collectAsState(initial = emptySet())

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

@Composable
private fun LoadingContent() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = SkyBlue40,
            strokeWidth = 4.dp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Загрузка станций...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StationInfoCard(
    stationData: StationAllData,
    onClose: () -> Unit,
    onParameterClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ComposeColor.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = SkyBlue40.copy(alpha = 0.1f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = null,
                            tint = SkyBlue40,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stationData.customName ?: stationData.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Станция ${stationData.stationNumber}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                SmallFloatingActionButton(
                    onClick = onClose,
                    containerColor = ComposeColor.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    elevation = FloatingActionButtonDefaults.elevation(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть"
                    )
                }
            }

            if (stationData.parameters.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))

                // Parameters list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(stationData.parameters) { param ->
                        ParameterValueItem(
                            param = param,
                            onClick = onParameterClick
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Нет данных",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ParameterValueItem(
    param: com.shestikpetr.meteoapp.data.model.StationParameterValue,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(10.dp),
        color = SkyBlue80.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = getParameterIcon(param.code),
                    contentDescription = null,
                    tint = SkyBlue40,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = param.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${param.value.formatParameterValue()} ${param.unit ?: ""}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SkyBlueDark
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = "Статистика",
                    tint = SkyBlue40.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun OsmMapView(
    modifier: Modifier = Modifier,
    stations: List<StationWithData>,
    selectedParameter: ParameterMetadata?,
    userLocation: GeoPoint?,
    initialCameraSet: Boolean,
    onInitialCameraSet: () -> Unit,
    focusedStation: StationWithData?,
    onFocusHandled: () -> Unit,
    onStationClick: (StationWithData) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

    val mapView = remember {
        MapView(context).apply {
            setMultiTouchControls(true)
            setBuiltInZoomControls(false)
        }
    }
    val markerRenderer = remember { MapMarkerRenderer(context) }

    // Set initial camera position only once
    LaunchedEffect(userLocation, stations) {
        if (!initialCameraSet && (userLocation != null || stations.isNotEmpty())) {
            val targetPoint = userLocation ?: stations.firstOrNull()?.let {
                GeoPoint(it.latitude, it.longitude)
            }
            targetPoint?.let {
                mapView.controller.setZoom(10.0)
                mapView.controller.setCenter(it)
                onInitialCameraSet()
            }
        }
    }

    // Focus on selected station
    LaunchedEffect(focusedStation) {
        focusedStation?.let { station ->
            mapView.controller.animateTo(
                GeoPoint(station.latitude, station.longitude),
                14.0,
                1000L
            )
            onFocusHandled()
        }
    }

    // Update markers when stations or parameter changes (without moving camera)
    LaunchedEffect(stations, selectedParameter) {
        mapView.overlays.clear()

        stations.forEach { station ->
            val point = GeoPoint(station.latitude, station.longitude)

            val hasData = station.parameterValue != null || selectedParameter == null

            val displayText = if (selectedParameter != null) {
                val value = station.parameterValue?.formatParameterValue() ?: "—"
                if (station.unit != null && station.parameterValue != null) {
                    "$value ${station.unit}"
                } else {
                    value
                }
            } else {
                station.customName ?: station.name
            }

            val marker = Marker(mapView).apply {
                position = point
                icon = markerRenderer.render(displayText, hasData)
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                setOnMarkerClickListener { _, _ ->
                    onStationClick(station)
                    true
                }
            }
            mapView.overlays.add(marker)
        }
        mapView.invalidate()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onDetach()
        }
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BottomSheetContent(
    parameters: List<ParameterMetadata>,
    selectedParameter: ParameterMetadata?,
    onParameterSelected: (ParameterMetadata) -> Unit,
    stations: List<StationWithData>,
    onStationSelected: (StationWithData) -> Unit,
    isLoading: Boolean
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Параметры", "Станции")

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // Tabs
        PrimaryTabRow(
            selectedTabIndex = selectedTab,
            containerColor = ComposeColor.Transparent,
            contentColor = SkyBlue40
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (index == 0) Icons.Default.FilterList else Icons.Default.Place,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Normal
                            )
                            if (index == 0 && isLoading) {
                                Spacer(modifier = Modifier.width(8.dp))
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                    color = SkyBlue40
                                )
                            }
                        }
                    }
                )
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            when (selectedTab) {
                0 -> {
                    // Parameters tab
                    Text(
                        text = if (selectedParameter != null) "Выбран: ${selectedParameter.name}" else "Выберите параметр для карты",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedParameter != null) SkyBlue40 else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        items(parameters) { param ->
                            ParameterItem(
                                parameter = param,
                                isSelected = selectedParameter?.code == param.code,
                                onClick = { onParameterSelected(param) }
                            )
                        }
                    }
                }
                1 -> {
                    // Stations tab
                    Text(
                        text = "${stations.size} станций",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        items(stations) { station ->
                            StationItem(
                                station = station,
                                onClick = { onStationSelected(station) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StationItem(
    station: StationWithData,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = ComposeColor.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = SkyBlue80.copy(alpha = 0.3f)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SkyBlue40,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = station.customName ?: station.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "№ ${station.stationNumber}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (station.parameterValue != null) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SkyBlue40
                ) {
                    Text(
                        text = "${station.parameterValue.formatParameterValue()} ${station.unit ?: ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = ComposeColor.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParameterItem(
    parameter: ParameterMetadata,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() },
        color = if (isSelected) SkyBlue40.copy(alpha = 0.15f) else ComposeColor.Transparent,
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape,
                    color = if (isSelected) SkyBlue40 else SkyBlue80.copy(alpha = 0.3f)
                ) {
                    Icon(
                        imageVector = getParameterIcon(parameter.code),
                        contentDescription = null,
                        tint = if (isSelected) ComposeColor.White else SkyBlue40,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = parameter.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    parameter.unit?.let { unit ->
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isSelected) {
                Surface(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape,
                    color = SkyBlue40
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Выбрано",
                        tint = ComposeColor.White,
                        modifier = Modifier
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }
            }
        }
    }
}

