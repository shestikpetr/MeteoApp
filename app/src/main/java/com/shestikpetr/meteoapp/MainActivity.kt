package com.shestikpetr.meteoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.shestikpetr.meteoapp.navigation.NavGraph
import com.shestikpetr.meteoapp.navigation.Screen
import com.shestikpetr.meteoapp.presentation.root.RootViewModel
import com.shestikpetr.meteoapp.ui.theme.MeteoAppTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val rootVm: RootViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by rootVm.themeMode.collectAsState()
            val start by rootVm.start.collectAsState()

            MeteoAppTheme(themeMode = themeMode) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    when (start) {
                        RootViewModel.StartDestination.Auth ->
                            NavGraph(startDestination = Screen.Auth.route)
                        RootViewModel.StartDestination.Main ->
                            NavGraph(startDestination = Screen.Main.route)
                        null -> Unit // первый кадр пока определяется логин
                    }
                }
            }
        }
    }
}
