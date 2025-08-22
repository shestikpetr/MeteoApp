package com.shestikpetr.meteo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteo.network.SensorDataPoint
import com.shestikpetr.meteo.ui.MeteoViewModel
import com.shestikpetr.meteo.ui.Parameters
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    viewModel: MeteoViewModel,
    selectedChartParameter: Parameters,
    selectedDateRange: Pair<Long?, Long?>,
    onChangeChartParameter: (Parameters) -> Unit,
    onChangeDateRange: (Pair<Long?, Long?>) -> Unit,
    sensorData: List<SensorDataPoint>,
    onFetchData: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isChartVisible by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val isDataLoading = viewModel.chartUiState.collectAsState().value.isLoadingSensorData

    // Автоматически загружать данные, когда выбран диапазон дат и параметр
    LaunchedEffect(selectedDateRange, selectedChartParameter) {
        if (selectedDateRange.first != null && selectedDateRange.second != null) {
            // Не запускаем автоматически при первой загрузке
            if (isChartVisible) {
                onFetchData()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Метеостанция",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "Настройки графика",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    SensorSelectionSection(
                        selectedParameter = selectedChartParameter,
                        onChangeParameter = onChangeChartParameter
                    )

                    DateRangePickerSection(
                        selectedDateRange = selectedDateRange,
                        onChangeDateRange = onChangeDateRange
                    )

                    Button(
                        onClick = {
                            isChartVisible = true
                            onFetchData()
                        },
                        modifier = Modifier.fillMaxWidth(0.8f),
                        enabled = selectedDateRange.first != null && selectedDateRange.second != null && !isDataLoading
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Построить график")
                            Text(text = "Построить график")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedVisibility(
                visible = isDataLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Загрузка данных...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !isDataLoading && isChartVisible && sensorData.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        SensorDataChart(
                            sensorData = sensorData,
                            title = "График ${getParameterDisplayName(selectedChartParameter)}",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                        )

                        // Дополнительная информация о данных
                        if (sensorData.isNotEmpty()) {
                            DataSummary(
                                sensorData = sensorData
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = !isDataLoading && isChartVisible && sensorData.isEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        "Данные для построения графика отсутствуют",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}

@Composable
fun SensorSelectionSection(
    selectedParameter: Parameters,
    onChangeParameter: (Parameters) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Выберите тип данных",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        ParametersDropdownMenu(
            selectedParameter = selectedParameter,
            onChangeParameter = onChangeParameter
        )
    }
}

@Composable
fun DateRangePickerSection(
    selectedDateRange: Pair<Long?, Long?>,
    onChangeDateRange: (Pair<Long?, Long?>) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Выберите период",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        DateRangePickerScreen(
            onChangeDateRange = onChangeDateRange,
            selectedDateRange = selectedDateRange
        )
    }
}

@Composable
fun DataSummary(
    sensorData: List<SensorDataPoint>
) {

    val dateFormatter = SimpleDateFormat("dd.MM.yyyy''HH:mm", Locale.getDefault())
    val startDate = sensorData.minByOrNull { it.time }?.time?.let { Date(it * 1000) }
    val endDate = sensorData.maxByOrNull { it.time }?.time?.let { Date(it * 1000) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                "Сводка данных:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            if (startDate != null && endDate != null) {
                Text(
                    "Период: ${dateFormatter.format(startDate)} - ${dateFormatter.format(endDate)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                "Всего точек: ${sensorData.size}",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

fun getParameterDisplayName(parameter: Parameters): String {
    return when (parameter) {
        Parameters.TEMPERATURE -> "температуры"
        Parameters.HUMIDITY -> "влажности"
        Parameters.PRESSURE -> "давления"
    }
}