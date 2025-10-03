package com.shestikpetr.meteo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.Icons.AutoMirrored
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteo.network.SensorDataPoint
import com.shestikpetr.meteo.ui.chart.ChartViewModel
import com.shestikpetr.meteo.model.ParameterConfig
import com.shestikpetr.meteo.localization.compose.stringResource
import com.shestikpetr.meteo.localization.interfaces.StringKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ChartScreen(
    chartViewModel: ChartViewModel,
    selectedParameter: ParameterConfig?,
    availableParameters: List<ParameterConfig>,
    selectedDateRange: Pair<Long?, Long?>,
    onChangeParameter: (ParameterConfig) -> Unit,
    onChangeDateRange: (Pair<Long?, Long?>) -> Unit,
    sensorData: List<SensorDataPoint>,
    onFetchData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChartVisible by remember { mutableStateOf(false) }
    val isDataLoading = chartViewModel.uiState.collectAsState().value.isLoadingSensorData
    var isRefreshing by remember { mutableStateOf(false) }
    var isSettingsExpanded by remember { mutableStateOf(true) }

    // Автоматически загружать данные, когда выбран диапазон дат и параметр
    LaunchedEffect(selectedDateRange, selectedParameter) {
        if (selectedDateRange.first != null && selectedDateRange.second != null && selectedParameter != null) {
            if (isChartVisible) {
                onFetchData()
            }
        }
    }

    LaunchedEffect(isDataLoading) {
        if (!isDataLoading) {
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(StringKey.WeatherStationTitle),
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                actions = {
                    if (isChartVisible && sensorData.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                onFetchData()
                            },
                            enabled = !isDataLoading
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Обновить данные"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (isChartVisible && !isDataLoading) {
                    isRefreshing = true
                    onFetchData()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = rememberPullToRefreshState()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Settings Card
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Header with collapse button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    stringResource(StringKey.ChartSettings),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                if (!isSettingsExpanded && selectedParameter != null) {
                                    Text(
                                        text = selectedParameter.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { isSettingsExpanded = !isSettingsExpanded }) {
                                Icon(
                                    imageVector = if (isSettingsExpanded)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isSettingsExpanded) "Свернуть" else "Развернуть"
                                )
                            }
                        }

                        // Expandable content
                        AnimatedVisibility(visible = isSettingsExpanded) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Parameter Selection
                                SensorSelectionSection(
                                    selectedParameter = selectedParameter,
                                    availableParameters = availableParameters,
                                    onChangeParameter = onChangeParameter
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Quick Date Range Presets
                                QuickDateRangeSelector(
                                    onSelectRange = { startTime, endTime ->
                                        onChangeDateRange(Pair(startTime, endTime))
                                    },
                                    selectedDateRange = selectedDateRange
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Custom Date Range Picker
                                DateRangePickerSection(
                                    selectedDateRange = selectedDateRange,
                                    onChangeDateRange = onChangeDateRange
                                )

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    thickness = 1.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )

                                // Build Chart Button
                                Button(
                                    onClick = {
                                        isChartVisible = true
                                        onFetchData()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp),
                                    enabled = selectedDateRange.first != null &&
                                             selectedDateRange.second != null &&
                                             selectedParameter != null &&
                                             !isDataLoading,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        text = stringResource(StringKey.BuildChart),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // Loading State
                AnimatedVisibility(
                    visible = isDataLoading,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    stringResource(StringKey.LoadingData),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Chart Display
                AnimatedVisibility(
                    visible = !isDataLoading && isChartVisible && sensorData.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(durationMillis = 500)) +
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetY = { it / 3 }
                            ),
                    exit = fadeOut(animationSpec = tween(durationMillis = 300))
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Chart Card
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            tonalElevation = 2.dp,
                            shadowElevation = 6.dp
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                SensorDataChart(
                                    sensorData = sensorData,
                                    title = selectedParameter?.name ?: stringResource(StringKey.NoDataAvailable),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(340.dp)
                                )
                            }
                        }

                        // Enhanced Data Summary
                        if (sensorData.isNotEmpty()) {
                            EnhancedDataSummary(
                                sensorData = sensorData,
                                parameterUnit = selectedParameter?.unit ?: ""
                            )
                        }
                    }
                }

                // Empty State
                AnimatedVisibility(
                    visible = !isDataLoading && isChartVisible && sensorData.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                stringResource(StringKey.NoDataAvailable),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "Попробуйте выбрать другой период или параметр",
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuickDateRangeSelector(
    onSelectRange: (Long, Long) -> Unit,
    selectedDateRange: Pair<Long?, Long?>
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "Быстрый выбор периода",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val currentTime = System.currentTimeMillis()

            QuickRangeChip(
                label = "24 часа",
                hours = 24,
                currentTime = currentTime,
                selectedDateRange = selectedDateRange,
                onSelectRange = onSelectRange
            )

            QuickRangeChip(
                label = "7 дней",
                hours = 24 * 7,
                currentTime = currentTime,
                selectedDateRange = selectedDateRange,
                onSelectRange = onSelectRange
            )

            QuickRangeChip(
                label = "30 дней",
                hours = 24 * 30,
                currentTime = currentTime,
                selectedDateRange = selectedDateRange,
                onSelectRange = onSelectRange
            )
        }
    }
}

@Composable
fun QuickRangeChip(
    label: String,
    hours: Int,
    currentTime: Long,
    selectedDateRange: Pair<Long?, Long?>,
    onSelectRange: (Long, Long) -> Unit
) {
    val startTime = currentTime - (hours * 60 * 60 * 1000L)
    val isSelected = selectedDateRange.first != null &&
                    selectedDateRange.second != null &&
                    kotlin.math.abs((selectedDateRange.first!! - startTime)) < 60000 &&
                    kotlin.math.abs((selectedDateRange.second!! - currentTime)) < 60000

    FilterChip(
        selected = isSelected,
        onClick = { onSelectRange(startTime, currentTime) },
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else null
    )
}

@Composable
fun SensorSelectionSection(
    selectedParameter: ParameterConfig?,
    availableParameters: List<ParameterConfig>,
    onChangeParameter: (ParameterConfig) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(StringKey.SelectDataType),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        DynamicParametersDropdownMenu(
            selectedParameter = selectedParameter,
            availableParameters = availableParameters,
            onParameterSelected = onChangeParameter
        )
    }
}

@Composable
fun DateRangePickerSection(
    selectedDateRange: Pair<Long?, Long?>,
    onChangeDateRange: (Pair<Long?, Long?>) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = stringResource(StringKey.SelectPeriod),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        DateRangePickerScreen(
            onChangeDateRange = onChangeDateRange,
            selectedDateRange = selectedDateRange
        )
    }
}

@Composable
fun EnhancedDataSummary(
    sensorData: List<SensorDataPoint>,
    parameterUnit: String
) {
    val values = sensorData.map { it.value }
    val minValue = values.minOrNull() ?: 0.0
    val maxValue = values.maxOrNull() ?: 0.0
    val avgValue = if (values.isNotEmpty()) values.average() else 0.0
    val lastValue = sensorData.lastOrNull()?.value ?: 0.0

    // Create list of statistics for horizontal scrolling
    data class StatItem(
        val label: String,
        val value: String,
        val icon: ImageVector,
        val gradient: Brush
    )

    val stats = listOf(
        StatItem(
            label = "Последнее",
            value = "${lastValue.roundTo(2)} $parameterUnit",
            icon = Icons.Default.Thermostat,
            gradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                )
            )
        ),
        StatItem(
            label = "Минимум",
            value = "${minValue.roundTo(2)} $parameterUnit",
            icon = Icons.AutoMirrored.Filled.TrendingDown,
            gradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.7f)
                )
            )
        ),
        StatItem(
            label = "Максимум",
            value = "${maxValue.roundTo(2)} $parameterUnit",
            icon = Icons.AutoMirrored.Filled.TrendingUp,
            gradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                )
            )
        ),
        StatItem(
            label = "Среднее",
            value = "${avgValue.roundTo(2)} $parameterUnit",
            icon = Icons.Default.ShowChart,
            gradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                )
            )
        ),
        StatItem(
            label = "Точек данных",
            value = sensorData.size.toString(),
            icon = Icons.Default.DataUsage,
            gradient = Brush.verticalGradient(
                colors = listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                )
            )
        )
    )

    // Horizontal scrolling row of statistics
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(stats) { stat ->
            StatisticChip(
                label = stat.label,
                value = stat.value,
                icon = stat.icon,
                gradient = stat.gradient,
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Composable
fun StatisticChip(
    label: String,
    value: String,
    icon: ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

// Extension function to round Double to specified decimal places
fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return (this * multiplier).roundToInt() / multiplier
}

