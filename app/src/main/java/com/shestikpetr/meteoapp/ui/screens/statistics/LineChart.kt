package com.shestikpetr.meteoapp.ui.screens.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.toColorInt
import com.shestikpetr.meteoapp.data.model.TimeSeriesDataPoint
import com.shestikpetr.meteoapp.ui.theme.SkyBlue40
import com.shestikpetr.meteoapp.ui.theme.SkyBlue80
import com.shestikpetr.meteoapp.ui.theme.SkyBlueDark
import com.shestikpetr.meteoapp.ui.theme.SunOrange
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_POINTS = 500

private fun aggregate(
    data: List<TimeSeriesDataPoint>,
    level: Int
): List<TimeSeriesDataPoint> {
    if (level <= 1 || data.isEmpty()) return data
    return data.chunked(level).map { chunk ->
        TimeSeriesDataPoint(
            time = chunk[chunk.size / 2].time,
            value = chunk.map { it.value }.average()
        )
    }
}

@Composable
fun LineChart(
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

    val minAggregationLevel = remember(data.size) {
        var level = 1
        while (data.size / level > MAX_POINTS) level *= 2
        level
    }

    val initialLevel = remember(data.size) {
        var level = minAggregationLevel
        while (data.size / level > 150 && level < data.size / 3) level *= 2
        level.coerceAtLeast(minAggregationLevel)
    }

    var aggregationLevel by remember(data.size) { mutableIntStateOf(initialLevel) }

    val aggregatedData = remember(data, aggregationLevel) { aggregate(data, aggregationLevel) }
    val aggregatedAdditional = remember(additionalDatasets, aggregationLevel) {
        additionalDatasets.map { (dsData, color) -> Pair(aggregate(dsData, aggregationLevel), color) }
    }
    val aggregatedPrevData = remember(previousPeriodData, aggregationLevel) {
        aggregate(previousPeriodData, aggregationLevel)
    }

    val values = aggregatedData.map { it.value.toFloat() }
    val prevValues = aggregatedPrevData.map { it.value.toFloat() }
    val allMainValues = values + prevValues
    val minValue = allMainValues.minOrNull() ?: 0f
    val maxValue = allMainValues.maxOrNull() ?: 1f
    val range = if (maxValue - minValue == 0f) 1f else maxValue - minValue

    val pointSpacing = 60.dp
    val minChartWidth = 300.dp
    val calculatedWidth = with(density) { (pointSpacing * (aggregatedData.size - 1).coerceAtLeast(1)).toPx() }
    val chartWidthDp = maxOf(minChartWidth, with(density) { calculatedWidth.toDp() + 60.dp })

    val pointRadius = 4.dp

    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    val trendLine = remember(aggregatedData) { computeTrendLine(aggregatedData) }

    Box(modifier = modifier) {
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

                    val linePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = linePath,
                        color = SkyBlue40,
                        style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                    )

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
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(10.dp.toPx(), 5.dp.toPx())
                                )
                            )
                        }
                    }

                    thresholdMin?.let { thresh ->
                        val threshY = paddingTop + chartHeight - ((thresh.toFloat() - minValue) / range) * chartHeight
                        if (threshY in paddingTop..(paddingTop + chartHeight)) {
                            drawLine(
                                color = Color(0xFF2196F3).copy(alpha = 0.8f),
                                start = Offset(paddingLeft, threshY),
                                end = Offset(paddingLeft + chartWidth, threshY),
                                strokeWidth = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(8.dp.toPx(), 4.dp.toPx())
                                )
                            )
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
                                pathEffect = PathEffect.dashPathEffect(
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

                    if (aggregatedPrevData.size >= 2) {
                        val timeMin = aggregatedData.minOf { it.time }
                        val timeMax = aggregatedData.maxOf { it.time }
                        val timeRange = if (timeMax == timeMin) 1L else timeMax - timeMin

                        val prevPoints = aggregatedPrevData.map { point ->
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
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(6.dp.toPx(), 4.dp.toPx())
                                )
                            )
                        )
                    }

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

                        val currentRadius = when {
                            isSelected -> pointRadius.toPx() * 2f
                            exceedsThreshold -> pointRadius.toPx() * 1.5f
                            else -> pointRadius.toPx()
                        }
                        val pointColor = when {
                            isSelected -> SunOrange
                            exceedsThreshold -> Color(0xFFF44336)
                            else -> SkyBlue40
                        }

                        if (isSelected) {
                            drawCircle(
                                color = SunOrange.copy(alpha = 0.3f),
                                radius = currentRadius + 8.dp.toPx(),
                                center = point
                            )
                        } else if (exceedsThreshold) {
                            drawCircle(
                                color = Color(0xFFF44336).copy(alpha = 0.2f),
                                radius = currentRadius + 4.dp.toPx(),
                                center = point
                            )
                        }

                        drawCircle(color = pointColor, radius = currentRadius, center = point)
                        drawCircle(color = Color.White, radius = currentRadius * 0.6f, center = point)

                        val timeText = dateFormatter.format(Date(aggregatedData[index].time * 1000))
                        val lines = timeText.split("\n")

                        drawContext.canvas.nativeCanvas.apply {
                            drawText(lines[0], point.x, paddingTop + chartHeight + 18.dp.toPx(), textPaint)
                            if (lines.size > 1) {
                                drawText(lines[1], point.x, paddingTop + chartHeight + 32.dp.toPx(), textPaint)
                            }
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
            val canDecreaseDetail = aggregatedData.size / 2 >= 3
            Surface(
                onClick = { if (canDecreaseDetail) aggregationLevel *= 2 },
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

            Text(
                text = "${aggregatedData.size} т.",
                style = MaterialTheme.typography.labelSmall,
                color = SkyBlueDark
            )

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

        // Selected point info
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
                    androidx.compose.foundation.layout.Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = fullDateFormatter.format(Date(selectedPoint.time * 1000)),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(4.dp))
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
                    Spacer(modifier = Modifier.size(4.dp))
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
