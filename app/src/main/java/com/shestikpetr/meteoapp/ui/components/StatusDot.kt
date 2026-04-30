package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.appColors

/**
 * Зелёная точка для онлайн-статуса станции, серая (`ink-4`) — оффлайн.
 * Соответствует .station-row__dot из meteo-web.
 */
@Composable
fun StatusDot(live: Boolean, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    val palette = MaterialTheme.appColors
    val color = if (live) palette.ok else palette.ink4
    Box(
        modifier = modifier
            .size(size)
            .background(color, CircleShape)
    )
}
