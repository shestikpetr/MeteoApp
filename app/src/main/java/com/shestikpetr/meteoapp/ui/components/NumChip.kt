package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.ui.theme.MeteoTextStyles
import com.shestikpetr.meteoapp.ui.theme.appColors

/**
 * Чип со счётчиком позиции выбора. Соответствует .numchip из meteo-web.
 *
 * @param index 0-based позиция в выбранных. Если null — чип не выбран.
 * @param swatchColor цвет, который будет залит в счётчик — обычно из палитры графика.
 * @param sub справа: короткий моно-текст (например, относительное время).
 */
@Composable
fun NumChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    index: Int? = null,
    swatchColor: Color? = null,
    desc: String? = null,
    sub: String? = null,
    isPrimary: Boolean = false
) {
    val palette = MaterialTheme.appColors
    val isSelected = index != null
    val containerColor = if (isSelected) palette.bgSunken else Color.Transparent
    val border = if (isSelected) BorderStroke(1.dp, palette.line) else null

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() },
        color = containerColor,
        border = border,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Квадратик-счётчик (или dashed при невыбранном)
            Box(
                modifier = Modifier.size(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    val chipColor = swatchColor ?: palette.ink
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .background(chipColor, RoundedCornerShape(5.dp))
                    )
                    Text(
                        text = "${index!! + 1}",
                        style = MeteoTextStyles.MonoSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    )
                    if (isPrimary) {
                        // Двойной outline для primary
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.Transparent)
                        )
                    }
                } else {
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = RoundedCornerShape(5.dp),
                        color = palette.bgElev,
                        border = BorderStroke(1.5.dp, palette.line2)
                    ) {}
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                desc?.let {
                    Text(
                        text = it,
                        style = MeteoTextStyles.MonoSmall,
                        color = palette.ink4,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            sub?.let {
                Text(
                    text = it,
                    style = MeteoTextStyles.MonoSmall,
                    color = palette.ink4,
                    maxLines = 1
                )
            }
        }
    }
}
