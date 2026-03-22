package com.shestikpetr.meteoapp.ui.screens.statistics

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.shestikpetr.meteoapp.util.TokenManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.core.graphics.toColorInt

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
    val tokenManager = remember { TokenManager(context) }
    val stationRepository = remember { StationRepository(tokenManager) }
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
                    val values = displayData.map { it.value }
                    val min = values.minOrNull() ?: 0.0
                    val max = values.maxOrNull() ?: 0.0
                    val avg = if (values.isNotEmpty()) values.average() else 0.0

                    // Median
                    val sortedValues = values.sorted()
                    val median = if (sortedValues.isNotEmpty()) {
                        if (sortedValues.size % 2 == 0) {
                            (sortedValues[sortedValues.size / 2 - 1] + sortedValues[sortedValues.size / 2]) / 2
                        } else {
                            sortedValues[sortedValues.size / 2]
                        }
                    } else 0.0

                    // Standard deviation
                    val stdDev = if (values.size > 1) {
                        val variance = values.map { (it - avg) * (it - avg) }.average()
                        kotlin.math.sqrt(variance)
                    } else 0.0

                    // Change over period
                    val firstValue = values.firstOrNull() ?: 0.0
                    val lastValue = values.lastOrNull() ?: 0.0
                    val absoluteChange = lastValue - firstValue
                    val percentChange = if (firstValue != 0.0) (absoluteChange / firstValue) * 100 else 0.0

                    // Trend
                    val trend = when {
                        absoluteChange > 0.1 -> "Рост"
                        absoluteChange < -0.1 -> "Падение"
                        else -> "Стабил."
                    }
                    val trendIcon = when {
                        absoluteChange > 0.1 -> Icons.AutoMirrored.Filled.TrendingUp
                        absoluteChange < -0.1 -> Icons.AutoMirrored.Filled.TrendingDown
                        else -> Icons.AutoMirrored.Filled.TrendingFlat
                    }
                    val trendColor = when {
                        absoluteChange > 0.1 -> Color(0xFF4CAF50)
                        absoluteChange < -0.1 -> Color(0xFFF44336)
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
                                value = String.format("%.1f", min),
                                unit = parameterUnit,
                                subtitle = minPoint?.let { statDateTimeFormatter.format(Date(it.time * 1000)) },
                                icon = Icons.AutoMirrored.Filled.TrendingDown,
                                color = SkyBlue40,
                                modifier = Modifier.weight(1f)
                            )
                            StatCardWithSubtitle(
                                title = "Максимум",
                                value = String.format("%.1f", max),
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
                                value = String.format("%.1f", avg),
                                unit = parameterUnit,
                                icon = Icons.Default.BarChart,
                                color = SkyBlueDark,
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Медиана",
                                value = String.format("%.1f", median),
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
                                value = String.format("%.2f", stdDev),
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
                                value = String.format("%+.1f", absoluteChange),
                                unit = parameterUnit,
                                subtitle = String.format("%+.1f%%", percentChange),
                                icon = Icons.Default.Timeline,
                                color = if (absoluteChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
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
                                val n = minOf(displayData.size, secondParamData.size)
                                if (n < 3) null
                                else {
                                    val xValues = displayData.take(n).map { it.value }
                                    val yValues = secondParamData.take(n).map { it.value }
                                    val xMean = xValues.average()
                                    val yMean = yValues.average()

                                    var numerator = 0.0
                                    var denomX = 0.0
                                    var denomY = 0.0
                                    for (i in 0 until n) {
                                        val xDiff = xValues[i] - xMean
                                        val yDiff = yValues[i] - yMean
                                        numerator += xDiff * yDiff
                                        denomX += xDiff * xDiff
                                        denomY += yDiff * yDiff
                                    }

                                    val denominator = kotlin.math.sqrt(denomX * denomY)
                                    if (denominator == 0.0) 0.0 else numerator / denominator
                                }
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

@Composable
private fun LineChart(
    data: List<TimeSeriesDataPoint>,
    modifier: Modifier = Modifier,
    additionalDatasets: List<Pair<List<TimeSeriesDataPoint>, Color>> = emptyList(),
    previousPeriodData: List<TimeSeriesDataPoint> = emptyList(),
    thresholdMin: Double? = null,
    thresholdMax: Double? = null,
    showTrendLine: Boolean = true
) {
    if (data.isEmpty()) return

    val dateFormatter = remember { SimpleDateFormat("dd.MM\nHH:mm", Locale.getDefault()) }
    val density = LocalDensity.current
    val scrollState = rememberScrollState()

    // Aggregation level: 1 = original, 2 = every 2 points averaged, 4 = every 4, etc.
    // Max 500 points to prevent crashes
    val maxPoints = 500

    // Calculate minimum aggregation level to stay under maxPoints
    val minAggregationLevel = remember(data.size) {
        var level = 1
        while (data.size / level > maxPoints) {
            level *= 2
        }
        level
    }

    // Start with a level that shows reasonable detail but stays safe
    val initialLevel = remember(data.size) {
        var level = minAggregationLevel
        // Start with ~100-200 points for good overview
        while (data.size / level > 150 && level < data.size / 3) {
            level *= 2
        }
        level.coerceAtLeast(minAggregationLevel)
    }

    var aggregationLevel by remember(data.size) { mutableIntStateOf(initialLevel) }

    // Aggregate data based on current level
    val aggregatedData = remember(data, aggregationLevel) {
        if (aggregationLevel <= 1) {
            data
        } else {
            data.chunked(aggregationLevel).map { chunk ->
                TimeSeriesDataPoint(
                    time = chunk[chunk.size / 2].time,
                    value = chunk.map { it.value }.average()
                )
            }
        }
    }

    // Aggregate additional datasets
    val aggregatedAdditional = remember(additionalDatasets, aggregationLevel) {
        additionalDatasets.map { (dsData, color) ->
            val aggregated = if (dsData.isEmpty()) emptyList()
            else if (aggregationLevel <= 1) dsData
            else dsData.chunked(aggregationLevel).map { chunk ->
                TimeSeriesDataPoint(chunk[chunk.size / 2].time, chunk.map { it.value }.average())
            }
            Pair(aggregated, color)
        }
    }

    val aggregatedPrevData = remember(previousPeriodData, aggregationLevel) {
        if (previousPeriodData.isEmpty()) emptyList()
        else if (aggregationLevel <= 1) previousPeriodData
        else previousPeriodData.chunked(aggregationLevel).map { chunk ->
            TimeSeriesDataPoint(chunk[chunk.size / 2].time, chunk.map { it.value }.average())
        }
    }

    val values = aggregatedData.map { it.value.toFloat() }
    // Include previous period in Y range (same parameter, same scale)
    val prevValues = aggregatedPrevData.map { it.value.toFloat() }
    val allMainValues = values + prevValues
    val minValue = allMainValues.minOrNull() ?: 0f
    val maxValue = allMainValues.maxOrNull() ?: 1f
    val range = if (maxValue - minValue == 0f) 1f else maxValue - minValue


    // Fixed point spacing - chart width depends on number of points
    val pointSpacing = 60.dp
    val minChartWidth = 300.dp
    val calculatedWidth = with(density) { (pointSpacing * (aggregatedData.size - 1).coerceAtLeast(1)).toPx() }
    val chartWidthDp = maxOf(minChartWidth, with(density) { calculatedWidth.toDp() + 60.dp })

    val pointRadius = 4.dp

    // Selected point for tap interaction
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Calculate trend line (linear regression)
    val trendLine = remember(aggregatedData) {
        if (aggregatedData.size < 2) null
        else {
            val n = aggregatedData.size
            val xMean = (n - 1) / 2.0
            val yMean = aggregatedData.map { it.value }.average()

            var numerator = 0.0
            var denominator = 0.0
            aggregatedData.forEachIndexed { index, point ->
                val xDiff = index - xMean
                val yDiff = point.value - yMean
                numerator += xDiff * yDiff
                denominator += xDiff * xDiff
            }

            val slope = if (denominator != 0.0) numerator / denominator else 0.0
            val intercept = yMean - slope * xMean

            // Return start and end Y values
            Pair(intercept.toFloat(), (slope * (n - 1) + intercept).toFloat())
        }
    }

    Box(modifier = modifier) {
        // Chart content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 40.dp)
                .horizontalScroll(scrollState)
        ) {
            Canvas(
                modifier = Modifier
                    .width(chartWidthDp)
                    .fillMaxSize()
                    .pointerInput(aggregatedData) {
                        detectTapGestures { tapOffset ->
                            // Find closest point to tap
                            val paddingLeft = 16.dp.toPx()
                            val paddingRight = 16.dp.toPx()
                            val chartWidth = size.width - paddingLeft - paddingRight

                            val pointPositions = aggregatedData.mapIndexed { index, _ ->
                                paddingLeft + (index.toFloat() / (aggregatedData.size - 1).coerceAtLeast(1)) * chartWidth
                            }

                            val closestIndex = pointPositions.indices.minByOrNull { index ->
                                kotlin.math.abs(pointPositions[index] - tapOffset.x)
                            }

                            selectedPointIndex = if (selectedPointIndex == closestIndex) null else closestIndex
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                val paddingLeft = 16.dp.toPx()
                val paddingRight = 16.dp.toPx()
                val paddingTop = 24.dp.toPx()
                val paddingBottom = 50.dp.toPx()

                val chartWidth = width - paddingLeft - paddingRight
                val chartHeight = height - paddingTop - paddingBottom

                val points = values.mapIndexed { index, value ->
                    val x = paddingLeft + (index.toFloat() / (values.size - 1).coerceAtLeast(1)) * chartWidth
                    val y = paddingTop + chartHeight - ((value - minValue) / range) * chartHeight
                    Offset(x, y)
                }

                // Draw gradient fill
                if (points.size >= 2) {
                    val fillPath = Path().apply {
                        moveTo(points.first().x, paddingTop + chartHeight)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, paddingTop + chartHeight)
                        close()
                    }

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                SkyBlue40.copy(alpha = 0.3f),
                                SkyBlue40.copy(alpha = 0.0f)
                            )
                        )
                    )

                    // Draw line
                    val lineWidth = 2.5.dp
                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }

                    drawPath(
                        path = linePath,
                        color = SkyBlue40,
                        style = Stroke(width = lineWidth.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw trend line
                    if (showTrendLine) {
                        trendLine?.let { (startY, endY) ->
                            val trendStartY = paddingTop + chartHeight - ((startY - minValue) / range) * chartHeight
                            val trendEndY = paddingTop + chartHeight - ((endY - minValue) / range) * chartHeight

                            drawLine(
                                color = Color(0xFFFF5722).copy(alpha = 0.7f),
                                start = Offset(points.first().x, trendStartY),
                                end = Offset(points.last().x, trendEndY),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(10.dp.toPx(), 5.dp.toPx())
                                )
                            )
                        }
                    }

                    // Draw threshold lines
                    thresholdMin?.let { thresh ->
                        val threshY = paddingTop + chartHeight - ((thresh.toFloat() - minValue) / range) * chartHeight
                        if (threshY in paddingTop..(paddingTop + chartHeight)) {
                            drawLine(
                                color = Color(0xFF2196F3).copy(alpha = 0.8f),
                                start = Offset(paddingLeft, threshY),
                                end = Offset(paddingLeft + chartWidth, threshY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                                )
                            )
                            // Label
                            drawContext.canvas.nativeCanvas.drawText(
                                "мин: ${String.format("%.1f", thresh)}",
                                paddingLeft + 4.dp.toPx(),
                                threshY - 4.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = "#2196F3".toColorInt()
                                    textSize = 9.dp.toPx()
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    thresholdMax?.let { thresh ->
                        val threshY = paddingTop + chartHeight - ((thresh.toFloat() - minValue) / range) * chartHeight
                        if (threshY in paddingTop..(paddingTop + chartHeight)) {
                            drawLine(
                                color = Color(0xFFF44336).copy(alpha = 0.8f),
                                start = Offset(paddingLeft, threshY),
                                end = Offset(paddingLeft + chartWidth, threshY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                                )
                            )
                            drawContext.canvas.nativeCanvas.drawText(
                                "макс: ${String.format("%.1f", thresh)}",
                                paddingLeft + 4.dp.toPx(),
                                threshY - 4.dp.toPx(),
                                android.graphics.Paint().apply {
                                    color = "#F44336".toColorInt()
                                    textSize = 9.dp.toPx()
                                    isAntiAlias = true
                                }
                            )
                        }
                    }

                    // Draw additional datasets (each with its own Y scale)
                    aggregatedAdditional.forEach { (dsData, dsColor) ->
                        if (dsData.size >= 2) {
                            val dsValues = dsData.map { it.value.toFloat() }
                            val dsMin = dsValues.minOrNull() ?: 0f
                            val dsMax = dsValues.maxOrNull() ?: 1f
                            val dsRange = if (dsMax - dsMin == 0f) 1f else dsMax - dsMin

                            val dsPoints = dsValues.mapIndexed { index, value ->
                                val x = paddingLeft + (index.toFloat() / (dsValues.size - 1).coerceAtLeast(1)) * chartWidth
                                val y = paddingTop + chartHeight - ((value - dsMin) / dsRange) * chartHeight
                                Offset(x, y)
                            }

                            val dsFillPath = Path().apply {
                                moveTo(dsPoints.first().x, paddingTop + chartHeight)
                                dsPoints.forEach { lineTo(it.x, it.y) }
                                lineTo(dsPoints.last().x, paddingTop + chartHeight)
                                close()
                            }
                            drawPath(
                                path = dsFillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        dsColor.copy(alpha = 0.1f),
                                        dsColor.copy(alpha = 0.0f)
                                    )
                                )
                            )

                            val dsLinePath = Path().apply {
                                moveTo(dsPoints.first().x, dsPoints.first().y)
                                dsPoints.drop(1).forEach { lineTo(it.x, it.y) }
                            }
                            drawPath(
                                path = dsLinePath,
                                color = dsColor,
                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                            )

                            dsPoints.forEach { point ->
                                drawCircle(color = dsColor, radius = 3.dp.toPx(), center = point)
                                drawCircle(color = Color.White, radius = 1.5.dp.toPx(), center = point)
                            }
                        }
                    }

                    // Draw previous period line (dashed, gray, same Y scale as main)
                    if (aggregatedPrevData.size >= 2) {
                        val prevPoints = aggregatedPrevData.map { point ->
                            // Map using same time axis as main data
                            val timeMin = aggregatedData.minOf { it.time }
                            val timeMax = aggregatedData.maxOf { it.time }
                            val timeRange = if (timeMax == timeMin) 1L else timeMax - timeMin
                            val x = paddingLeft + ((point.time - timeMin).toFloat() / timeRange) * chartWidth
                            val y = paddingTop + chartHeight - ((point.value.toFloat() - minValue) / range) * chartHeight
                            Offset(x, y)
                        }

                        val prevLinePath = Path().apply {
                            moveTo(prevPoints.first().x, prevPoints.first().y)
                            prevPoints.drop(1).forEach { lineTo(it.x, it.y) }
                        }
                        drawPath(
                            path = prevLinePath,
                            color = Color.Gray.copy(alpha = 0.6f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                                )
                            )
                        )
                    }

                    // Draw points and time labels
                    val textPaint = android.graphics.Paint().apply {
                        color = "#5B6B7C".toColorInt()
                        textSize = 9.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                    }

                    val valuePaint = android.graphics.Paint().apply {
                        color = "#1976D2".toColorInt()
                        textSize = 10.dp.toPx()
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        isFakeBoldText = true
                    }

                    points.forEachIndexed { index, point ->
                        val isSelected = index == selectedPointIndex
                        val pointValue = aggregatedData[index].value
                        val exceedsThreshold = (thresholdMax != null && pointValue > thresholdMax) ||
                                (thresholdMin != null && pointValue < thresholdMin)

                        // Draw point (larger if selected)
                        val currentRadius = if (isSelected) pointRadius.toPx() * 2f
                            else if (exceedsThreshold) pointRadius.toPx() * 1.5f
                            else pointRadius.toPx()

                        val pointColor = when {
                            isSelected -> SunOrange
                            exceedsThreshold -> Color(0xFFF44336)
                            else -> SkyBlue40
                        }

                        if (isSelected) {
                            // Draw selection ring
                            drawCircle(
                                color = SunOrange.copy(alpha = 0.3f),
                                radius = currentRadius + 8.dp.toPx(),
                                center = point
                            )
                        } else if (exceedsThreshold) {
                            // Draw warning ring for threshold exceedance
                            drawCircle(
                                color = Color(0xFFF44336).copy(alpha = 0.2f),
                                radius = currentRadius + 4.dp.toPx(),
                                center = point
                            )
                        }

                        drawCircle(
                            color = pointColor,
                            radius = currentRadius,
                            center = point
                        )
                        drawCircle(
                            color = Color.White,
                            radius = currentRadius * 0.6f,
                            center = point
                        )

                        // Draw time label
                        val timeText = dateFormatter.format(Date(aggregatedData[index].time * 1000))
                        val lines = timeText.split("\n")

                        drawContext.canvas.nativeCanvas.apply {
                            // Draw date
                            drawText(
                                lines[0],
                                point.x,
                                paddingTop + chartHeight + 18.dp.toPx(),
                                textPaint
                            )
                            // Draw time
                            if (lines.size > 1) {
                                drawText(
                                    lines[1],
                                    point.x,
                                    paddingTop + chartHeight + 32.dp.toPx(),
                                    textPaint
                                )
                            }

                            // Draw value above point
                            drawText(
                                String.format("%.1f", aggregatedData[index].value),
                                point.x,
                                point.y - 12.dp.toPx(),
                                valuePaint
                            )
                        }
                    }
                }
            }
        }

        // Aggregation controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .background(
                    color = Color.White.copy(alpha = 0.9f),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Less detail (increase aggregation) - min 3 points
            val canDecreaseDetail = aggregatedData.size / 2 >= 3
            Surface(
                onClick = {
                    if (canDecreaseDetail) {
                        aggregationLevel *= 2
                    }
                },
                shape = CircleShape,
                color = if (canDecreaseDetail) SkyBlue80.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = "Меньше деталей",
                    tint = if (canDecreaseDetail) SkyBlue40 else Color.Gray,
                    modifier = Modifier.padding(6.dp)
                )
            }

            // Points count indicator
            Text(
                text = "${aggregatedData.size} т.",
                style = MaterialTheme.typography.labelSmall,
                color = SkyBlueDark
            )

            // More detail (decrease aggregation) - limited by maxPoints
            val canIncreaseDetail = aggregationLevel > minAggregationLevel
            Surface(
                onClick = {
                    if (canIncreaseDetail) {
                        aggregationLevel = (aggregationLevel / 2).coerceAtLeast(minAggregationLevel)
                    }
                },
                shape = CircleShape,
                color = if (canIncreaseDetail) SkyBlue80.copy(alpha = 0.3f) else Color.Gray.copy(alpha = 0.2f),
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Больше деталей",
                    tint = if (canIncreaseDetail) SkyBlue40 else Color.Gray,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }

        // Selected point info card
        selectedPointIndex?.let { index ->
            if (index in aggregatedData.indices) {
                val selectedPoint = aggregatedData[index]
                val fullDateFormatter = remember { SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale.getDefault()) }

                Card(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = fullDateFormatter.format(Date(selectedPoint.time * 1000)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.2f", selectedPoint.value),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = SunOrange
                        )
                    }
                }
            }
        }

        // Trend indicator
        if (showTrendLine) trendLine?.let { (startY, endY) ->
            val trendPercent = if (startY != 0f) ((endY - startY) / startY) * 100 else 0f
            Card(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (trendPercent >= 0) Color(0xFF4CAF50).copy(alpha = 0.1f)
                    else Color(0xFFF44336).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (trendPercent >= 0) Icons.AutoMirrored.Filled.TrendingUp
                        else Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = if (trendPercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%+.1f%%", trendPercent),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (trendPercent >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    unit: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(8.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    if (unit != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCardWithSubtitle(
    title: String,
    value: String,
    unit: String?,
    subtitle: String?,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier
                        .padding(6.dp)
                        .size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    if (unit != null) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun exportDataToCsv(
    context: Context,
    data: List<TimeSeriesDataPoint>,
    stationName: String,
    parameterName: String
) {
    try {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val fileFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val fileName = "${stationName}_${parameterName}_${fileFormatter.format(Date())}.csv"

        val csvContent = buildString {
            appendLine("Дата и время,Значение")
            data.forEach { point ->
                appendLine("${dateFormatter.format(Date(point.time * 1000))},${point.value}")
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                contentValues
            )

            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(csvContent.toByteArray())
                }
                Toast.makeText(context, "Файл сохранён в Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            file.writeText(csvContent)
            Toast.makeText(context, "Файл сохранён: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}
