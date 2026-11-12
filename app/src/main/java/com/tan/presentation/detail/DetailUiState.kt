package com.tan.presentation.detail

import com.tan.domain.model.Segment
import com.tan.export.TranscriptExporter

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Content(val detail: DetailUi) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

data class DetailUi(
    val title: String,
    val date: String,
    /** "Archivo" o "Grabación". */
    val sourceLabel: String,
    val model: String,
    val segments: List<Segment>
) {
    /** Texto corrido que se muestra en pantalla . */
    val text: String get() = TranscriptExporter.toTxt(segments)
}
