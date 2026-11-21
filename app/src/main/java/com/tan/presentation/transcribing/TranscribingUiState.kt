package com.tan.presentation.transcribing

sealed interface TranscribingUiState {
    data class InProgress(
        val progress: Float,
        val minutesLeft: Int,
    ) : TranscribingUiState

    data class Error(
        val progress: Float,
        val minutesLeft: Int,
        val message: String = "Error en el proceso de transcripción",
    ) : TranscribingUiState

    /** La transcripción terminó y quedó guardada con este id. */
    data class Done(val transcriptionId: Long) : TranscribingUiState
}
