package com.shestikpetr.meteoapp.ui.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** «25.3» вместо «25.300000…». */
fun Double.formatParameterValue(): String = String.format(Locale.US, "%.1f", this)

private val DATE_TIME = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
private val DATE_ONLY = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
private val TIME_ONLY = SimpleDateFormat("HH:mm", Locale.getDefault())

fun formatDateTime(epochSeconds: Long?): String =
    if (epochSeconds == null) "—" else DATE_TIME.format(Date(epochSeconds * 1000))

fun formatDate(epochSeconds: Long?): String =
    if (epochSeconds == null) "—" else DATE_ONLY.format(Date(epochSeconds * 1000))

fun formatTime(epochSeconds: Long?): String =
    if (epochSeconds == null) "—" else TIME_ONLY.format(Date(epochSeconds * 1000))

/**
 * «3 мин назад», «вчера», «давно». Возвращает «—» если время неизвестно.
 * Соответствует formatRelative из meteo-web/lib/format.ts.
 */
fun formatRelative(epochSeconds: Long?, nowSeconds: Long): String {
    if (epochSeconds == null) return "—"
    val diff = nowSeconds - epochSeconds
    return when {
        diff < 0 -> "только что"
        diff < 60 -> "только что"
        diff < 3600 -> "${diff / 60} мин"
        diff < 24 * 3600 -> "${diff / 3600} ч"
        diff < 7 * 24 * 3600 -> "${diff / (24 * 3600)} д"
        diff < 30 * 24 * 3600 -> "${diff / (7 * 24 * 3600)} нед"
        else -> "давно"
    }
}
