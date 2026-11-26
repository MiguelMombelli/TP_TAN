package com.tan.presentation.format

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT)

/** "17/04/2026" */
fun formatDate(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE.withZone(zone).format(Instant.ofEpochMilli(epochMs))

/** "17/04/2026 21:15" */
fun formatDateTime(epochMs: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    DATE_TIME.withZone(zone).format(Instant.ofEpochMilli(epochMs))
