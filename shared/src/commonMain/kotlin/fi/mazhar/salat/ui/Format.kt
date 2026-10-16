package fi.mazhar.salat.ui

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Instant

fun Instant.formatTime(timeZone: TimeZone): String {
    val local = toLocalDateTime(timeZone)
    return "${local.hour.pad()}:${local.minute.pad()}"
}

fun Duration.formatCountdown(): String {
    val total = inWholeSeconds.coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return "${h.toInt().pad()}:${m.toInt().pad()}:${s.toInt().pad()}"
}

fun LocalDate.formatLong(): String {
    val weekday = dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    val month = this.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "$weekday, $day $month $year"
}

fun formatCoordinate(latitude: Double, longitude: Double): String {
    val lat = "${roundTo(abs(latitude), 2)}° ${if (latitude >= 0) "N" else "S"}"
    val lng = "${roundTo(abs(longitude), 2)}° ${if (longitude >= 0) "E" else "W"}"
    return "$lat · $lng"
}

fun roundTo(value: Double, decimals: Int): String {
    var factor = 1.0
    repeat(decimals) { factor *= 10 }
    val rounded = kotlin.math.round(value * factor) / factor
    if (decimals == 0) return rounded.toLong().toString()
    val text = rounded.toString()
    val dot = text.indexOf('.')
    if (dot < 0) return text + "." + "0".repeat(decimals)
    val fractional = text.substring(dot + 1).take(decimals).padEnd(decimals, '0')
    return text.substring(0, dot) + "." + fractional
}

fun Int.withThousands(): String {
    val digits = toString()
    return digits.reversed().chunked(3).joinToString(",").reversed()
}

private fun Int.pad(): String = toString().padStart(2, '0')
