package com.tan.domain.model

import com.tan.export.TranscriptExporter

/** De dónde salió el audio. */
enum class TranscriptionSource { RECORDING, FILE }

/** Una transcripción completa, con sus segmentos. Es lo que ven Resultado y Detalle. */
data class Transcription(
    val id: Long = 0,
    val title: String,
    val createdAtMs: Long,
    val source: TranscriptionSource,
    /** Identificador del modelo usado (por ejemplo "tiny" o "base"). */
    val model: String,
    val durationMs: Long,
    val segments: List<Segment>
) {
    /** Texto corrido de la transcripción (el mismo que se exporta a TXT). */
    val text: String get() = TranscriptExporter.toTxt(segments)
}

/** Versión liviana para la lista del Historial. */
data class TranscriptionSummary(
    val id: Long,
    val title: String,
    val createdAtMs: Long,
    val source: TranscriptionSource,
    val model: String,
    val durationMs: Long,
    val preview: String
)
