package com.shestikpetr.meteoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.shestikpetr.meteoapp.navigation.NavGraph
import com.shestikpetr.meteoapp.navigation.Screen
import com.shestikpetr.meteoapp.ui.theme.MeteoAppTheme
import com.shestikpetr.meteoapp.util.TokenStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MeteoAppTheme {
                val tokenStore = remember { TokenStore(applicationContext) }
                var startDestination by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(Unit) {
                    startDestination = if (tokenStore.isLoggedIn()) {
                        Screen.Main.route
                    } else {
                        Screen.Auth.route
                    }
                }

                startDestination?.let { NavGraph(startDestination = it) }
            }
        }
    }
}
