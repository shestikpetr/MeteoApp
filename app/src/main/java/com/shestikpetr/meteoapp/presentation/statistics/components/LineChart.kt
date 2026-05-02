package com.shestikpetr.meteoapp.presentation.statistics.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.Zoom
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.decoration.HorizontalLine
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.shestikpetr.meteoapp.domain.model.TimeSeriesPoint
import com.shestikpetr.meteoapp.ui.theme.appColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Серия данных на графике (станция × параметр). */
data class MeteoChartSeries(
    val label: String,
    val color: Color,
    val points: List<TimeSeriesPoint>
)

/**
 * Линейный график на основе Vico. Поддерживает несколько серий, pan/zoom,
 * горизонтальные пороги и опциональные «пунктирные» линии прошлого периода.
 *
 * X-ось — секунды с эпохи (Double). Форматирование меток адаптивно к диапазону.
 *
 * Большие массивы точек автоматически прорежиается до [maxPointsPerSeries],
 * чтобы держать частоту кадров (Vico эффективен, но ~10k точек на серию уже
 * нагружают GPU).
 */
@Composable
fun MeteoLineChart(
    series: List<MeteoChartSeries>,
    modifier: Modifier = Modifier,
    previousPeriodSeries: List<MeteoChartSeries> = emptyList(),
    thresholdMin: Double? = null,
    thresholdMax: Double? = null,
    maxPointsPerSeries: Int = 1500
) {
    if (series.isEmpty() || series.all { it.points.isEmpty() }) return

    // Прорежиаем точки и сразу выкидываем пустые серии — иначе они портят
    // авто-диапазон оси (точки 0;0 «оттягивают» график к 1970 году).
    val primary = remember(series, maxPointsPerSeries) {
        series.filter { it.points.isNotEmpty() }
            .map { it.copy(points = downsample(it.points, maxPointsPerSeries)) }
    }
    val previous = remember(previousPeriodSeries, maxPointsPerSeries) {
        previousPeriodSeries.filter { it.points.isNotEmpty() }
            .map { it.copy(points = downsample(it.points, maxPointsPerSeries)) }
    }
    if (primary.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(primary, previous) {
        modelProducer.runTransaction {
            lineSeries {
                primary.forEach { s ->
                    series(
                        x = s.points.map { it.time.toDouble() },
                        y = s.points.map { it.value }
                    )
                }
                previous.forEach { s ->
                    series(
                        x = s.points.map { it.time.toDouble() },
                        y = s.points.map { it.value }
                    )
                }
            }
        }
    }

    val primaryLines = primary.map { s ->
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(s.color)),
            stroke = LineCartesianLayer.LineStroke.Continuous(thicknessDp = 2f),
            areaFill = null,
            pointProvider = null
        )
    }
    val previousLines = previous.map { s ->
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(fill(s.color.copy(alpha = 0.45f))),
            stroke = LineCartesianLayer.LineStroke.Dashed(
                thicknessDp = 1.5f,
                dashLengthDp = 6f,
                gapLengthDp = 4f
            ),
            areaFill = null,
            pointProvider = null
        )
    }
    val allLines = primaryLines + previousLines

    val xRange = remember(primary, previous) {
        val all = primary.flatMap { it.points } + previous.flatMap { it.points }
        if (all.isEmpty()) 0L to 1L
        else all.minOf { it.time } to all.maxOf { it.time }
    }
    val xFormatter = remember(xRange) {
        CartesianValueFormatter { _, value, _ ->
            formatTimeAxisLabel(value.toLong(), xRange.first, xRange.second)
        }
    }

    val decorations = buildList {
        thresholdMin?.let { add(rememberThresholdLine(it, Color(0xFF2F6BCB), "min")) }
        thresholdMax?.let { add(rememberThresholdLine(it, Color(0xFFD2422E), "max")) }
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(allLines)
        ),
        startAxis = VerticalAxis.rememberStart(),
        bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = xFormatter),
        decorations = decorations
    )

    // Множитель зума, на который мы влияем кнопками. Vico не предоставляет
    // публичного API для программного зума — поэтому при изменении этого
    // значения мы пересоздаём VicoZoomState с новым `initialZoom`, и Vico
    // выставляет ему этот фактор как стартовый.
    var zoomMultiplier by rememberSaveable { mutableFloatStateOf(1f) }
    val zoomState = rememberVicoZoomState(
        zoomEnabled = true,
        initialZoom = remember(zoomMultiplier) { Zoom.fixed(zoomMultiplier) },
        minZoom = Zoom.Content,
        maxZoom = remember { Zoom.max(Zoom.fixed(MAX_ZOOM), Zoom.Content) }
    )

    Box(modifier = modifier.fillMaxSize()) {
        CartesianChartHost(
            chart = chart,
            modelProducer = modelProducer,
            modifier = Modifier.fillMaxSize(),
            zoomState = zoomState,
            scrollState = rememberVicoScrollState(scrollEnabled = true)
        )

        // Контролы зума поверх графика.
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            ZoomControl(
                icon = Icons.Default.Add,
                description = "Приблизить",
                enabled = zoomState.value < MAX_ZOOM,
                onClick = {
                    zoomMultiplier = (zoomState.value * ZOOM_STEP).coerceAtMost(MAX_ZOOM)
                }
            )
            ZoomControl(
                icon = Icons.Default.Remove,
                description = "Отдалить",
                enabled = true,
                onClick = {
                    // valueRange.start — минимальный допустимый фактор (Zoom.Content),
                    // нижняя граница, ниже которой Vico всё равно скоррозит значение.
                    val minAllowed = zoomState.valueRange.start
                    zoomMultiplier = (zoomState.value / ZOOM_STEP).coerceAtLeast(minAllowed)
                }
            )
            ZoomControl(
                icon = Icons.Default.Refresh,
                description = "Сбросить зум",
                enabled = zoomMultiplier != 1f,
                onClick = { zoomMultiplier = 1f }
            )
        }
    }
}

private const val MAX_ZOOM = 50f
private const val ZOOM_STEP = 1.6f

@Composable
private fun ZoomControl(
    icon: ImageVector,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.appColors
    val tint = if (enabled) palette.ink2 else palette.ink4
    Surface(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick),
        color = palette.bgElev.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, palette.line),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun rememberThresholdLine(
    value: Double,
    color: Color,
    label: String
): HorizontalLine {
    val line = rememberLineComponent(
        fill = fill(color.copy(alpha = 0.7f)),
        thickness = 1.dp
    )
    return remember(value, color, label) {
        HorizontalLine(
            y = { value },
            line = line,
            label = { label }
        )
    }
}

/** Простое прорежиание шагом — для X-сортированных рядов оно сохраняет форму. */
private fun downsample(points: List<TimeSeriesPoint>, maxPoints: Int): List<TimeSeriesPoint> {
    if (points.size <= maxPoints) return points
    val step = (points.size + maxPoints - 1) / maxPoints
    val result = ArrayList<TimeSeriesPoint>(maxPoints + 2)
    var i = 0
    while (i < points.size) {
        result.add(points[i])
        i += step
    }
    // Обязательно сохраняем последнюю точку, чтобы график доезжал до правого края.
    if (result.last() !== points.last()) result.add(points.last())
    return result
}

private val FORMATTER_HM = SimpleDateFormat("HH:mm", Locale.getDefault())
private val FORMATTER_DH = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
private val FORMATTER_DM = SimpleDateFormat("dd.MM", Locale.getDefault())

/** Подбираем уровень детализации для подписи X-оси в зависимости от ширины окна. */
private fun formatTimeAxisLabel(epochSeconds: Long, minSeconds: Long, maxSeconds: Long): String {
    val date = Date(epochSeconds * 1000)
    val rangeSeconds = (maxSeconds - minSeconds).coerceAtLeast(1)
    return when {
        rangeSeconds < 36 * 3600 -> FORMATTER_HM.format(date)         // < 36 часов -> HH:mm
        rangeSeconds < 14 * 24 * 3600 -> FORMATTER_DH.format(date)    // < 2 недель -> dd.MM HH:mm
        else -> FORMATTER_DM.format(date)                              // больше -> dd.MM
    }
}
