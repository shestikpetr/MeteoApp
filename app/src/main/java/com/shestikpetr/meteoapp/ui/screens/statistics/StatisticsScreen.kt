package com.shestikpetr.meteoapp.ui.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.data.model.ParameterMetadata
import com.shestikpetr.meteoapp.data.model.TimeSeriesDataPoint
import com.shestikpetr.meteoapp.data.model.UserStationResponse
import com.shestikpetr.meteoapp.data.repository.StationRepository
import com.shestikpetr.meteoapp.ui.theme.SkyBlue40
import com.shestikpetr.meteoapp.ui.theme.SkyBlue80
import com.shestikpetr.meteoapp.ui.theme.SkyBlueDark
import com.shestikpetr.meteoapp.ui.theme.SunOrange
import com.shestikpetr.meteoapp.util.SettingsManager
import com.shestikpetr.meteoapp.util.TokenStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TimePeriod(val label: String, val hours: Int) {
    DAY("24 часа", 24),
    WEEK("Неделя", 168),
    MONTH("Месяц", 720),
    CUSTOM("Свой", 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val tokenStore = remember { TokenStore(context) }
    val stationRepository = remember { StationRepository(tokenStore) }
    val settingsManager = remember { SettingsManager(context) }

    val hiddenStations by settingsManager.hiddenStations.collectAsState(initial = emptySet())
    val hiddenParameters by settingsManager.hiddenParameters.collectAsState(initial = emptySet())
    val tooltipsEnabled by settingsManager.tooltipsEnabled.collectAsState(initial = true)

    var stations by remember { mutableStateOf<List<UserStationResponse>>(emptyList()) }
    var selectedStation by remember { mutableStateOf<UserStationResponse?>(null) }
    var parameters by remember { mutableStateOf<List<ParameterMetadata>>(emptyList()) }
    var selectedParameters by remember { mutableStateOf<List<ParameterMetadata>>(emptyList()) }
    var selectedPeriod by remember { mutableStateOf(TimePeriod.DAY) }

    // Data for first selected parameter (used for stats, export)
    var historyData by remember { mutableStateOf<List<TimeSeriesDataPoint>>(emptyList()) }
    var parameterUnit by remember { mutableStateOf<String?>(null) }
    // Data for additional selected parameters
    var additionalParamsData by remember { mutableStateOf<Map<String, List<TimeSeriesDataPoint>>>(emptyMap()) }

    var isLoading by remember { mutableStateOf(true) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    var stationDropdownExpanded by remember { mutableStateOf(false) }
    var parameterDropdownExpanded by remember { mutableStateOf(false) }

    // Custom date range
    var customStartTime by remember { mutableStateOf<Long?>(null) }
    var customEndTime by remember { mutableStateOf<Long?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    // Last data time
    var lastDataTime by remember { mutableStateOf<Long?>(null) }

    // Colors for parameters on chart
    val parameterColors = remember {
        listOf(
            SkyBlue40,
            SunOrange,
            Color(0xFF4CAF50),
            Color(0xFF9C27B0),
            Color(0xFF00BCD4)
        )
    }

    // Compare period (previous period)
    var comparePeriodEnabled by remember { mutableStateOf(false) }
    var previousPeriodData by remember { mutableStateOf<List<TimeSeriesDataPoint>>(emptyList()) }

    // Thresholds
    var thresholdMin by remember { mutableStateOf<Double?>(null) }
    var thresholdMax by remember { mutableStateOf<Double?>(null) }
    var showThresholdDialog by remember { mutableStateOf(false) }
    var thresholdMinText by remember { mutableStateOf("") }
    var thresholdMaxText by remember { mutableStateOf("") }

    // Trend line
    var showTrendLine by remember { mutableStateOf(true) }

    val dateFormatter = remember { SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()) }
    val dateTimeFormatter = remember { SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()) }

    // Load stations
    LaunchedEffect(Unit, hiddenStations) {
        isLoading = true
        val result = stationRepository.getUserStations()
        result.getOrNull()?.let { loaded ->
            val visible = loaded.filter { (it.station?.stationNumber ?: "") !in hiddenStations }
            stations = visible
            if (visible.isNotEmpty() && (selectedStation == null || selectedStation !in visible)) {
                selectedStation = visible.first()
            }
        }
        isLoading = false
    }

    // Load parameters when station changes
    LaunchedEffect(selectedStation, hiddenParameters) {
        selectedStation?.station?.stationNumber?.let { stationNumber ->
            val result = stationRepository.getStationParameters(stationNumber)
            result.getOrNull()?.let { loaded ->
                val visible = loaded.filter { it.code !in hiddenParameters }
                parameters = visible
                selectedParameters = if (visible.isNotEmpty()) {
                    listOf(visible.first())
                } else {
                    emptyList()
                }
            }
            additionalParamsData = emptyMap()
        }
    }

    // Load last data time for the station (independent of selected period)
    LaunchedEffect(selectedStation, selectedParameters) {
        val stationNumber = selectedStation?.station?.stationNumber
        if (stationNumber == null || selectedParameters.isEmpty()) {
            lastDataTime = null
            return@LaunchedEffect
        }
        val firstParam = selectedParameters.first()
        stationRepository.getLatestDataTime(stationNumber, firstParam.code)
            .getOrNull()?.let { lastDataTime = it }
    }

    // Load history for all selected parameters
    LaunchedEffect(selectedStation, selectedParameters, selectedPeriod, customStartTime, customEndTime) {
        val stationNumber = selectedStation?.station?.stationNumber
        if (stationNumber == null || selectedParameters.isEmpty()) {
            historyData = emptyList()
            parameterUnit = null
            additionalParamsData = emptyMap()
            return@LaunchedEffect
        }

        val startTime: Long
        val endTime: Long
        if (selectedPeriod == TimePeriod.CUSTOM) {
            if (customStartTime == null || customEndTime == null) return@LaunchedEffect
            startTime = customStartTime!! / 1000
            endTime = customEndTime!! / 1000
        } else {
            endTime = System.currentTimeMillis() / 1000
            startTime = endTime - (selectedPeriod.hours * 3600L)
        }

        isLoadingHistory = true

        // Load first parameter (main — for stats, export)
        val firstParam = selectedParameters.first()
        val result = stationRepository.getParameterHistory(
            stationNumber = stationNumber,
            parameterCode = firstParam.code,
            startTime = startTime,
            endTime = endTime
        )
        val firstParamData = result.getOrNull()
        historyData = firstParamData?.data ?: emptyList()
        parameterUnit = firstParamData?.parameter?.unit

        // Load additional parameters
        val additional = mutableMapOf<String, List<TimeSeriesDataPoint>>()
        selectedParameters.drop(1).forEach { param ->
            val r = stationRepository.getParameterHistory(
                stationNumber = stationNumber,
                parameterCode = param.code,
                startTime = startTime,
                endTime = endTime
            )
            r.getOrNull()?.let { additional[param.code] = it.data }
        }
        additionalParamsData = additional

        isLoadingHistory = false
    }

    // Load previous period data (for first selected parameter)
    LaunchedEffect(selectedStation, selectedParameters, selectedPeriod, comparePeriodEnabled, customStartTime, customEndTime) {
        if (!comparePeriodEnabled) {
            previousPeriodData = emptyList()
            return@LaunchedEffect
        }
        val stationNumber = selectedStation?.station?.stationNumber
        val paramCode = selectedParameters.firstOrNull()?.code
        if (stationNumber != null && paramCode != null) {
            val periodDuration: Long
            val currentEndTime: Long
            if (selectedPeriod == TimePeriod.CUSTOM) {
                if (customStartTime == null || customEndTime == null) return@LaunchedEffect
                periodDuration = (customEndTime!! - customStartTime!!) / 1000
                currentEndTime = customStartTime!! / 1000
            } else {
                periodDuration = selectedPeriod.hours * 3600L
                currentEndTime = System.currentTimeMillis() / 1000 - periodDuration
            }
            val prevStartTime = currentEndTime - periodDuration
            val result = stationRepository.getParameterHistory(
                stationNumber = stationNumber,
                parameterCode = paramCode,
                startTime = prevStartTime,
                endTime = currentEndTime
            )
            result.getOrNull()?.let {
                previousPeriodData = it.data
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Статистика",
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SkyBlue40
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = SkyBlue40)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Station selector
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = SkyBlue40.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = null,
                                    tint = SkyBlue40,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Станция",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                                color = SkyBlueDark
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = SkyBlue80.copy(alpha = 0.15f),
                                onClick = { stationDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedStation?.customName
                                            ?: selectedStation?.station?.name
                                            ?: "Выберите станцию",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedStation != null) SkyBlueDark else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = SkyBlue40.copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = SkyBlue40,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = stationDropdownExpanded,
                                onDismissRequest = { stationDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                stations.forEach { station ->
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.Place,
                                                    contentDescription = null,
                                                    tint = SkyBlue40,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = station.customName ?: station.station?.name ?: "",
                                                    fontWeight = if (station == selectedStation) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (station == selectedStation) SkyBlue40 else Color.Black
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedStation = station
                                            stationDropdownExpanded = false
                                        },
                                        modifier = Modifier.background(
                                            if (station == selectedStation) SkyBlue80.copy(alpha = 0.1f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Parameter selector (multi-select)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(32.dp),
                                shape = CircleShape,
                                color = SkyBlue40.copy(alpha = 0.1f)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                    contentDescription = null,
                                    tint = SkyBlue40,
                                    modifier = Modifier.padding(6.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Параметры",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = SkyBlueDark
                                )
                                if (tooltipsEnabled) {
                                    Text(
                                        text = "Выберите один или несколько для сравнения на графике",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Box {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = SkyBlue80.copy(alpha = 0.15f),
                                onClick = { parameterDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when {
                                            selectedParameters.isEmpty() -> "Выберите параметр"
                                            selectedParameters.size == 1 -> selectedParameters.first().name
                                            else -> "${selectedParameters.first().name} +${selectedParameters.size - 1}"
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium,
                                        color = if (selectedParameters.isNotEmpty()) SkyBlueDark else Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = SkyBlue40.copy(alpha = 0.2f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = SkyBlue40,
                                            modifier = Modifier.padding(4.dp)
                                        )
                                    }
                                }
                            }
                            DropdownMenu(
                                expanded = parameterDropdownExpanded,
                                onDismissRequest = { parameterDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                if (tooltipsEnabled) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "Нажмите для выбора/снятия (макс. 5)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        },
                                        onClick = {},
                                        enabled = false
                                    )
                                }
                                parameters.forEachIndexed { index, param ->
                                    val isSelected = selectedParameters.any { it.code == param.code }
                                    val paramIndex = selectedParameters.indexOfFirst { it.code == param.code }
                                    val color = if (isSelected && paramIndex in parameterColors.indices)
                                        parameterColors[paramIndex] else Color.Black
                                    DropdownMenuItem(
                                        text = {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                if (isSelected) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(18.dp)
                                                            .background(color, CircleShape),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "${paramIndex + 1}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = Color.White,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                                        contentDescription = null,
                                                        tint = Color.Gray,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = param.name,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isSelected) color else Color.Black,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedParameters = if (isSelected) {
                                                // Don't allow deselecting the last one
                                                if (selectedParameters.size > 1) {
                                                    selectedParameters.filter { it.code != param.code }
                                                } else selectedParameters
                                            } else {
                                                if (selectedParameters.size < 5) {
                                                    selectedParameters + param
                                                } else selectedParameters
                                            }
                                        },
                                        modifier = Modifier.background(
                                            if (isSelected) color.copy(alpha = 0.08f) else Color.Transparent
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Last data time display
                if (lastDataTime != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = SkyBlue80.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = SkyBlue40,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Последние данные: ${dateTimeFormatter.format(Date(lastDataTime!! * 1000))}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SkyBlueDark
                            )
                        }
                    }
                }

                // Period selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimePeriod.entries.forEach { period ->
                        FilterChip(
                            selected = selectedPeriod == period,
                            onClick = { selectedPeriod = period },
                            label = {
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = period.label,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SkyBlue40,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                // Custom date range selector
                AnimatedVisibility(visible = selectedPeriod == TimePeriod.CUSTOM) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Выберите период",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Start date button
                                OutlinedButton(
                                    onClick = { showStartDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SkyBlue40
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customStartTime?.let { dateFormatter.format(Date(it)) } ?: "С",
                                        maxLines = 1
                                    )
                                }

                                // End date button
                                OutlinedButton(
                                    onClick = { showEndDatePicker = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = SkyBlue40
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = customEndTime?.let { dateFormatter.format(Date(it)) } ?: "По",
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // Options: Period comparison, Thresholds, Area zoom
                Text(
                    text = "Инструменты анализа",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = SkyBlueDark
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = showTrendLine,
                        onClick = { showTrendLine = !showTrendLine },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Тренд",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFFF5722),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = comparePeriodEnabled,
                        onClick = { comparePeriodEnabled = !comparePeriodEnabled },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Пред. период",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF9C27B0),
                            selectedLabelColor = Color.White
                        )
                    )
                    FilterChip(
                        selected = thresholdMin != null || thresholdMax != null,
                        onClick = { showThresholdDialog = true },
                        label = {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "Пороги",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF44336),
                            selectedLabelColor = Color.White
                        )
                    )
                }

                // Hints for active features
                if (tooltipsEnabled) {
                    AnimatedVisibility(visible = comparePeriodEnabled) {
                        Text(
                            text = "Серая пунктирная линия — данные за предыдущий аналогичный период для сравнения динамики",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9C27B0).copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    AnimatedVisibility(visible = thresholdMin != null || thresholdMax != null) {
                        Text(
                            text = "Пунктирные линии — пороговые значения. Точки за пределами порогов выделены красным",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFF44336).copy(alpha = 0.8f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    AnimatedVisibility(visible = showTrendLine && historyData.isNotEmpty()) {
                        Text(
                            text = "Пунктирная линия — линейный тренд. Нажмите на точку для подробностей",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
                val displayData = historyData

                // Time-shift previous period data to align with current
                val shiftedPreviousData = remember(previousPeriodData, displayData) {
                    if (previousPeriodData.isEmpty() || displayData.isEmpty()) emptyList()
                    else {
                        val currentStart = displayData.minOf { it.time }
                        val prevStart = previousPeriodData.minOf { it.time }
                        val timeShift = currentStart - prevStart
                        previousPeriodData.map { TimeSeriesDataPoint(it.time + timeShift, it.value) }
                    }
                }


                // Chart
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isLoadingHistory) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = SkyBlue40
                                    )
                                }

                                // Export CSV button
                                if (historyData.isNotEmpty()) {
                                    Surface(
                                        onClick = {
                                            exportDataToCsv(
                                                context = context,
                                                data = historyData,
                                                stationName = selectedStation?.customName
                                                    ?: selectedStation?.station?.name ?: "station",
                                                parameterName = selectedParameters.firstOrNull()?.name ?: "parameter"
                                            )
                                        },
                                        shape = CircleShape,
                                        color = SkyBlue80.copy(alpha = 0.3f),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Download,
                                            contentDescription = "Экспорт CSV",
                                            tint = SkyBlue40,
                                            modifier = Modifier.padding(6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (historyData.isEmpty() && !isLoadingHistory) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Нет данных за выбранный период",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (historyData.isNotEmpty()) {
                            // Legend
                            if (selectedParameters.size > 1 || (comparePeriodEnabled && previousPeriodData.isNotEmpty())) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    selectedParameters.forEachIndexed { index, param ->
                                        if (index in parameterColors.indices) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(12.dp)
                                                        .background(parameterColors[index], CircleShape)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = param.name,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                    if (comparePeriodEnabled && previousPeriodData.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color.Gray, CircleShape)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Пред. период",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            // Build additional datasets for chart
                            val additionalDatasets = remember(additionalParamsData, selectedParameters) {
                                selectedParameters.drop(1).mapIndexedNotNull { index, param ->
                                    val data = additionalParamsData[param.code]
                                    if (data != null && (index + 1) in parameterColors.indices) {
                                        Pair(data, parameterColors[index + 1])
                                    } else null
                                }
                            }

                            LineChart(
                                data = displayData,
                                additionalDatasets = additionalDatasets,
                                previousPeriodData = shiftedPreviousData,
                                thresholdMin = thresholdMin,
                                thresholdMax = thresholdMax,
                                showTrendLine = showTrendLine,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp)
                            )

                        }
                    }
                }

                // Statistics cards
                AnimatedVisibility(visible = displayData.isNotEmpty()) {
                    val stats = computeSeriesStats(displayData.map { it.value })

                    // Trend
                    val trend = when {
                        stats.absoluteChange > 0.1 -> "Рост"
                        stats.absoluteChange < -0.1 -> "Падение"
                        else -> "Стабил."
                    }
                    val trendIcon = when {
                        stats.absoluteChange > 0.1 -> Icons.AutoMirrored.Filled.TrendingUp
                        stats.absoluteChange < -0.1 -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    }
                    val trendColor = when {
                        stats.absoluteChange > 0.1 -> Color(0xFF4CAF50)
                        stats.absoluteChange < -0.1 -> Color(0xFFF44336)
                        else -> SkyBlue40
                    }

                    // Time of min/max
                    val minPoint = displayData.minByOrNull { it.value }
                    val maxPoint = displayData.maxByOrNull { it.value }

                    // Period
                    val firstTime = displayData.minByOrNull { it.time }?.time
                    val lastTime = displayData.maxByOrNull { it.time }?.time

                    val statDateTimeFormatter = remember { SimpleDateFormat("dd.MM.yy HH:mm", Locale.getDefault()) }
                    val statDateFormatter = remember { SimpleDateFormat("dd.MM.yy", Locale.getDefault()) }

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Статистика",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (tooltipsEnabled) {
                            Text(
                                text = "Основные показатели за выбранный период",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }

                        // Min / Max
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCardWithSubtitle(
                                title = "Минимум",
                                value = String.format("%.1f", stats.min),
                                unit = parameterUnit,
                                subtitle = minPoint?.let { statDateTimeFormatter.format(Date(it.time * 1000)) },
                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                color = SkyBlue40,
                                modifier = Modifier.weight(1f)
                            )
                            StatCardWithSubtitle(
                                title = "Максимум",
                                value = String.format("%.1f", stats.max),
                                unit = parameterUnit,
                                subtitle = maxPoint?.let { statDateTimeFormatter.format(Date(it.time * 1000)) },
                                icon = Icons.AutoMirrored.Filled.TrendingUp,
                                color = SunOrange,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (tooltipsEnabled) {
                            Text(
                                text = "Под значениями указано время, когда был зафиксирован минимум и максимум",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Average / Median
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Среднее",
                                value = String.format("%.1f", stats.avg),
                                unit = parameterUnit,
                                icon = Icons.Default.BarChart,
                                color = SkyBlueDark,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Медиана",
                                value = String.format("%.1f", stats.median),
                                unit = parameterUnit,
                                icon = Icons.Default.Functions,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (tooltipsEnabled) {
                            Text(
                                text = "Среднее — среднее арифметическое всех значений. Медиана — значение посередине отсортированного ряда, устойчиво к выбросам",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Std Dev / Measurements count
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Ст. отклонение",
                                value = String.format("%.2f", stats.stdDev),
                                unit = parameterUnit,
                                icon = Icons.Default.Equalizer,
                                color = Color(0xFF00BCD4),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Измерений",
                                value = displayData.size.toString(),
                                unit = null,
                                icon = Icons.Default.Numbers,
                                color = Color(0xFF795548),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (tooltipsEnabled) {
                            Text(
                                text = "Ст. отклонение — мера разброса значений. Чем больше значение, тем сильнее колебания параметра",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Trend / Change
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Тренд",
                                value = trend,
                                unit = null,
                                icon = trendIcon,
                                color = trendColor,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                            StatCardWithSubtitle(
                                title = "Изменение",
                                value = String.format("%+.1f", stats.absoluteChange),
                                unit = parameterUnit,
                                subtitle = String.format("%+.1f%%", stats.percentChange),
                                icon = Icons.Default.Timeline,
                                color = if (stats.absoluteChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        if (tooltipsEnabled) {
                            Text(
                                text = "Тренд — общее направление изменений. Изменение — разница между первым и последним значением периода",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }

                        // Period info
                        if (firstTime != null && lastTime != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(4.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        modifier = Modifier.size(40.dp),
                                        shape = CircleShape,
                                        color = Color(0xFF607D8B).copy(alpha = 0.1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = Color(0xFF607D8B),
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Период данных",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "${statDateFormatter.format(Date(firstTime * 1000))} — ${statDateFormatter.format(Date(lastTime * 1000))}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF607D8B)
                                        )
                                    }
                                }
                            }
                        }

                        // Correlation analysis (between first two selected parameters)
                        val secondParamData = selectedParameters.getOrNull(1)?.let { additionalParamsData[it.code] }
                        if (secondParamData != null && secondParamData.isNotEmpty() && displayData.isNotEmpty()) {
                            val correlation = remember(displayData, secondParamData) {
                                computePearsonCorrelation(
                                    displayData.map { it.value },
                                    secondParamData.map { it.value }
                                )
                            }

                            if (correlation != null) {
                                val corrColor = when {
                                    correlation > 0.7 -> Color(0xFF4CAF50)
                                    correlation > 0.4 -> SunOrange
                                    correlation > -0.4 -> Color.Gray
                                    correlation > -0.7 -> SunOrange
                                    else -> Color(0xFFF44336)
                                }
                                val corrText = when {
                                    kotlin.math.abs(correlation) > 0.7 -> "Сильная"
                                    kotlin.math.abs(correlation) > 0.4 -> "Средняя"
                                    kotlin.math.abs(correlation) > 0.2 -> "Слабая"
                                    else -> "Нет"
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = corrColor.copy(alpha = 0.1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CompareArrows,
                                                contentDescription = null,
                                                tint = corrColor,
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = "Корреляция: ${selectedParameters.getOrNull(0)?.name ?: ""} — ${selectedParameters.getOrNull(1)?.name ?: ""}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Row(verticalAlignment = Alignment.Bottom) {
                                                Text(
                                                    text = String.format("%.3f", correlation),
                                                    style = MaterialTheme.typography.headlineSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = corrColor
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "($corrText ${if (correlation >= 0) "положительная" else "отрицательная"})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (tooltipsEnabled) {
                                                Text(
                                                    text = "Значение от -1 до 1. Близко к 1 — параметры растут вместе, близко к -1 — один растёт, другой падает, около 0 — связи нет",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Start Date Picker Dialog
    if (showStartDatePicker) {
        val startDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customStartTime ?: (System.currentTimeMillis() - 7 * 24 * 3600 * 1000L),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDatePickerState.selectedDateMillis?.let {
                            customStartTime = it
                        }
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK", color = SkyBlue40)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Отмена", color = SkyBlue40)
                }
            }
        ) {
            DatePicker(state = startDatePickerState)
        }
    }

    // End Date Picker Dialog
    if (showEndDatePicker) {
        val endDatePickerState = rememberDatePickerState(
            initialSelectedDateMillis = customEndTime ?: System.currentTimeMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val minDate = customStartTime ?: 0L
                    return utcTimeMillis >= minDate && utcTimeMillis <= System.currentTimeMillis()
                }
            }
        )
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let {
                            // Set to end of day (23:59:59)
                            customEndTime = it + (24 * 3600 * 1000L - 1)
                        }
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK", color = SkyBlue40)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Отмена", color = SkyBlue40)
                }
            }
        ) {
            DatePicker(state = endDatePickerState)
        }
    }

    // Threshold Dialog
    if (showThresholdDialog) {
        AlertDialog(
            onDismissRequest = { showThresholdDialog = false },
            title = { Text("Пороговые значения") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Укажите границы для отображения на графике",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = thresholdMinText,
                        onValueChange = { thresholdMinText = it },
                        label = { Text("Минимальный порог") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = thresholdMaxText,
                        onValueChange = { thresholdMaxText = it },
                        label = { Text("Максимальный порог") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    thresholdMin = thresholdMinText.toDoubleOrNull()
                    thresholdMax = thresholdMaxText.toDoubleOrNull()
                    showThresholdDialog = false
                }) {
                    Text("OK", color = SkyBlue40)
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        thresholdMin = null
                        thresholdMax = null
                        thresholdMinText = ""
                        thresholdMaxText = ""
                        showThresholdDialog = false
                    }) {
                        Text("Сбросить", color = Color(0xFFF44336))
                    }
                    TextButton(onClick = { showThresholdDialog = false }) {
                        Text("Отмена", color = SkyBlue40)
                    }
                }
            }
        )
    }
}
