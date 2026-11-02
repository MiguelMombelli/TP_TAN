package com.tan.domain.model

/**
 * Los tiempos están en milisegundos desde el inicio del audio.
 */
data class Segment(
    val startMs: Long,
    val endMs: Long,
    val text: String
)
