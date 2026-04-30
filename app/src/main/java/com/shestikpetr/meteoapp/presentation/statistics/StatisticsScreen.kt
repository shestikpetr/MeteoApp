package com.shestikpetr.meteoapp.presentation.statistics

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.shestikpetr.meteoapp.domain.model.ParameterMeta
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.TimeSeriesPoint
import com.shestikpetr.meteoapp.presentation.statistics.components.LineChart
import com.shestikpetr.meteoapp.ui.components.NumChip
import com.shestikpetr.meteoapp.ui.components.SegmentedTabs
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors
import com.shestikpetr.meteoapp.ui.util.formatDateTime
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private val PARAM_PALETTE = listOf(
    Color(0xFF2F6BCB), // accent (синий)
    Color(0xFFE08C2A), // оранж
    Color(0xFF3FA268), // зелёный
    Color(0xFF8E5BC7), // фиолетовый
    Color(0xFFD15B5B)  // красный
)

@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val palette = MaterialTheme.appColors
    val context = LocalContext.current

    Surface(color = palette.bg, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            StatsTopBar(onNavigateBack = onNavigateBack)

            if (state.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.CircularProgressIndicator(color = palette.ink, strokeWidth = 2.dp)
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
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Статистика",
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.ink
                    )
                    if (state.historyData.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                exportCsv(context, state.historyData,
                                    state.selectedStation?.name ?: "station",
                                    state.selectedParameters.firstOrNull()?.name ?: "parameter")
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Экспорт CSV",
                                tint = palette.ink2
                            )
                        }
                    }
                }

                StationSelector(state.stations, state.selectedStation, viewModel::selectStation)

                ParametersSelector(state.parameters, state.selectedParameters, viewModel::toggleParameter)

                state.lastDataTime?.let { time ->
                    Surface(
                        color = palette.bgSunken,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Последние данные: ${formatDateTime(time)}",
                            style = MeteoTextStyles.MonoSmall,
                            color = palette.ink3,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                }

                PeriodSelector(state, viewModel::setPeriod, viewModel::setComparePeriod, viewModel::setShowTrendLine)

                ChartCard(state)

                if (state.historyData.isNotEmpty()) {
                    StatsGrid(state.historyData, state.parameterUnit)
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
private fun StationSelector(
    stations: List<Station>,
    selected: Station?,
    onSelect: (Station) -> Unit
) {
    val palette = MaterialTheme.appColors
    Column {
        Text(
            text = "СТАНЦИЯ",
            style = MeteoTextStyles.Label,
            color = palette.ink3
        )
        Spacer(Modifier.size(6.dp))
        Surface(
            color = palette.bgElev,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, palette.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                if (stations.isEmpty()) {
                    Text("Нет станций", color = palette.ink3, modifier = Modifier.padding(8.dp))
                } else {
                    stations.forEach { st ->
                        val isSel = st.stationNumber == selected?.stationNumber
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(st) },
                            color = if (isSel) palette.bgSunken else Color.Transparent,
                            shape = RoundedCornerShape(6.dp),
                            border = if (isSel) BorderStroke(1.dp, palette.line2) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = st.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                        color = palette.ink,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = st.stationNumber,
                                        style = MeteoTextStyles.MonoSmall,
                                        color = palette.ink4
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

@Composable
private fun ParametersSelector(
    parameters: List<ParameterMeta>,
    selected: List<ParameterMeta>,
    onToggle: (ParameterMeta) -> Unit
) {
    val palette = MaterialTheme.appColors
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
                        "Нет параметров",
                        color = palette.ink3,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    parameters.forEach { param ->
                        val idx = selected.indexOfFirst { it.code == param.code }.takeIf { it >= 0 }
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
                    }
                }
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    state: StatisticsUiState,
    onPeriod: (TimePeriod) -> Unit,
    onCompare: (Boolean) -> Unit,
    onShowTrend: (Boolean) -> Unit
) {
    val palette = MaterialTheme.appColors
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SegmentedTabs(
                options = TimePeriod.entries.map { it.label },
                selectedIndex = TimePeriod.entries.indexOf(state.period),
                onSelected = { onPeriod(TimePeriod.entries[it]) },
                monoLabels = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { /* TODO: date picker for CUSTOM */ }) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = "Выбрать период",
                    tint = palette.ink2
                )
            }
        }

        Spacer(Modifier.size(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToggleRow(
                label = "Линия тренда",
                checked = state.showTrendLine,
                onChecked = onShowTrend,
                modifier = Modifier.weight(1f)
            )
            ToggleRow(
                label = "Пред. период",
                checked = state.comparePeriodEnabled,
                onChecked = onCompare,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = modifier
            .clickable { onChecked(!checked) },
        color = if (checked) palette.bgSunken else palette.bgElev,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (checked) palette.line2 else palette.line)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = if (checked) palette.ink else palette.ink3
            )
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        if (checked) palette.ink else palette.line2,
                        androidx.compose.foundation.shape.CircleShape
                    )
            )
        }
    }
}

@Composable
private fun ChartCard(state: StatisticsUiState) {
    val palette = MaterialTheme.appColors
    Surface(
        color = palette.bgElev,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, palette.line),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.selectedParameters.firstOrNull()?.name ?: "—",
                    style = MaterialTheme.typography.titleMedium,
                    color = palette.ink
                )
                Text(
                    text = state.parameterUnit ?: "",
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink4
                )
            }
            Spacer(Modifier.size(6.dp))

            // Легенда
            if (state.selectedParameters.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.selectedParameters.forEachIndexed { i, p ->
                        val color = PARAM_PALETTE[i % PARAM_PALETTE.size]
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(width = 12.dp, height = 2.dp)
                                    .background(color)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                p.name,
                                style = MeteoTextStyles.MonoSmall,
                                color = palette.ink3,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                Spacer(Modifier.size(8.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
            ) {
                if (state.historyData.isEmpty() && !state.isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Нет данных за выбранный период",
                            color = palette.ink3,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else if (state.historyData.isNotEmpty()) {
                    LineChart(
                        data = state.historyData,
                        additionalDatasets = state.selectedParameters.drop(1)
                            .mapIndexedNotNull { index, p ->
                                val data = state.additionalParamsData[p.code] ?: return@mapIndexedNotNull null
                                val color = PARAM_PALETTE[(index + 1) % PARAM_PALETTE.size]
                                data to color
                            },
                        previousPeriodData = state.previousPeriodData,
                        thresholdMin = state.thresholdMin,
                        thresholdMax = state.thresholdMax,
                        showTrendLine = state.showTrendLine,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (state.isLoadingHistory) {
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

@Composable
private fun StatsGrid(data: List<TimeSeriesPoint>, unit: String?) {
    val palette = MaterialTheme.appColors
    val stats = remember(data) { computeStats(data.map { it.value }) }
    Column {
        Text("СВОДКА", style = MeteoTextStyles.Label, color = palette.ink3)
        Spacer(Modifier.size(6.dp))
        Surface(
            color = palette.bgElev,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, palette.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                StatRow("Минимум", stats.min, unit, isFirst = true)
                StatRow("Максимум", stats.max, unit)
                StatRow("Среднее", stats.avg, unit)
                StatRow("Медиана", stats.median, unit)
                StatRow("Ст. отклонение", stats.std, unit)
                StatRow("Измерений", data.size.toDouble(), null, isLast = true, integer = true)
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
            .padding(horizontal = 14.dp, vertical = 10.dp),
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

private fun exportCsv(
    context: Context,
    data: List<TimeSeriesPoint>,
    stationName: String,
    parameterName: String
) {
    try {
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val fileFormatter = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val safeStation = stationName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val safeParam = parameterName.replace(Regex("[^A-Za-z0-9_-]"), "_")
        val fileName = "${safeStation}_${safeParam}_${fileFormatter.format(Date())}.csv"
        val csv = buildString {
            appendLine("time_iso,value")
            data.forEach { p ->
                appendLine("${dateFormatter.format(Date(p.time * 1000))},${p.value}")
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
