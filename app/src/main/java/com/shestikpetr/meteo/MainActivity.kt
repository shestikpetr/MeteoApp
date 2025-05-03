package com.shestikpetr.meteo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.shestikpetr.meteo.ui.navigation.MeteoApp
import com.shestikpetr.meteo.ui.theme.MeteoTheme
import com.yandex.mapkit.MapKitFactory
import dagger.hilt.android.AndroidEntryPoint
import ru.sulgik.mapkit.MapKit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        init {
            MapKit.setApiKey("e6cb4f2f-1295-4ffe-bfca-8ab2b9533d6a")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.initialize(this)
        setContent {
            MeteoTheme {
                MeteoApp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
    }

    override fun onStop() {
        MapKitFactory.getInstance().onStop()
        super.onStop()
    }

}

