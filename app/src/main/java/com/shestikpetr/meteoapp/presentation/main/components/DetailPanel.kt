package com.shestikpetr.meteoapp.presentation.main.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.ui.components.AppButton
import com.shestikpetr.meteoapp.ui.components.AppButtonStyle
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors
import com.shestikpetr.meteoapp.ui.util.formatDateTime
import com.shestikpetr.meteoapp.ui.util.formatParameterValue
import com.shestikpetr.meteoapp.ui.util.getParameterIcon

private const val TEMPERATURE_CODE = 1

/**
 * Карточка с деталями станции: координаты, имя, температура крупно, сетка параметров.
 */
@Composable
fun DetailPanel(
    latest: StationLatest,
    hiddenParameterCodes: Set<Int>,
    onClose: () -> Unit,
    onOpenStatistics: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.bgElev,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.line)
    ) {
        Column {
            Box {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text(
                        text = if (latest.latitude != null && latest.longitude != null)
                            "${latest.latitude.format4()}°, ${latest.longitude.format4()}°"
                        else "координаты неизвестны",
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink4
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = latest.name,
                        style = MaterialTheme.typography.headlineSmall,
                        color = palette.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = listOfNotNull(latest.location, latest.stationNumber).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink3
                    )
                    val temp = latest.parameters.firstOrNull { it.code == TEMPERATURE_CODE }?.value
                    if (temp != null) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = temp.formatParameterValue(),
                                style = MaterialTheme.typography.displayLarge,
                                color = palette.ink
                            )
                            Text(
                                text = "°C",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
                                color = palette.ink3,
                                modifier = Modifier.padding(start = 2.dp, bottom = 6.dp)
                            )
                        }
                    }
                    if (latest.time != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "обновлено ${formatDateTime(latest.time)}",
                            style = MeteoTextStyles.MonoSmall,
                            color = palette.ink4
                        )
                    }
                }
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(28.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = palette.ink3,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(color = palette.line)

            val visible = latest.parameters.filter {
                it.code != TEMPERATURE_CODE && it.code !in hiddenParameterCodes
            }
            val hasAny = latest.parameters.any { it.value != null }
            if (!hasAny) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(palette.warn.copy(alpha = 0.10f))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Станция не передаёт свежих показаний.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.ink2
                    )
                }
            }

            if (visible.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Нет других показаний.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.ink3
                    )
                }
            } else {
                Column(modifier = Modifier.padding(8.dp)) {
                    visible.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            row.forEach { param ->
                                ParamCell(
                                    name = param.name,
                                    value = param.value,
                                    unit = param.unit,
                                    description = param.description,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Если в строке нечётный — заполнитель
                            if (row.size == 1) Box(modifier = Modifier.weight(1f)) {}
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.line)
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                AppButton(
                    text = "В статистику →",
                    onClick = onOpenStatistics,
                    style = AppButtonStyle.Primary
                )
            }
        }
    }
}

@Composable
private fun ParamCell(
    name: String,
    value: Double?,
    unit: String?,
    description: String?,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appColors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
                imageVector = getParameterIcon(unit),
                contentDescription = null,
                tint = palette.ink3,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = name.uppercase(),
                style = MeteoTextStyles.Label,
                color = palette.ink4,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = palette.ink4,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value?.formatParameterValue() ?: "—",
                style = MeteoTextStyles.Mono.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                color = palette.ink
            )
            unit?.let {
                Text(
                    text = it,
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink3,
                    modifier = Modifier.padding(start = 3.dp, bottom = 2.dp)
                )
            }
        }
    }
}

private fun Double.format4(): String = String.format(java.util.Locale.US, "%.4f", this)
