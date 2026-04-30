package com.shestikpetr.meteoapp.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Compress
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Подбирает иконку параметра по единице измерения.
 *
 * Сопоставление основано на физическом смысле единицы измерения, а не на коде/имени параметра:
 * это устойчиво к локализации и переименованию параметров на сервере.
 */
fun getParameterIcon(unit: String?): ImageVector {
    if (unit.isNullOrBlank()) return Icons.AutoMirrored.Filled.ShowChart

    // Нормализуем: убираем пробелы, приводим к нижнему регистру.
    // Заменяем кириллический "С" (Цельсий записывается как "°С" с кириллической буквой) на латинский,
    // а также мю-символ (микро) на латинский u — встречается в "мкг/м³".
    val u = unit.trim().lowercase()
        .replace('\u0421', 'c') // кир. С -> лат. c
        .replace('\u00b5', 'u') // µ -> u

    return when {
        // Температура: °C, °F, K, °С (кир.)
        u.contains("°c") || u.contains("°f") || u == "k" || u.contains(" k") -> Icons.Default.Thermostat
        // Влажность: %
        u == "%" || u.contains("%") -> Icons.Default.WaterDrop
        // Давление: Pa, hPa, kPa, mmHg, mbar, мм рт. ст., гПа, мбар
        u.contains("pa") || u.contains("па") ||
                u.contains("mmhg") || u.contains("мм рт") ||
                u.contains("bar") || u.contains("бар") ||
                u.contains("torr") -> Icons.Default.Compress
        // Скорость ветра: m/s, км/ч, mph, knots, узл
        u.contains("m/s") || u.contains("м/с") ||
                u.contains("km/h") || u.contains("км/ч") ||
                u.contains("mph") || u.contains("knot") || u.contains("узл") -> Icons.Default.Air
        // Направление ветра: ° (градусы без °C/°F), румбы
        u == "°" || u == "deg" || u == "град" -> Icons.Default.Explore
        // Осадки: mm, мм
        u == "mm" || u == "мм" || u.startsWith("mm/") || u.startsWith("мм/") -> Icons.Default.Opacity
        // Солнечная радиация / освещённость: W/m², Вт/м², lux, лк, lx
        u.contains("w/m") || u.contains("вт/м") ||
                u == "lux" || u == "lx" || u == "лк" -> Icons.Default.WbSunny
        // Электрические величины: В, V, A, Ом, ohm
        u == "v" || u == "в" || u == "a" || u == "а" ||
                u == "ом" || u.contains("ohm") -> Icons.Default.Bolt
        // Расход / частота / прочие интенсивности
        u.contains("/s") || u.contains("/с") || u.contains("hz") || u.contains("гц") -> Icons.Default.Speed
        else -> Icons.AutoMirrored.Filled.ShowChart
    }
}
