package com.shestikpetr.meteoapp.presentation.statistics.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.shestikpetr.meteoapp.domain.model.TimeSeriesPoint
import com.shestikpetr.meteoapp.ui.theme.appColors

/** Базовый линейный график. Отображает линию по точкам [data] и опциональные доп. линии. */
@Composable
fun LineChart(
    data: List<TimeSeriesPoint>,
    modifier: Modifier = Modifier,
    additionalDatasets: List<Pair<List<TimeSeriesPoint>, Color>> = emptyList(),
    previousPeriodData: List<TimeSeriesPoint> = emptyList(),
    thresholdMin: Double? = null,
    thresholdMax: Double? = null,
    showTrendLine: Boolean = true
) {
    val palette = MaterialTheme.appColors
    if (data.isEmpty()) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val padL = 12.dp.toPx()
        val padR = 12.dp.toPx()
        val padT = 16.dp.toPx()
        val padB = 24.dp.toPx()
        val chartW = w - padL - padR
        val chartH = h - padT - padB

        val allValues = data.map { it.value.toFloat() } +
                additionalDatasets.flatMap { it.first }.map { it.value.toFloat() } +
                previousPeriodData.map { it.value.toFloat() }
        val minV = allValues.min()
        val maxV = allValues.max()
        val rangeV = if (maxV - minV == 0f) 1f else maxV - minV

        // Вспомогательная сетка по горизонтали (4 линии)
        for (i in 1..3) {
            val y = padT + chartH * i / 4f
            drawLine(
                color = palette.line,
                start = Offset(padL, y),
                end = Offset(padL + chartW, y),
                strokeWidth = 1f
            )
        }

        fun toX(idx: Int, total: Int): Float {
            val n = (total - 1).coerceAtLeast(1)
            return padL + chartW * idx / n.toFloat()
        }
        fun toY(value: Float): Float = padT + chartH - (value - minV) / rangeV * chartH

        // Доп. датасеты
        additionalDatasets.forEach { (set, color) ->
            if (set.size < 2) return@forEach
            val path = Path()
            set.forEachIndexed { i, p ->
                val x = toX(i, set.size)
                val y = toY(p.value.toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }

        // Прошлый период (пунктир)
        if (previousPeriodData.size >= 2) {
            val path = Path()
            previousPeriodData.forEachIndexed { i, p ->
                val x = toX(i, previousPeriodData.size)
                val y = toY(p.value.toFloat())
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = palette.ink3.copy(alpha = 0.6f),
                style = Stroke(
                    width = 1.5f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                )
            )
        }

        // Главная серия
        val mainPath = Path()
        data.forEachIndexed { i, p ->
            val x = toX(i, data.size)
            val y = toY(p.value.toFloat())
            if (i == 0) mainPath.moveTo(x, y) else mainPath.lineTo(x, y)
        }
        drawPath(
            path = mainPath,
            color = palette.accent,
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        // Точки
        data.forEachIndexed { i, p ->
            val x = toX(i, data.size)
            val y = toY(p.value.toFloat())
            val outOfRange = (thresholdMin != null && p.value < thresholdMin) ||
                    (thresholdMax != null && p.value > thresholdMax)
            drawCircle(
                color = if (outOfRange) palette.danger else palette.accent,
                radius = 3f,
                center = Offset(x, y)
            )
            drawCircle(color = palette.bgElev, radius = 1.5f, center = Offset(x, y))
        }

        // Линия тренда — упрощённо (от первой точки к последней)
        if (showTrendLine && data.size >= 2) {
            val first = data.first().value.toFloat()
            val last = data.last().value.toFloat()
            drawLine(
                color = palette.warn.copy(alpha = 0.7f),
                start = Offset(toX(0, data.size), toY(first)),
                end = Offset(toX(data.size - 1, data.size), toY(last)),
                strokeWidth = 1.5f,
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f))
            )
        }

        // Пороги
        thresholdMin?.let { thresh ->
            val y = toY(thresh.toFloat())
            drawLine(
                color = palette.accent.copy(alpha = 0.7f),
                start = Offset(padL, y),
                end = Offset(padL + chartW, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
        }
        thresholdMax?.let { thresh ->
            val y = toY(thresh.toFloat())
            drawLine(
                color = palette.danger.copy(alpha = 0.7f),
                start = Offset(padL, y),
                end = Offset(padL + chartW, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f))
            )
        }
    }
}
