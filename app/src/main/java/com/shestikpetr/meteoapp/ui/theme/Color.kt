package com.shestikpetr.meteoapp.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Палитра meteo-web. Цвета взяты из app/globals.css:
 * --bg, --bg-elev, --bg-sunken, --ink, --ink-2..4, --line, --line-2,
 * --accent, --accent-soft, --accent-ink, --warn, --danger, --ok.
 *
 * oklch значения переведены в sRGB-приближение (визуально совпадают).
 */
object MeteoColors {

    // ===== Light =====
    object Light {
        val Bg = Color(0xFFFAFAFA)
        val BgElev = Color(0xFFFFFFFF)
        val BgSunken = Color(0xFFF3F3F4)

        val Ink = Color(0xFF0E0F12)
        val Ink2 = Color(0xFF3A3D45)
        val Ink3 = Color(0xFF6B6F78)
        val Ink4 = Color(0xFF9CA0A8)

        val Line = Color(0xFFE7E7EA)
        val Line2 = Color(0xFFD8D9DD)

        // accent: oklch(0.58 0.16 240) -> примерно #2F6BCB
        val Accent = Color(0xFF2F6BCB)
        // accent-soft: oklch(0.94 0.04 240) -> очень светлый голубой
        val AccentSoft = Color(0xFFE3ECF8)
        // accent-ink: oklch(0.42 0.15 240) -> приглушённый тёмно-синий
        val AccentInk = Color(0xFF1F4D8C)

        // warn: oklch(0.7 0.15 60) -> янтарный
        val Warn = Color(0xFFE08C2A)
        // danger: oklch(0.6 0.2 25) -> красно-малиновый
        val Danger = Color(0xFFD2422E)
        // ok: oklch(0.65 0.14 150) -> зелёный
        val Ok = Color(0xFF3FA268)
    }

    // ===== Dark =====
    object Dark {
        val Bg = Color(0xFF0C0D10)
        val BgElev = Color(0xFF131418)
        val BgSunken = Color(0xFF08090B)

        val Ink = Color(0xFFECECEF)
        val Ink2 = Color(0xFFC4C6CC)
        val Ink3 = Color(0xFF8A8D96)
        val Ink4 = Color(0xFF5B5E67)

        val Line = Color(0xFF1F2026)
        val Line2 = Color(0xFF2A2C33)

        // accent: oklch(0.72 0.16 240) -> голубее, светлее
        val Accent = Color(0xFF66A6F2)
        val AccentSoft = Color(0xFF1B2D4A)
        val AccentInk = Color(0xFFB6D2F5)

        val Warn = Color(0xFFE8A047)
        val Danger = Color(0xFFE06A55)
        val Ok = Color(0xFF5BB987)
    }
}
