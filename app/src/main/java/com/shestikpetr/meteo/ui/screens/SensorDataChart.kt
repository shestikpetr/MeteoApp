package com.shestikpetr.meteo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.yml.charts.axis.AxisData
import co.yml.charts.common.extensions.formatToSinglePrecision
import co.yml.charts.common.model.Point
import co.yml.charts.ui.linechart.LineChart
import co.yml.charts.ui.linechart.model.*
import com.shestikpetr.meteo.network.SensorDataPoint
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun SensorDataChart(
    modifier: Modifier = Modifier,
    sensorData: List<SensorDataPoint> = emptyList(),
    title: String = "Показания датчика",
    lineColor: Color = Color(0xFF4285F4), // Google Blue
    backgroundColor: Color = Color(0xFFF8F8F8),
    showGrid: Boolean = true
) {
    // Обработка пустых данных
    if (sensorData.isEmpty()) {
        EmptyChartPlaceholder(modifier, title)
        return
    }

    // Динамический расчет границ для более эффективного использования пространства графика
    val padding = 0.1f // 10% отступ для визуальной ясности
    val yRawMin = sensorData.minOfOrNull { it.value.toFloat() } ?: 0f
    val yRawMax = sensorData.maxOfOrNull { it.value.toFloat() } ?: 100f
    val yRange = (yRawMax - yRawMin).coerceAtLeast(1f)
    val yMin = (yRawMin - yRange * padding).coerceAtLeast(0f)
    val yMax = yRawMax + yRange * padding

    // Адаптивный шаг отображения в зависимости от количества данных
    val stepSize = when {
        sensorData.size > 1000 -> sensorData.size / 50
        sensorData.size > 500 -> sensorData.size / 25
        sensorData.size > 100 -> sensorData.size / 10
        else -> maxOf(1, sensorData.size / 10)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Отображение текущего и среднего значения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentValue = sensorData.lastOrNull()?.value ?: 0.0
                val avgValue = if (sensorData.isNotEmpty()) {
                    sensorData.map { it.value }.average()
                } else 0.0

                ValueIndicator(
                    label = "Текущее",
                    value = String.format(Locale.getDefault(), "%.1f", currentValue),
                    color = lineColor
                )

                ValueIndicator(
                    label = "Среднее",
                    value = String.format(Locale.getDefault(), "%.1f", avgValue),
                    color = Color(0xFF34A853) // Google Green
                )
            }

            // Настройка данных для оси X
            val xAxisData = remember(sensorData) {
                AxisData.Builder()
                    .axisStepSize(100.dp)
                    .steps(sensorData.size / stepSize)
                    .labelData { i ->
                        val index = i * stepSize
                        if (index < sensorData.size) {
                            val date = Date(sensorData[index].time * 1000)
                            SimpleDateFormat("dd.MM\nHH:mm", Locale.getDefault()).format(date)
                        } else ""
                    }
                    .labelAndAxisLinePadding(15.dp)
                    .axisLabelFontSize(10.sp)
                    .axisLineColor(Color.Gray.copy(alpha = 0.5f))
                    .build()
            }

            // Настройка данных для оси Y с более адаптивными шагами
            val yAxisData = remember(yMin, yMax) {
                val steps = 5
                AxisData.Builder()
                    .steps(steps)
                    .labelData { i ->
                        val yScale = (yMax - yMin) / steps
                        (yMin + i * yScale).formatToSinglePrecision()
                    }
                    .labelAndAxisLinePadding(20.dp)
                    .axisLabelFontSize(12.sp)
                    .axisLineColor(Color.Gray.copy(alpha = 0.5f))
                    .build()
            }

            // Фильтрация данных для графика
            val filteredSensorData = sensorData.filterIndexed { index, _ ->
                index % stepSize == 0
            }

            // Создание градиента для заливки под линией
            val gradientColors = listOf(
                lineColor.copy(alpha = 0.3f),
                lineColor.copy(alpha = 0.1f),
                lineColor.copy(alpha = 0.0f)
            )

            // Данные линии графика
            val lineChartData = remember(filteredSensorData, yMin, yMax) {
                LineChartData(
                    linePlotData = LinePlotData(
                        lines = listOf(
                            Line(
                                dataPoints = filteredSensorData.mapIndexed { index, point ->
                                    Point(index.toFloat(), point.value.toFloat())
                                },
                                lineStyle = LineStyle(
                                    color = lineColor,
                                    width = 3f
                                ),
                                IntersectionPoint(
                                    color = lineColor,
                                    radius = 5.dp
                                ),
                                SelectionHighlightPoint(
                                    color = lineColor
                                ),
                                ShadowUnderLine(
                                    brush = Brush.verticalGradient(gradientColors),
                                    alpha = 1f
                                ),
                                SelectionHighlightPopUp(
                                    popUpLabel = { x, y ->
                                        val index =
                                            (x * stepSize).toInt().coerceIn(0, sensorData.size - 1)
                                        val date = Date(sensorData[index].time * 1000)
                                        val timeStr = SimpleDateFormat(
                                            "dd.MM HH:mm",
                                            Locale.getDefault()
                                        ).format(date)
                                        "$timeStr\nЗначение: ${
                                            String.format(
                                                Locale.getDefault(),
                                                "%.2f",
                                                y
                                            )
                                        }"
                                    }
                                )
                            )
                        ),
                    ),
                    xAxisData = xAxisData,
                    yAxisData = yAxisData,
                    gridLines = if (showGrid) {
                        GridLines(
                            color = Color.LightGray.copy(alpha = 0.3f),
                            lineWidth = 0.5.dp
                        )
                    } else null,
                    backgroundColor = backgroundColor
                )
            }

            // Рендер графика с улучшенным модификатором
            LineChart(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(8.dp)),
                lineChartData = lineChartData
            )
        }
    }
}

@Composable
private fun ValueIndicator(
    label: String,
    value: String,
    color: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun EmptyChartPlaceholder(
    modifier: Modifier = Modifier,
    title: String
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "Данные отсутствуют",
                color = Color.Gray
            )
        }
    }
}