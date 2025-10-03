package com.shestikpetr.meteo.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

/**
 * Minimal button with subtle animations.
 * Features:
 * - Smooth scale animation on press
 * - Loading state support
 * - Material3 design
 * - Consistent styling across the app
 */
@Composable
fun MinimalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = if (enabled && !loading) 1f else 0.95f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )

    val alpha by animateFloatAsState(
        targetValue = if (enabled && !loading) 1f else 0.6f,
        animationSpec = tween(300),
        label = "button_alpha"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale
            )
            .alpha(alpha),
        enabled = enabled && !loading,
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onPrimary
            )
        } else {
            content()
        }
    }
}