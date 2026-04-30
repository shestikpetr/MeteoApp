package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.appColors

private val Shape = RoundedCornerShape(6.dp)
private val DefaultPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp)

enum class AppButtonStyle { Primary, Secondary, Ghost, Accent, Danger }

/**
 * Кнопка в стиле meteo-web. По умолчанию `Primary` — тёмная заливка `ink`,
 * `Secondary` — светлая обводка, `Ghost` — без рамки, `Accent` — акцентный синий,
 * `Danger` — красный для деструктивных действий.
 */
@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: AppButtonStyle = AppButtonStyle.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    icon: ImageVector? = null,
    fullWidth: Boolean = false
) {
    val palette = MaterialTheme.appColors

    val container = when (style) {
        AppButtonStyle.Primary -> palette.ink
        AppButtonStyle.Accent -> palette.accent
        AppButtonStyle.Danger -> palette.danger
        AppButtonStyle.Secondary, AppButtonStyle.Ghost -> palette.bgElev
    }
    val onContainer = when (style) {
        AppButtonStyle.Primary -> palette.bgElev
        AppButtonStyle.Accent, AppButtonStyle.Danger -> Color.White
        AppButtonStyle.Secondary, AppButtonStyle.Ghost -> palette.ink
    }
    val borderColor = when (style) {
        AppButtonStyle.Secondary -> palette.line2
        AppButtonStyle.Ghost -> Color.Transparent
        else -> container
    }

    val widthMod = if (fullWidth) Modifier.fillMaxWidth() else Modifier
    val finalModifier = modifier.then(widthMod).height(36.dp)

    val content: @Composable () -> Unit = {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 1.5.dp,
                    color = onContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
            } else if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = onContainer
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }

    when (style) {
        AppButtonStyle.Ghost -> TextButton(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = finalModifier,
            shape = Shape,
            colors = ButtonDefaults.textButtonColors(contentColor = onContainer),
            contentPadding = DefaultPadding
        ) { CompositionLocalProvider(LocalContentColor provides onContainer) { content() } }

        AppButtonStyle.Secondary -> OutlinedButton(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = finalModifier,
            shape = Shape,
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = container,
                contentColor = onContainer
            ),
            border = BorderStroke(1.dp, borderColor),
            contentPadding = DefaultPadding
        ) { CompositionLocalProvider(LocalContentColor provides onContainer) { content() } }

        else -> Button(
            onClick = onClick,
            enabled = enabled && !loading,
            modifier = finalModifier,
            shape = Shape,
            colors = ButtonDefaults.buttonColors(
                containerColor = container,
                contentColor = onContainer
            ),
            contentPadding = DefaultPadding
        ) { CompositionLocalProvider(LocalContentColor provides onContainer) { content() } }
    }
}
