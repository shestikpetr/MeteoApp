package com.shestikpetr.meteoapp.presentation.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.domain.model.Station
import com.shestikpetr.meteoapp.domain.model.StationLatest
import com.shestikpetr.meteoapp.ui.components.AppInput
import com.shestikpetr.meteoapp.ui.components.StatusDot
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors
import com.shestikpetr.meteoapp.ui.util.formatRelative

private const val ACTIVE_FRESH_SECONDS = 24 * 3600L

/**
 * Список станций в стиле meteo-web `.station-list`. Использует bottom-sheet для мобильного,
 * с поиском и StatusDot.
 */
@Composable
fun StationListPanel(
    stations: List<Station>,
    latestByStation: Map<String, StationLatest>,
    activeStationNumber: String?,
    onStationClick: (Station) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appColors
    var search by remember { mutableStateOf("") }
    val now = remember { System.currentTimeMillis() / 1000 }

    val filtered = remember(stations, search) {
        if (search.isBlank()) stations
        else stations.filter { st ->
            val q = search.lowercase()
            st.name.lowercase().contains(q) ||
                    st.stationNumber.lowercase().contains(q) ||
                    (st.location ?: "").lowercase().contains(q)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Станции",
                style = MaterialTheme.typography.titleLarge,
                color = palette.ink
            )
            Text(
                text = "${stations.size}",
                style = MeteoTextStyles.MonoSmall,
                color = palette.ink4
            )
        }

        Box(modifier = Modifier.padding(horizontal = 14.dp)) {
            AppInput(
                value = search,
                onValueChange = { search = it },
                placeholder = "Поиск по имени, серийному, локации",
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = palette.ink4
                    )
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (stations.isEmpty()) "Нет привязанных станций" else "Ничего не найдено",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink3
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)
            ) {
                items(filtered, key = { it.stationNumber }) { station ->
                    val latest = latestByStation[station.stationNumber]
                    val live = latest?.time?.let { now - it in 0..ACTIVE_FRESH_SECONDS } == true
                    StationRow(
                        station = station,
                        live = live,
                        relTime = formatRelative(latest?.time, now),
                        active = station.stationNumber == activeStationNumber,
                        onClick = { onStationClick(station) }
                    )
                }
            }
        }
    }
}

@Composable
private fun StationRow(
    station: Station,
    live: Boolean,
    relTime: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val palette = MaterialTheme.appColors
    val bg = if (active) palette.bgSunken else androidx.compose.ui.graphics.Color.Transparent
    val border = if (active) palette.line2 else androidx.compose.ui.graphics.Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatusDot(live = live)
            Text(
                text = station.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = relTime,
                style = MeteoTextStyles.MonoSmall,
                color = palette.ink3
            )
        }
        Text(
            text = station.stationNumber + (station.location?.let { " · $it" } ?: ""),
            style = MeteoTextStyles.MonoSmall,
            color = palette.ink4,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 16.dp, top = 2.dp)
        )
    }
}
