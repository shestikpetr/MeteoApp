package com.shestikpetr.meteoapp.ui.components

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.appColors

/**
 * Верхняя навигация в стиле meteo-web `.topbar`:
 * — высота 56dp, фон bg-elev, нижняя граница `line`
 * — слева: лого + название
 * — по центру: pill-табы
 * — справа: пользователь (имя + аватар) или slot-actions
 */
@Composable
fun AppTopBar(
    selectedTab: TopBarTab,
    onTabSelected: (TopBarTab) -> Unit,
    modifier: Modifier = Modifier,
    username: String? = null,
    onUserClick: (() -> Unit)? = null,
    actions: @Composable () -> Unit = {}
) {
    val palette = MaterialTheme.appColors
    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(palette.bgElev)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Бренд слева
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            ) {
                BrandMark(size = 22.dp, bg = palette.ink, glyph = palette.bgElev)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Meteo",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.ink3,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    )
                    Text(
                        text = "App",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            // Pill-табы по центру
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                val tabs = TopBarTab.entries
                SegmentedTabs(
                    options = tabs.map { it.label },
                    selectedIndex = tabs.indexOf(selectedTab),
                    onSelected = { onTabSelected(tabs[it]) }
                )
            }

            // Right slot
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions()
                if (username != null && onUserClick != null) {
                    UserPill(username = username, onClick = onUserClick)
                }
            }
        }
        HorizontalDivider(color = palette.line, thickness = 1.dp)
    }
}

enum class TopBarTab(val label: String) {
    Map("Карта"),
    Stats("Статистика"),
    Profile("Профиль")
}

@Composable
private fun UserPill(username: String, onClick: () -> Unit) {
    val palette = MaterialTheme.appColors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(palette.bgElev)
            .clickable { onClick() }
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = username,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 120.dp)
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(palette.bgSunken),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = username.firstOrNull()?.uppercase() ?: "?",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = palette.ink2
            )
        }
    }
}

