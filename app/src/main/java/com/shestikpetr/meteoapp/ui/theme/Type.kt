package com.shestikpetr.meteoapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Типографика в духе meteo-web:
 * — sans-serif для текста, monospace для чисел/кодов/времени
 * — плотные межстрочные, отрицательный letter-spacing на крупных заголовках
 * — лейблы UPPERCASE с расширенным трекингом (см. MeteoTextStyles.label)
 *
 * Inter / JetBrains Mono можно подключить через resources, но даже на системных
 * sans/monospace дизайн читается узнаваемо.
 */
private val Sans: FontFamily = FontFamily.SansSerif
val MonoFamily: FontFamily = FontFamily.Monospace

private val tightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

val MeteoTypography: Typography = Typography(
    // Большие герои: detail.temp, h1 на статистике (упрощённое сопоставление)
    displayLarge = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02).em
    ),
    displayMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.02).em
    ),

    // h1, h2 в meteo-web
    headlineLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.02).em
    ),
    headlineMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.02).em
    ),
    headlineSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.01).em
    ),

    // Заголовки секций / карточек
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 14.5.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 13.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    titleSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),

    // Основной текст
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    ),

    // Кнопки, чипы
    labelLarge = TextStyle(
        fontFamily = Sans,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelMedium = TextStyle(
        fontFamily = Sans,
        fontSize = 12.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium
    )
)

/**
 * Дополнительные стили, которых нет в Material Typography:
 * — eyebrow / label: UPPERCASE, расширенный трекинг
 * — mono: моноширинный для чисел, временных меток
 */
object MeteoTextStyles {
    val Eyebrow = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.08.em
    )

    val Label = TextStyle(
        fontFamily = Sans,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.04.em
    )

    val Mono = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal
    )

    val MonoSmall = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal
    )

    val MonoLink = TextStyle(
        fontFamily = MonoFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        textDecoration = TextDecoration.None
    )
}
