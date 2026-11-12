package com.tan.presentation.transcribing

sealed interface TranscribingUiState {
    data class InProgress(
        val progress: Float,
        val minutesLeft: Int,
    ) : TranscribingUiState

    data class Error(
        val progress: Float,
        val minutesLeft: Int,
    ) : TranscribingUiState

    /** La transcripción terminó y quedó guardada con este id. */
    data class Done(val transcriptionId: Long) : TranscribingUiState
}
