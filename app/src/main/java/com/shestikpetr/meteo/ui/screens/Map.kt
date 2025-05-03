package com.shestikpetr.meteo.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.shestikpetr.meteo.ui.Parameters
import com.shestikpetr.meteo.data.StationWithLocation
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

@OptIn(YandexMapsComposeExperimentalApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    selectedParameter: Parameters,
    userStations: List<StationWithLocation>,
    latestSensorData: Map<String, Double>,
    isLoadingLatestData: Boolean,
    onChangeMapParameter: (Parameters) -> Unit,
    onCameraZoomChange: (Float) -> Unit,
    navController: NavController
) {
    // Настраиваем начальную позицию камеры
    val initialPosition = if (userStations.isNotEmpty()) {
        CameraPosition(
            Point(userStations[0].latitude, userStations[0].longitude),
            zoom = 15.0f,
            azimuth = 0.0f,
            tilt = 0.0f
        )
    } else {
        CameraPosition(
            Point(56.460337, 84.961591), // Дефолтная позиция
            zoom = 15.0f,
            azimuth = 0.0f,
            tilt = 0.0f
        )
    }

    val cameraPositionState = rememberCameraPositionState {
        position = initialPosition
    }

    // Отслеживаем изменение зума камеры
    LaunchedEffect(cameraPositionState.position.zoom) {
        onCameraZoomChange(cameraPositionState.position.zoom)
    }

    var selectedPoint by remember { mutableStateOf<StationWithLocation?>(null) }
    val scope = rememberCoroutineScope()

    // Состояние для BottomSheet с начальным значением PartiallyExpanded
    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true // Предотвращаем полное скрытие меню
    )

    // Учитываем высоту статус-бара
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navigationBarsPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BottomSheetScaffold(
        scaffoldState = BottomSheetScaffoldState(bottomSheetState, SnackbarHostState()),
        sheetPeekHeight = 64.dp + navigationBarsPadding, // Высота видимой части меню в свернутом состоянии
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        bottom = navigationBarsPadding
                    )
                    .defaultMinSize(minHeight = 400.dp)
            ) {
                // Заголовок с полосой для перетаскивания
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Полоса для перетаскивания
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Заголовок виден только в развернутом состоянии
                        if (bottomSheetState.currentValue == SheetValue.Expanded) {
                            Text(
                                "Настройки карты",
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            // В свернутом состоянии показываем только текущий параметр
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

                // Дальнейшее содержимое видно только в развернутом состоянии
                if (bottomSheetState.currentValue == SheetValue.Expanded) {
                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Секция параметров
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
                                onChangeParameter = onChangeMapParameter,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Секция выбора точки
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
                                "Выбрать точку",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )

                            Spacer(modifier = Modifier.height(8.dp))

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
                                        text = selectedPoint?.name ?: "Выберите точку на карте",
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
                                userStations.forEach { station ->
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
                                            selectedPoint = station
                                            expanded = false
                                            cameraPositionState.position =
                                                CameraPosition(
                                                    Point(station.latitude, station.longitude),
                                                    zoom = 18f,
                                                    azimuth = 0f,
                                                    tilt = 0f
                                                )
                                            scope.launch {
                                                bottomSheetState.partialExpand()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        },
        sheetShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        sheetShadowElevation = 8.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetDragHandle = null // Мы добавили свой собственный дизайн для drag handle
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Карта с кластеризацией точек
            YandexMap(
                cameraPositionState = cameraPositionState,
                modifier = Modifier.fillMaxSize()
            ) {
                ClusteredMapView(
                    stations = userStations,
                    zoomLevel = cameraPositionState.position.zoom,
                    selectedParameter = selectedParameter,
                    latestSensorData = latestSensorData
                ) { stationId ->
                    navController.navigate("${Screen.Chart.route}/$stationId")
                }
            }

            // Тень для статус-бара
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarPadding)
                    .background(Color.Black.copy(alpha = 0.3f))
            )

            // Индикатор загрузки
            if (isLoadingLatestData) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp) // Выше нижнего меню
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
        }
    }
}

@Composable
fun ClusteredMapView(
    stations: List<StationWithLocation>,
    zoomLevel: Float,
    selectedParameter: Parameters,
    latestSensorData: Map<String, Double>,
    onMarkerClick: (String) -> Unit
) {
    // Порог расстояния для объединения точек, зависящий от зума
    val clusterThreshold = when {
        zoomLevel < 10f -> 0.05 // большое расстояние при малом зуме
        zoomLevel < 13f -> 0.02 // среднее расстояние
        else -> 0.0 // без кластеризации при большом зуме
    }

    // Группируем станции в кластеры
    val clusters = if (clusterThreshold > 0.0) {
        createClusters(stations, clusterThreshold, latestSensorData, selectedParameter)
    } else {
        // Каждая станция как отдельный кластер
        stations.map { station ->
            ClusterInfo(
                stations = listOf(station),
                latitude = station.latitude,
                longitude = station.longitude,
                averageValue = latestSensorData[station.stationNumber] ?: 0.0,
                parameter = selectedParameter
            )
        }
    }

    // Отображаем кластеры на карте
    clusters.forEach { cluster ->
        val placemarkState = rememberPlacemarkState(Point(cluster.latitude, cluster.longitude))

        Placemark(
            state = placemarkState,
            icon = imageProvider(
                size = DpSize(if (cluster.stations.size > 1) 100.dp else 80.dp, 40.dp)
            ) {
                ClusterMarker(cluster)
            },
            onTap = {
                if (cluster.stations.size == 1) {
                    onMarkerClick(cluster.stations.first().stationNumber)
                } else {
                    // При нажатии на кластер - зуммируем карту для разделения кластера
                }
                true
            }
        )
    }
}

data class ClusterInfo(
    val stations: List<StationWithLocation>,
    val latitude: Double,
    val longitude: Double,
    val averageValue: Double,
    val parameter: Parameters
)

// Функция для кластеризации точек
private fun createClusters(
    stations: List<StationWithLocation>,
    threshold: Double,
    latestSensorData: Map<String, Double>,
    parameter: Parameters
): List<ClusterInfo> {
    val clusters = mutableListOf<MutableList<StationWithLocation>>()

    for (station in stations) {
        var addedToExistingCluster = false

        for (cluster in clusters) {
            val clusterCenter = calculateClusterCenter(cluster)

            if (calculateDistance(
                    station.latitude, station.longitude,
                    clusterCenter.first, clusterCenter.second
                ) <= threshold) {
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
        val avgValue = cluster
            .mapNotNull { latestSensorData[it.stationNumber] }
            .takeIf { it.isNotEmpty() }
            ?.average() ?: 0.0

        ClusterInfo(
            stations = cluster,
            latitude = center.first,
            longitude = center.second,
            averageValue = avgValue,
            parameter = parameter
        )
    }
}

// Рассчитать центр кластера
private fun calculateClusterCenter(stations: List<StationWithLocation>): Pair<Double, Double> {
    val sumLat = stations.sumOf { it.latitude }
    val sumLon = stations.sumOf { it.longitude }
    return Pair(sumLat / stations.size, sumLon / stations.size)
}

// Рассчитать расстояние между точками (приближенно)
private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    return sqrt((lat1 - lat2).pow(2) + (lon1 - lon2).pow(2))
}

@Composable
private fun ClusterMarker(cluster: ClusterInfo) {
    Box(
        modifier = Modifier
            .shadow(4.dp, RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surfaceVariant,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.dp,
                MaterialTheme.colorScheme.outline,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .animateContentSize()
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = String.format(Locale.getDefault(), "%.1f", cluster.averageValue) +
                        " ${cluster.parameter.getUnit()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (cluster.stations.size > 1) {
                Text(
                    text = "(${cluster.stations.size} точек)",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
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
                text = selectedParameter.name,
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
        modifier = Modifier.fillMaxWidth(0.9f)
    ) {
        Parameters.entries.forEach { parameter ->
            DropdownMenuItem(
                text = {
                    Text(
                        parameter.name,
                        fontWeight = if (parameter == selectedParameter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = {
                    onChangeParameter(parameter)
                    expanded = false
                },
                colors = MenuDefaults.itemColors(
                    textColor = if (parameter == selectedParameter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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