package com.tan.presentation.result

import com.tan.domain.model.Segment
import com.tan.export.TranscriptExporter

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Content(val title: String, val segments: List<Segment>) : ResultUiState {
        /** Texto corrido que se muestra en pantalla (el mismo que se exporta a TXT). */
        val text: String get() = TranscriptExporter.toTxt(segments)
    }
    data class Error(val message: String) : ResultUiState
}
