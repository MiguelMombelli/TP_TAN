package com.tan.data.local.entity

/** Resultado de la consulta del Historial: sin cargar el texto completo de cada transcripción. */
data class TranscriptionSummaryRow(
    val id: Long,
    val title: String,
    val createdAtMs: Long,
    val source: String,
    val model: String,
    val durationMs: Long,
    val preview: String
)
