package com.shestikpetr.meteoapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Логотип приложения. Повторяет meteo-web/app/icon.svg и иконку приложения
 * (drawable/ic_launcher_foreground.xml): скруглённый квадрат-фон + белая
 * «M-волна».
 *
 * Path в исходной 24×24 системе:
 *   M6 17  L6 7  L12 13  L18 7  L18 17
 *
 * Линия рисуется со скруглёнными окончаниями и соединениями, чтобы совпадать
 * с stroke-linecap="round" / stroke-linejoin="round" из исходного SVG.
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

        // Фон: скруглённый квадрат (rx ≈ 0.20 от стороны, как в SVG rx=5/24).
        val bgPath = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = s,
                    bottom = s,
                    cornerRadius = CornerRadius(s * 0.20f)
                )
            )
        }
        drawPath(bgPath, color = bg)

        // Глиф: M-волна. Координаты переводятся из 24-системы в s×s.
        val k = s / 24f
        val pts = listOf(
            Offset(6f * k, 17f * k),
            Offset(6f * k, 7f * k),
            Offset(12f * k, 13f * k),
            Offset(18f * k, 7f * k),
            Offset(18f * k, 17f * k)
        )
        val glyphPath = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
        }
        drawPath(
            path = glyphPath,
            color = glyph,
            style = Stroke(
                width = 2.2f * k,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
