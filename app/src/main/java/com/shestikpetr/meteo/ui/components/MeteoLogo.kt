package com.shestikpetr.meteo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Custom animated Meteo logo with minimalist design
 * Features:
 * - Weather station tower with rotating antenna
 * - Pulsing signal waves
 * - Breathing animation for the whole logo
 * - Blue gradient theme matching app design
 */
@Composable
fun MeteoLogo(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    animated: Boolean = true
) {
    val density = LocalDensity.current
    val sizePixels = with(density) { size.toPx() }

    // Colors
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "logo_animation")

    // Breathing effect - subtle scale animation
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing"
    )

    // Antenna rotation
    val antennaRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing)
        ),
        label = "antenna_rotation"
    )

    // Signal waves pulsing
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_alpha"
    )

    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_scale"
    )

    Canvas(
        modifier = modifier
            .size(size)
            .then(
                if (animated) Modifier.graphicsLayer(
                    scaleX = breathingScale,
                    scaleY = breathingScale
                ) else Modifier
            )
    ) {
        val center = center
        val radius = sizePixels / 2f

        // Draw signal waves (background)
        if (animated) {
            drawSignalWaves(
                center = center,
                baseRadius = radius * 0.7f,
                color = primaryColor,
                alpha = waveAlpha,
                scale = waveScale
            )
        }

        // Draw weather station base
        drawWeatherStation(
            center = center,
            radius = radius,
            primaryColor = primaryColor,
            surfaceColor = surfaceColor,
            onSurfaceColor = onSurfaceColor
        )

        // Draw rotating antenna
        if (animated) {
            rotate(antennaRotation, center) {
                drawAntenna(
                    center = center,
                    radius = radius,
                    color = primaryColor
                )
            }
        } else {
            drawAntenna(
                center = center,
                radius = radius,
                color = primaryColor
            )
        }
    }
}

private fun DrawScope.drawSignalWaves(
    center: Offset,
    baseRadius: Float,
    color: Color,
    alpha: Float,
    scale: Float
) {
    val strokeWidth = 2.dp.toPx()

    for (i in 1..3) {
        val waveRadius = (baseRadius * 0.3f * i) * scale
        drawCircle(
            color = color,
            radius = waveRadius,
            center = center,
            alpha = alpha * (1f - i * 0.2f),
            style = Stroke(width = strokeWidth)
        )
    }
}

private fun DrawScope.drawWeatherStation(
    center: Offset,
    radius: Float,
    primaryColor: Color,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    val stationRadius = radius * 0.15f
    val towerHeight = radius * 0.6f
    val towerWidth = stationRadius * 0.3f

    // Draw tower base (circle)
    drawCircle(
        color = primaryColor,
        radius = stationRadius,
        center = Offset(center.x, center.y + towerHeight * 0.3f)
    )

    // Draw tower body (rectangle)
    drawRect(
        color = primaryColor,
        topLeft = Offset(
            center.x - towerWidth / 2,
            center.y - towerHeight * 0.4f
        ),
        size = Size(
            width = towerWidth,
            height = towerHeight
        )
    )

    // Draw sensor housing (small circle on tower)
    drawCircle(
        color = surfaceColor,
        radius = stationRadius * 0.5f,
        center = Offset(center.x, center.y - towerHeight * 0.1f)
    )

    // Draw sensor details (small dots)
    for (i in 0..2) {
        val angle = (i * 120f) * (PI / 180f)
        val sensorX = center.x + cos(angle).toFloat() * stationRadius * 0.3f
        val sensorY = center.y - towerHeight * 0.1f + sin(angle).toFloat() * stationRadius * 0.3f

        drawCircle(
            color = onSurfaceColor,
            radius = 1.5.dp.toPx(),
            center = Offset(sensorX, sensorY)
        )
    }
}

private fun DrawScope.drawAntenna(
    center: Offset,
    radius: Float,
    color: Color
) {
    val antennaLength = radius * 0.4f
    val antennaWidth = 1.5.dp.toPx()

    // Main antenna pole
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius * 0.4f),
        end = Offset(center.x, center.y - radius * 0.8f),
        strokeWidth = antennaWidth
    )

    // Antenna dishes (small circles at the end)
    val dishRadius = radius * 0.08f
    drawCircle(
        color = color,
        radius = dishRadius,
        center = Offset(center.x - dishRadius, center.y - radius * 0.8f),
        style = Stroke(width = antennaWidth)
    )

    drawCircle(
        color = color,
        radius = dishRadius * 0.7f,
        center = Offset(center.x + dishRadius, center.y - radius * 0.75f),
        style = Stroke(width = antennaWidth)
    )

    // Small connecting lines
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius * 0.8f),
        end = Offset(center.x - dishRadius, center.y - radius * 0.8f),
        strokeWidth = antennaWidth * 0.7f
    )

    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius * 0.75f),
        end = Offset(center.x + dishRadius, center.y - radius * 0.75f),
        strokeWidth = antennaWidth * 0.7f
    )
}

/**
 * Static version of the logo for use in places where animation is not desired
 */
@Composable
fun MeteoLogoStatic(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp
) {
    MeteoLogo(
        modifier = modifier,
        size = size,
        animated = false
    )
}