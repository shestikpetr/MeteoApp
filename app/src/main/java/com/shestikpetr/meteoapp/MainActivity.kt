package com.shestikpetr.meteoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.shestikpetr.meteoapp.navigation.NavGraph
import com.shestikpetr.meteoapp.ui.theme.MeteoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeteoAppTheme {
                NavGraph()
            }
        }
    }
}
