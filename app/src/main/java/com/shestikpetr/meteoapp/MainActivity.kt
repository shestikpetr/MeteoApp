package com.shestikpetr.meteoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shestikpetr.meteoapp.data.api.RetrofitClient
import com.shestikpetr.meteoapp.navigation.NavGraph
import com.shestikpetr.meteoapp.ui.theme.MeteoAppTheme
import com.shestikpetr.meteoapp.util.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RetrofitClient.init(TokenManager(this))
        enableEdgeToEdge()
        setContent {
            MeteoAppTheme {
                NavGraph()
            }
        }
    }
}
