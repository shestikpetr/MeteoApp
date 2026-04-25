package com.shestikpetr.meteoapp.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

fun getParameterIcon(code: String): ImageVector {
    return when {
        code.contains("temp", ignoreCase = true) -> Icons.Default.Thermostat
        code.contains("humid", ignoreCase = true) -> Icons.Default.WaterDrop
        code.contains("pressure", ignoreCase = true) || code.contains("press", ignoreCase = true) -> Icons.Default.Compress
        code.contains("wind", ignoreCase = true) -> Icons.Default.Air
        code.contains("rain", ignoreCase = true) || code.contains("precip", ignoreCase = true) -> Icons.Default.Opacity
        code.contains("sun", ignoreCase = true) || code.contains("solar", ignoreCase = true) -> Icons.Default.WbSunny
        code.contains("cloud", ignoreCase = true) -> Icons.Default.Cloud
        code.contains("vis", ignoreCase = true) -> Icons.Default.Visibility
        else -> Icons.AutoMirrored.Filled.ShowChart
    }
}
