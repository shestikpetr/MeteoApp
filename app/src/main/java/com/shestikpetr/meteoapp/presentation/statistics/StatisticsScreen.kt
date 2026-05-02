package com.shestikpetr.meteoapp.presentation.statistics

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.TimeSeriesPoint
import com.shestikpetr.meteoapp.presentation.statistics.components.MeteoChartSeries
import com.shestikpetr.meteoapp.presentation.statistics.components.MeteoLineChart
import com.shestikpetr.meteoapp.ui.components.NumChip
import com.shestikpetr.meteoapp.ui.components.SegmentedTabsEqual
import com.shestikpetr.meteoapp.ui.theme.AppPalette
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors
import com.shestikpetr.meteoapp.ui.util.formatDate
import com.shestikpetr.meteoapp.ui.util.formatTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private val PARAM_PALETTE = listOf(
    Color(0xFF2F6BCB), // синий
    Color(0xFFE08C2A), // оранж
    Color(0xFF3FA268), // зелёный
    Color(0xFF8E5BC7), // фиолетовый
    Color(0xFFD15B5B)  // красный
)

/** Серия для отображения: метаданные + цвет + точки. */
private data class DisplaySeries(
    val key: SeriesKey,
    val station: Station,
    val parameter: ParameterMeta,
    val color: Color,
    val points: List<TimeSeriesPoint>,
    val unit: String?,
    val previousPoints: List<TimeSeriesPoint>
)

@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val palette = MaterialTheme.appColors

    Surface(color = palette.bg, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatsTopBar(onNavigateBack = onNavigateBack)

            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = palette.ink, strokeWidth = 2.dp
                    )
                }
                return@Column
            }

            val scroll = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .widthIn(max = 880.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Статистика",
                    style = MaterialTheme.typography.headlineMedium,
                    color = palette.ink
                )

                StationsSelector(
                    stations = state.stations,
                    selected = state.selectedStations,
                    lastDataTimes = state.lastDataTimes,
                    onToggle = viewModel::toggleStation
                )

                ParametersSelector(
                    parameters = state.parameters,
                    selected = state.selectedParameters,
                    onToggle = viewModel::toggleParameter,
                    isMultiStation = state.isMultiStation
                )

                PeriodSelector(
                    state = state,
                    onPeriod = viewModel::setPeriod,
                    onCustomRange = viewModel::setCustomRange
                )

                val seriesList = remember(
                    state.seriesData,
                    state.previousPeriodData,
                    state.selectedStations,
                    state.selectedParameters,
                    state.parameterUnits,
                    state.isMultiStation
                ) {
                    buildDisplaySeries(state)
                }

                ChartCard(
                    seriesList = seriesList,
                    isLoading = state.isLoadingHistory,
                    thresholdMin = state.thresholdMin,
                    thresholdMax = state.thresholdMax
                )

                if (seriesList.isNotEmpty()) {
                    SummarySection(seriesList)
                }
            }
        }
    }
}

@Composable
private fun StatsTopBar(onNavigateBack: () -> Unit) {
    val palette = MaterialTheme.appColors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.bgElev)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = palette.ink
                )
            }
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = palette.ink
            )
        }
        HorizontalDivider(color = palette.line)
    }
}

@Composable
private fun StationsSelector(
    stations: List<Station>,
    selected: List<Station>,
    lastDataTimes: Map<String, Long?>,
    onToggle: (Station) -> Unit
) {
    val palette = MaterialTheme.appColors
    Column {
        Text("СТАНЦИИ", style = MeteoTextStyles.Label, color = palette.ink3)
        Spacer(Modifier.size(6.dp))
        Surface(
            color = palette.bgElev,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, palette.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (stations.isEmpty()) {
                    Text(
                        text = "Нет станций",
                        color = palette.ink3,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    stations.forEach { st ->
                        val idx = selected.indexOfFirst { it.stationNumber == st.stationNumber }
                            .takeIf { it >= 0 }
                        StationRow(
                            station = st,
                            index = idx,
                            lastDataTime = lastDataTimes[st.stationNumber],
                            onClick = { onToggle(st) }
                        )
                    }
                }
            }
        }
    }
}

/** Строка станции: квадратик-индекс выбора + имя/номер + время последних данных. */
@Composable
private fun StationRow(
    station: Station,
    index: Int?,
    lastDataTime: Long?,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.appColors
    val isSelected = index != null
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = if (isSelected) palette.bgSunken else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, palette.line2) else null,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Квадратик-индикатор: пустой/с индексом серии.
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(
                                PARAM_PALETTE[index!! % PARAM_PALETTE.size],
                                RoundedCornerShape(5.dp)
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = RoundedCornerShape(5.dp),
                        color = palette.bgElev,
                        border = BorderStroke(1.5.dp, palette.line2)
                    ) {}
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = station.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = station.stationNumber,
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink4
                )
            }
            // Время последних данных — справа, в две строки (дата / время).
            if (lastDataTime != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatDate(lastDataTime),
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink3
                    )
                    Text(
                        text = formatTime(lastDataTime),
                        style = MeteoTextStyles.MonoSmall.copy(fontWeight = FontWeight.Medium),
                        color = palette.ink2
                    )
                }
            }
        }
    }
}

@Composable
private fun ParametersSelector(
    parameters: List<ParameterMeta>,
    selected: List<ParameterMeta>,
    onToggle: (ParameterMeta) -> Unit,
    isMultiStation: Boolean
) {
    val palette = MaterialTheme.appColors
    val grouped = remember(parameters) {
        val byName = LinkedHashMap<String, MutableList<ParameterMeta>>()
        parameters.forEach { p -> byName.getOrPut(p.name) { mutableListOf() }.add(p) }
        byName.values.toList()
    }
    Column {
        Text("ПАРАМЕТРЫ", style = MeteoTextStyles.Label, color = palette.ink3)
        Spacer(Modifier.size(6.dp))
        Surface(
            color = palette.bgElev,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, palette.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                if (parameters.isEmpty()) {
                    Text(
                        text = if (isMultiStation)
                            "У выбранных станций нет общих параметров"
                        else "Нет параметров",
                        color = palette.ink3,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    grouped.forEach { group ->
                        if (group.size == 1) {
                            val param = group[0]
                            val idx = selected.indexOfFirst { it.code == param.code }
                                .takeIf { it >= 0 }
                            NumChip(
                                label = param.name,
                                onClick = { onToggle(param) },
                                index = idx,
                                swatchColor = idx?.let { PARAM_PALETTE[it % PARAM_PALETTE.size] },
                                desc = param.description?.takeIf { it.isNotBlank() },
                                sub = param.unit?.takeIf { it.isNotBlank() },
                                isPrimary = idx == 0,
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            ParameterGroupChip(
                                group = group,
                                selected = selected,
                                onToggle = onToggle,
                                palette = palette
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParameterGroupChip(
    group: List<ParameterMeta>,
    selected: List<ParameterMeta>,
    onToggle: (ParameterMeta) -> Unit,
    palette: AppPalette
) {
    val first = group.first()
    val selectedInGroup = group.count { p -> selected.any { it.code == p.code } }
    var expanded by rememberSaveable(first.name) { mutableStateOf(selectedInGroup > 0) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .background(
                        if (selectedInGroup > 0) palette.ink else Color.Transparent,
                        RoundedCornerShape(5.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (selectedInGroup > 0) {
                    Text(
                        text = "$selectedInGroup",
                        style = MeteoTextStyles.MonoSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                } else {
                    Text(
                        text = "${group.size}",
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink4
                    )
                }
            }
            Spacer(Modifier.size(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = first.name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                first.unit?.takeIf { it.isNotBlank() }?.let { unit ->
                    Text(
                        text = unit,
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink4
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.ink3,
                modifier = Modifier.size(18.dp)
            )
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, top = 2.dp, bottom = 4.dp)) {
                group.forEach { param ->
                    val idx = selected.indexOfFirst { it.code == param.code }.takeIf { it >= 0 }
                    NumChip(
                        label = param.description?.takeIf { it.isNotBlank() }
                            ?: "Датчик #${param.code}",
                        onClick = { onToggle(param) },
                        index = idx,
                        swatchColor = idx?.let { PARAM_PALETTE[it % PARAM_PALETTE.size] },
                        sub = param.unit?.takeIf { it.isNotBlank() },
                        isPrimary = idx == 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodSelector(
    state: StatisticsUiState,
    onPeriod: (TimePeriod) -> Unit,
    onCustomRange: (Long, Long) -> Unit
) {
    val palette = MaterialTheme.appColors
    var pickerOpen by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SegmentedTabsEqual(
            options = TimePeriod.entries.map { it.label },
            selectedIndex = TimePeriod.entries.indexOf(state.period),
            onSelected = { i ->
                val p = TimePeriod.entries[i]
                if (p == TimePeriod.CUSTOM) {
                    pickerOpen = true
                } else {
                    onPeriod(p)
                }
            },
            monoLabels = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Чип «От … — До …» виден только в режиме CUSTOM. По тапу открывает picker.
        if (state.period == TimePeriod.CUSTOM) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { pickerOpen = true },
                color = palette.bgSunken,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, palette.line)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = palette.ink3,
                        modifier = Modifier.size(16.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        if (state.customStartMs != null && state.customEndMs != null) {
                            Text(
                                text = "${formatDate(state.customStartMs / 1000)}  →  ${formatDate(state.customEndMs / 1000)}",
                                style = MeteoTextStyles.Mono,
                                color = palette.ink
                            )
                            Text(
                                text = "${humanizeDuration(state.customEndMs - state.customStartMs)} • тап для редактирования",
                                style = MeteoTextStyles.MonoSmall,
                                color = palette.ink4
                            )
                        } else {
                            Text(
                                text = "Выберите начало и конец периода",
                                style = MeteoTextStyles.Mono,
                                color = palette.ink3
                            )
                        }
                    }
                }
            }
        }
    }

    if (pickerOpen) {
        DateRangePickerDialog(
            initialStartMs = state.customStartMs,
            initialEndMs = state.customEndMs,
            onDismiss = { pickerOpen = false },
            onConfirm = { start, end ->
                pickerOpen = false
                onCustomRange(start, end)
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    initialStartMs: Long?,
    initialEndMs: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val state = rememberDateRangePickerState(
        initialSelectedStartDateMillis = initialStartMs,
        initialSelectedEndDateMillis = initialEndMs
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                onClick = {
                    val s = state.selectedStartDateMillis
                    val e = state.selectedEndDateMillis
                    if (s != null && e != null) {
                        // DateRangePicker возвращает начало дня по UTC. Сдвигаем
                        // конец на «конец дня», чтобы захватить весь выбранный
                        // диапазон.
                        val endOfDay = e + 24 * 60 * 60 * 1000L - 1
                        onConfirm(s, endOfDay)
                    }
                }
            ) { Text("Применить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    ) {
        DateRangePicker(
            state = state,
            title = {
                Text(
                    text = "Период",
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                )
            },
            showModeToggle = false
        )
    }
}

@Composable
private fun ChartCard(
    seriesList: List<DisplaySeries>,
    isLoading: Boolean,
    thresholdMin: Double?,
    thresholdMax: Double?
) {
    val palette = MaterialTheme.appColors
    Surface(
        color = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Легенда (всегда — на одной серии или нескольких).
            if (seriesList.isNotEmpty()) {
                ChartLegend(seriesList)
                Spacer(Modifier.size(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                val hasData = seriesList.any { it.points.isNotEmpty() }
                if (!hasData && !isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Нет данных за выбранный период",
                            color = palette.ink3,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (hasData) {
                    val chartSeries = remember(seriesList) {
                        seriesList.map { ds ->
                            MeteoChartSeries(
                                label = ds.parameter.name,
                                color = ds.color,
                                points = ds.points
                            )
                        }
                    }
                    val previousSeries = remember(seriesList) {
                        seriesList.filter { it.previousPoints.isNotEmpty() }.map { ds ->
                            MeteoChartSeries(
                                label = "${ds.parameter.name} (пред.)",
                                color = ds.color,
                                points = ds.previousPoints
                            )
                        }
                    }
                    MeteoLineChart(
                        series = chartSeries,
                        previousPeriodSeries = previousSeries,
                        thresholdMin = thresholdMin,
                        thresholdMax = thresholdMax,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (isLoading) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(16.dp)
                            .padding(top = 8.dp),
                        strokeWidth = 1.5.dp,
                        color = palette.ink3
                    )
                }
            }
        }
    }
}

/** Лёгкая горизонтальная легенда: цвет + лейбл (станция · параметр). */
@Composable
private fun ChartLegend(seriesList: List<DisplaySeries>) {
    val palette = MaterialTheme.appColors
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        seriesList.forEach { ds ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 2.dp)
                        .background(ds.color)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = legendLabel(ds),
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink2,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ds.unit?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink4
                    )
                }
            }
        }
    }
}

/** Сводка по каждой серии. Иконка скачивания CSV — на каждой карточке. */
@Composable
private fun SummarySection(seriesList: List<DisplaySeries>) {
    val palette = MaterialTheme.appColors
    Column {
        Text(
            text = "СВОДКА",
            style = MeteoTextStyles.Label,
            color = palette.ink3,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        Spacer(Modifier.size(2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            seriesList.forEach { ds ->
                SeriesStatsCard(ds)
            }
        }
    }
}

@Composable
private fun SeriesStatsCard(ds: DisplaySeries) {
    val palette = MaterialTheme.appColors
    val context = LocalContext.current
    val stats = remember(ds.points) { computeStats(ds.points.map { it.value }) }
    Surface(
        color = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Заголовок серии: цветной маркер + название параметра + станция
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 14.dp, height = 14.dp)
                        .background(ds.color, RoundedCornerShape(3.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ds.parameter.name,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = ds.station.name,
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink3,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                ds.unit?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        text = it,
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink4
                    )
                }
                IconButton(
                    onClick = { exportCsv(context, listOf(ds)) },
                    enabled = ds.points.isNotEmpty()
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Экспорт CSV",
                        tint = if (ds.points.isNotEmpty()) palette.ink2 else palette.ink4
                    )
                }
            }
            HorizontalDivider(color = palette.line)
            if (ds.points.isEmpty()) {
                Text(
                    text = "Нет данных",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.ink4,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                )
            } else {
                StatRow("Минимум", stats.min, ds.unit, isFirst = true)
                StatRow("Максимум", stats.max, ds.unit)
                StatRow("Среднее", stats.avg, ds.unit)
                StatRow("Медиана", stats.median, ds.unit)
                StatRow("Ст. отклонение", stats.std, ds.unit)
                StatRow("Измерений", ds.points.size.toDouble(), null, isLast = true, integer = true)
            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: Double,
    unit: String?,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    integer: Boolean = false
) {
    val palette = MaterialTheme.appColors
    if (!isFirst) HorizontalDivider(color = palette.line)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.ink2
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (integer) value.toInt().toString()
                else String.format(Locale.US, "%.2f", value),
                style = MeteoTextStyles.Mono.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),
                color = palette.ink
            )
            unit?.let {
                Text(
                    text = " $it",
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink3
                )
            }
        }
    }
}

private data class SeriesStats(
    val min: Double,
    val max: Double,
    val avg: Double,
    val median: Double,
    val std: Double
)

private fun computeStats(values: List<Double>): SeriesStats {
    if (values.isEmpty()) return SeriesStats(0.0, 0.0, 0.0, 0.0, 0.0)
    val sorted = values.sorted()
    val avg = values.average()
    val variance = values.sumOf { (it - avg) * (it - avg) } / values.size
    return SeriesStats(
        min = sorted.first(),
        max = sorted.last(),
        avg = avg,
        median = sorted[sorted.size / 2],
        std = sqrt(variance)
    )
}

private fun legendLabel(ds: DisplaySeries): String {
    return "${ds.station.name} · ${ds.parameter.name}"
}

/** Удобочитаемая длительность в днях / часах / минутах. */
private fun humanizeDuration(durationMs: Long): String {
    val totalMin = durationMs / 60_000
    val days = totalMin / (60 * 24)
    val hours = (totalMin % (60 * 24)) / 60
    val minutes = totalMin % 60
    return when {
        days > 0 -> "$days д ${if (hours > 0) "$hours ч" else ""}".trim()
        hours > 0 -> "$hours ч ${if (minutes > 0) "$minutes мин" else ""}".trim()
        else -> "$minutes мин"
    }
}

/**
 * Собирает [DisplaySeries] для UI: каждой паре (станция × параметр) назначается
 * цвет из [PARAM_PALETTE]. Порядок индексации:
 * - При мульти-станции: каждая станция = отдельный цвет (параметр всегда один).
 * - При одной станции и нескольких параметрах: каждый параметр = отдельный цвет.
 */
private fun buildDisplaySeries(state: StatisticsUiState): List<DisplaySeries> {
    val list = mutableListOf<DisplaySeries>()
    if (state.isMultiStation) {
        val param = state.selectedParameters.firstOrNull() ?: return emptyList()
        state.selectedStations.forEachIndexed { index, st ->
            val key = SeriesKey(st.stationNumber, param.code)
            list += DisplaySeries(
                key = key,
                station = st,
                parameter = param,
                color = PARAM_PALETTE[index % PARAM_PALETTE.size],
                points = state.seriesData[key].orEmpty(),
                unit = state.parameterUnits[param.code] ?: param.unit,
                previousPoints = state.previousPeriodData[key].orEmpty()
            )
        }
    } else {
        val station = state.selectedStations.firstOrNull() ?: return emptyList()
        state.selectedParameters.forEachIndexed { index, p ->
            val key = SeriesKey(station.stationNumber, p.code)
            list += DisplaySeries(
                key = key,
                station = station,
                parameter = p,
                color = PARAM_PALETTE[index % PARAM_PALETTE.size],
                points = state.seriesData[key].orEmpty(),
                unit = state.parameterUnits[p.code] ?: p.unit,
                previousPoints = state.previousPeriodData[key].orEmpty()
            )
        }
    }
    return list
}

/**
 * Экспорт всех видимых серий в один CSV (long-format):
 * `time_iso,station,parameter,value`.
 */
private fun exportCsv(context: Context, seriesList: List<DisplaySeries>) {
    if (seriesList.all { it.points.isEmpty() }) {
        Toast.makeText(context, "Нет данных для экспорта", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val fileFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val first = seriesList.first()
        val safeStation = first.station.name
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .ifBlank { "stations" }
        // Когда экспортируем одну серию — добавляем код параметра в имя,
        // чтобы файлы по разным параметрам не перезаписывали друг друга.
        val nameSuffix = if (seriesList.size == 1) "_p${first.parameter.code}" else ""
        val fileName = "meteo_${safeStation}${nameSuffix}_${fileFormatter.format(Date())}.csv"
        val csv = buildString {
            appendLine("time_iso,station,station_number,parameter,parameter_code,value,unit")
            seriesList.forEach { ds ->
                ds.points.forEach { p ->
                    appendLine(
                        listOf(
                            dateFormatter.format(Date(p.time * 1000)),
                            csvEscape(ds.station.name),
                            ds.station.stationNumber,
                            csvEscape(ds.parameter.name),
                            ds.parameter.code.toString(),
                            p.value.toString(),
                            ds.unit ?: ""
                        ).joinToString(",")
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cv = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv)
            uri?.let {
                context.contentResolver.openOutputStream(it)?.use { os -> os.write(csv.toByteArray()) }
                Toast.makeText(context, "Сохранено в Downloads: $fileName", Toast.LENGTH_LONG).show()
            }
        } else {
            @Suppress("DEPRECATION")
            val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(dir, fileName)
            file.writeText(csv)
            Toast.makeText(context, "Сохранено: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Ошибка экспорта: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private fun csvEscape(value: String): String =
    if (value.contains(',') || value.contains('"') || value.contains('\n')) {
        "\"${value.replace("\"", "\"\"")}\""
    } else value
