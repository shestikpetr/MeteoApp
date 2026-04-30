package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.appColors

/**
 * Сегментированные «pill»-табы: контейнер `bg-sunken` с тонкой рамкой,
 * выделенная вкладка превращается в `bg-elev` с лёгкой тенью.
 * Используется для tabs (.tabs) и stats-toolbar (.seg).
 */
@Composable
fun SegmentedTabs(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    monoLabels: Boolean = false
) {
    val palette = MaterialTheme.appColors

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = palette.bgSunken,
        border = BorderStroke(1.dp, palette.line)
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                val labelStyle = if (monoLabels) {
                    com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles.MonoSmall
                } else {
                    MaterialTheme.typography.labelLarge
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) palette.bgElev else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelected(index) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = labelStyle.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) palette.ink else palette.ink3
                        )
                    )
                }
            }
        }
    }
}

/**
 * То же самое, но фиксированной шириной (распределяется поровну) — для
 * табов «Карта/Статистика/Профиль» в верхней навигации.
 */
@Composable
fun SegmentedTabsEqual(
    options: List<String>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MaterialTheme.appColors
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = palette.bgSunken,
        border = BorderStroke(1.dp, palette.line)
    ) {
        Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            options.forEachIndexed { index, label ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(5.dp))
                        .background(if (isSelected) palette.bgElev else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelected(index) }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                            color = if (isSelected) palette.ink else palette.ink3
                        )
                    )
                }
            }
        }
    }
}
