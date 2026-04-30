package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.appColors

private val CardShape = RoundedCornerShape(8.dp)

/**
 * Карточка в стиле meteo-web: bg-elev фон, 1px line-граница, без тени,
 * радиус 8dp. Ни в коем случае не CardElevation — мы делаем «edge-style».
 */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(14.dp),
    content: @Composable () -> Unit
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = modifier,
        color = palette.bgElev,
        contentColor = palette.ink,
        shape = CardShape,
        border = BorderStroke(1.dp, palette.line)
    ) {
        Column(modifier = Modifier.padding(contentPadding)) { content() }
    }
}

/**
 * Карточка с заголовком сверху, разделённая тонкой полосой `line`.
 * Тело уходит в `body` без внутренних паддингов — задаются через `bodyPadding`.
 */
@Composable
fun AppCardWithHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    headerActions: @Composable () -> Unit = {},
    bodyPadding: PaddingValues = PaddingValues(14.dp),
    body: @Composable () -> Unit
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = modifier,
        color = palette.bgElev,
        contentColor = palette.ink,
        shape = CardShape,
        border = BorderStroke(1.dp, palette.line)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = title, style = MaterialTheme.typography.titleLarge)
                    subtitle?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.ink3
                        )
                    }
                }
                headerActions()
            }
            HorizontalDivider(color = palette.line, thickness = 1.dp)
            Column(modifier = Modifier.padding(bodyPadding)) { body() }
        }
    }
}
