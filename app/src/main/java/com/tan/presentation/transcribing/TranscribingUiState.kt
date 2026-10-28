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
}