package com.tan.export

/**
 * Formatea milisegundos como `HH:MM:SS,mmm` (formato de tiempo de SRT).
 */
internal fun formatSrtTime(ms: Long): String {
    val total = if (ms < 0) 0L else ms
    val hours = total / 3_600_000
    val minutes = (total % 3_600_000) / 60_000
    val seconds = (total % 60_000) / 1_000
    val millis = total % 1_000
    return "%02d:%02d:%02d,%03d".format(java.util.Locale.ROOT, hours, minutes, seconds, millis)
}
