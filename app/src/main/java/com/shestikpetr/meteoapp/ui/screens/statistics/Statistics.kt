package com.shestikpetr.meteoapp.ui.screens.statistics

import com.shestikpetr.meteoapp.data.model.TimeSeriesDataPoint
import kotlin.math.sqrt

data class SeriesStats(
    val min: Double,
    val max: Double,
    val avg: Double,
    val median: Double,
    val stdDev: Double,
    val firstValue: Double,
    val lastValue: Double,
    val absoluteChange: Double,
    val percentChange: Double
)

fun computeSeriesStats(values: List<Double>): SeriesStats {
    if (values.isEmpty()) {
        return SeriesStats(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    }

    val min = values.min()
    val max = values.max()
    val avg = values.average()

    val sorted = values.sorted()
    val median = if (sorted.size % 2 == 0) {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
    } else {
        sorted[sorted.size / 2]
    }

    val stdDev = if (values.size > 1) {
        val variance = values.map { (it - avg) * (it - avg) }.average()
        sqrt(variance)
    } else 0.0

    val firstValue = values.first()
    val lastValue = values.last()
    val absoluteChange = lastValue - firstValue
    val percentChange = if (firstValue != 0.0) absoluteChange / firstValue * 100 else 0.0

    return SeriesStats(min, max, avg, median, stdDev, firstValue, lastValue, absoluteChange, percentChange)
}

/**
 * Возвращает Y-значения линии тренда (МНК-регрессия) для первой и последней точки.
 * null, если точек меньше двух.
 */
fun computeTrendLine(data: List<TimeSeriesDataPoint>): Pair<Float, Float>? {
    if (data.size < 2) return null

    val n = data.size
    val xMean = (n - 1) / 2.0
    val yMean = data.map { it.value }.average()

    var numerator = 0.0
    var denominator = 0.0
    data.forEachIndexed { index, point ->
        val xDiff = index - xMean
        val yDiff = point.value - yMean
        numerator += xDiff * yDiff
        denominator += xDiff * xDiff
    }

    val slope = if (denominator != 0.0) numerator / denominator else 0.0
    val intercept = yMean - slope * xMean
    return Pair(intercept.toFloat(), (slope * (n - 1) + intercept).toFloat())
}

/**
 * Коэффициент корреляции Пирсона. null если данных недостаточно.
 */
fun computePearsonCorrelation(
    xValues: List<Double>,
    yValues: List<Double>
): Double? {
    val n = minOf(xValues.size, yValues.size)
    if (n < 3) return null

    val xs = xValues.take(n)
    val ys = yValues.take(n)
    val xMean = xs.average()
    val yMean = ys.average()

    var numerator = 0.0
    var denomX = 0.0
    var denomY = 0.0
    for (i in 0 until n) {
        val xDiff = xs[i] - xMean
        val yDiff = ys[i] - yMean
        numerator += xDiff * yDiff
        denomX += xDiff * xDiff
        denomY += yDiff * yDiff
    }

    val denominator = sqrt(denomX * denomY)
    return if (denominator == 0.0) 0.0 else numerator / denominator
}
