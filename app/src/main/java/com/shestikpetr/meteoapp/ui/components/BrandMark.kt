package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Логотип приложения: квадратик со скруглёнными углами + волнообразная «глифа»
 * (метео-волна). Стилистика повторяет SVG из meteo-web /components/BrandMark.tsx.
 */
@Composable
fun BrandMark(
    size: Dp = 24.dp,
    bg: Color,
    glyph: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(size)) {
        val s = this.size.minDimension
        // Скруглённый квадрат
        val path = Path().apply {
            val r = s * 0.18f
            addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f, top = 0f, right = s, bottom = s,
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(r)
                )
            )
        }
        drawPath(path, color = bg)

        // Волна — три синусоидальных гребня
        val wave = Path().apply {
            val cy = s * 0.55f
            val a = s * 0.12f
            val seg = s / 6f
            moveTo(s * 0.18f, cy)
            cubicTo(
                s * 0.30f, cy - a,
                s * 0.40f, cy + a,
                s * 0.50f, cy
            )
            cubicTo(
                s * 0.60f, cy - a,
                s * 0.70f, cy + a,
                s * 0.82f, cy
            )
        }
        drawPath(
            wave,
            color = glyph,
            style = Stroke(width = s * 0.06f, cap = StrokeCap.Round)
        )

        // Маленькая точка солнца сверху слева
        drawCircle(
            color = glyph,
            radius = s * 0.06f,
            center = Offset(s * 0.32f, s * 0.30f)
        )
    }
}
