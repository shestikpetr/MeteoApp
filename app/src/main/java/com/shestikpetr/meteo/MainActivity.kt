package com.shestikpetr.meteo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

// Model for com.shestikpetr.meteo.SensorData
data class SensorData(
    val id: Int,
    val sensor_name: String,
    val unit: String,
    val additional_info: String,
    val measure_time: String,
    val measure_date: String,
    val measurement: Double
)

// Retrofit API interface
interface ApiService {
    @GET("api/sensors")
    suspend fun getAllSensors(): List<SensorData>
}

// Retrofit instance
object RetrofitInstance {
    private const val BASE_URL = "http://84.237.1.131:8080/api/sensors" // IP адрес вашего сервера

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

// ViewModel для работы с Retrofit
class SensorViewModel : ViewModel() {
    val sensorData = liveData(Dispatchers.IO) {
        try {
            val response = RetrofitInstance.apiService.getAllSensors()
            emit(response)
        } catch (e: Exception) {
            emit(emptyList()) // В случае ошибки возвращаем пустой список
        }
    }
}

// com.shestikpetr.meteo.MainActivity для отображения данных
class MainActivity : ComponentActivity() {
    private val sensorViewModel: SensorViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SensorScreen(sensorViewModel)
        }
    }
}

// UI для отображения сенсоров
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorScreen(sensorViewModel: SensorViewModel) {
    val sensors = sensorViewModel.sensorData.value ?: emptyList() // Safe call for nullable data

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Sensor Data") })
        },
        content = { padding ->
            Box(modifier = Modifier.padding(padding)) {
                if (sensors.isEmpty()) {
                    Text(text = "Loading...", modifier = Modifier.align(Alignment.Center))
                } else {
                    SensorList(sensors)
                }
            }
        }
    )
}

// LazyColumn для отображения списка сенсоров
@Composable
fun SensorList(sensors: List<SensorData>) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(sensors) { sensor ->
            SensorItem(sensor)
        }
    }
}

// Отображение информации о каждом сенсоре
@Composable
fun SensorItem(sensor: SensorData) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp) // Correctly passing Dp value for CardElevation
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Sensor: ${sensor.sensor_name}", style = MaterialTheme.typography.bodyMedium) // Corrected typography
            Text(text = "Unit: ${sensor.unit}")
            Text(text = "Measurement: ${sensor.measurement}")
            Text(text = "Additional Info: ${sensor.additional_info}")
            Text(text = "Date: ${sensor.measure_date}")
            Text(text = "Time: ${sensor.measure_time}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    SensorScreen(sensorViewModel = SensorViewModel())
}
