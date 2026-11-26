package com.tan.data.local.mapper

import com.tan.data.local.entity.SegmentEntity
import com.tan.data.local.entity.TranscriptionEntity
import com.tan.data.local.entity.TranscriptionSummaryRow
import com.tan.domain.model.Segment
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import com.tan.domain.model.TranscriptionSummary

/** Siempre arma una fila nueva (id = 0): Room asigna el id al insertar. */
fun Transcription.toEntity(): TranscriptionEntity = TranscriptionEntity(
    title = title,
    createdAtMs = createdAtMs,
    source = source.name,
    model = model,
    durationMs = durationMs,
    text = text
)

/** El `transcriptionId` queda en 0: el DAO lo completa al guardar. */
fun List<Segment>.toEntities(): List<SegmentEntity> = mapIndexed { index, segment ->
    SegmentEntity(
        transcriptionId = 0,
        position = index,
        startMs = segment.startMs,
        endMs = segment.endMs,
        text = segment.text
    )
}

fun SegmentEntity.toDomain(): Segment = Segment(startMs = startMs, endMs = endMs, text = text)

fun TranscriptionEntity.toDomain(segments: List<SegmentEntity>): Transcription = Transcription(
    id = id,
    title = title,
    createdAtMs = createdAtMs,
    source = source.toSource(),
    model = model,
    durationMs = durationMs,
    segments = segments.sortedBy { it.position }.map { it.toDomain() }
)

fun TranscriptionSummaryRow.toDomain(): TranscriptionSummary = TranscriptionSummary(
    id = id,
    title = title,
    createdAtMs = createdAtMs,
    source = source.toSource(),
    model = model,
    durationMs = durationMs,
    preview = preview
)

/** Si algún día aparece un valor desconocido en la base, no se rompe la app: se asume FILE. */
private fun String.toSource(): TranscriptionSource =
    TranscriptionSource.entries.firstOrNull { it.name == this } ?: TranscriptionSource.FILE
