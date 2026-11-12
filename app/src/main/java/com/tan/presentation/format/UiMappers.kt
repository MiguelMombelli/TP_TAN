package com.tan.presentation.format

import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import com.tan.domain.model.TranscriptionSummary
import com.tan.presentation.detail.DetailUi
import com.tan.presentation.history.HistoryItemUi
import com.tan.presentation.history.HistoryUiState
import com.tan.presentation.result.ResultUiState
import java.time.ZoneId

/** Texto de la etiqueta de origen que se muestra en pantalla. */
fun TranscriptionSource.label(): String = when (this) {
    TranscriptionSource.RECORDING -> "Grabación"
    TranscriptionSource.FILE -> "Archivo"
}

/** Título por defecto de una transcripción nueva, por ejemplo "Grabación 17/04/2026 21:15". */
fun defaultTitle(
    source: TranscriptionSource,
    epochMs: Long,
    zone: ZoneId = ZoneId.systemDefault()
): String = "${source.label()} ${formatDateTime(epochMs, zone)}"

fun TranscriptionSummary.toUi(zone: ZoneId = ZoneId.systemDefault()): HistoryItemUi = HistoryItemUi(
    id = id,
    title = title,
    date = formatDate(createdAtMs, zone),
    sourceLabel = source.label(),
    model = model,
    preview = preview
)

fun List<TranscriptionSummary>.toHistoryState(zone: ZoneId = ZoneId.systemDefault()): HistoryUiState =
    if (isEmpty()) HistoryUiState.Empty else HistoryUiState.Content(map { it.toUi(zone) })

fun Transcription.toDetailUi(zone: ZoneId = ZoneId.systemDefault()): DetailUi = DetailUi(
    title = title,
    date = formatDate(createdAtMs, zone),
    sourceLabel = source.label(),
    model = model,
    segments = segments
)

fun Transcription.toResultContent(): ResultUiState.Content =
    ResultUiState.Content(title = title, segments = segments)
