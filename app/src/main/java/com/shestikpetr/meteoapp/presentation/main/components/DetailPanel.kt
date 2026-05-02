package com.shestikpetr.meteoapp.presentation.main.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val scroll = androidx.compose.foundation.rememberScrollState()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.bgElev,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, palette.line)
    ) {
        // Высота карточки ограничена примерно половиной экрана, чтобы при большом
        // количестве параметров содержимое скроллилось, а не прижимало низ карты.
        val maxHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp * 0.6f
        Column(
            modifier = Modifier
                .heightIn(max = maxHeight)
                .verticalScroll(scroll)
        ) {
            // Box с fillMaxWidth, чтобы Alignment.TopEnd прижимал крестик к правому
            // краю карточки, а не к правому краю внутреннего Column.
            Box(modifier = Modifier.fillMaxWidth()) {
                // Дополнительный правый отступ резервирует место под крестик,
                // чтобы длинные имена и ellipsis не попадали под кнопку.
                Column(
                    modifier = Modifier.padding(
                        start = 18.dp,
                        end = 52.dp,
                        top = 16.dp,
                        bottom = 16.dp
                    )
                ) {
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
                    val tempParam = latest.parameters.firstOrNull { it.code == TEMPERATURE_CODE }
                    val temp = tempParam?.value
                    val showTempHero = temp != null && TEMPERATURE_CODE !in hiddenParameterCodes
                    if (showTempHero) {
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
                // Группируем параметры по name. Если у станции несколько датчиков
                // одного типа (например, температура воздуха/грунта/воды) —
                // они показываются одной раскрывающейся группой.
                val groups = visible.groupBy { it.name }
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    groups.entries.forEachIndexed { idx, (_, items) ->
                        if (idx > 0) Spacer(Modifier.height(6.dp))
                        if (items.size == 1) {
                            ParamRow(param = items[0])
                        } else {
                            ParamGroup(items = items)
                        }
                    }
                }
            }

            HorizontalDivider(color = palette.line)
            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                AppButton(
                    text = "В статистику →",
                    onClick = onOpenStatistics,
                    style = AppButtonStyle.Primary,
                    fullWidth = true
                )
            }
        }
    }
}

/**
 * Одиночный параметр: лейбл + описание слева, значение и единица — крупно справа.
 * Используется и для одиночных параметров, и для дочерних строк раскрытой группы.
 */
@Composable
private fun ParamRow(
    param: com.shestikpetr.meteoapp.domain.model.ParameterReading,
    showName: Boolean = true,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appColors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showName) {
            Icon(
                imageVector = getParameterIcon(param.unit),
                contentDescription = null,
                tint = palette.ink3,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(6.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = param.name.uppercase(),
                style = MeteoTextStyles.Label,
                color = palette.ink4,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            param.description?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = palette.ink4,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = param.value?.formatParameterValue() ?: "—",
                style = MeteoTextStyles.Mono.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                color = palette.ink
            )
            param.unit?.takeIf { it.isNotBlank() }?.let {
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

/**
 * Группа параметров с одинаковым именем — раскрывающаяся.
 * В свёрнутом виде: имя + бейдж количества.
 * В развёрнутом: список ParamRow с описанием каждого датчика.
 */
@Composable
private fun ParamGroup(
    items: List<com.shestikpetr.meteoapp.domain.model.ParameterReading>
) {
    val palette = MaterialTheme.appColors
    val first = items.first()
    var expanded by rememberSaveable(first.name) { mutableStateOf(false) }
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = getParameterIcon(first.unit),
                    contentDescription = null,
                    tint = palette.ink3,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = first.name.uppercase(),
                    style = MeteoTextStyles.Label,
                    color = palette.ink4,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "${items.size}",
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink4
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = palette.ink3,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 14.dp, top = 2.dp, bottom = 2.dp)) {
                items.forEach { p ->
                    // Подпись «дочерки» — берём описание (например, «Воздух»);
                    // если описания нет, в подписи останется только сам name.
                    val sub = p.copy(name = p.description?.takeIf { it.isNotBlank() } ?: "Датчик #${p.code}", description = null)
                    ParamRow(param = sub, showName = false)
                }
            }
        }
    }
}

private fun Double.format4(): String = String.format(java.util.Locale.US, "%.4f", this)
