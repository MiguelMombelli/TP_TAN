package com.tan.presentation.history

sealed interface HistoryUiState {
    data object Loading : HistoryUiState
    data object Empty : HistoryUiState
    data class Content(val items: List<HistoryItemUi>) : HistoryUiState
    data class Error(val message: String) : HistoryUiState
}

data class HistoryItemUi(
    val id: Long,
    val title: String,
    val date: String,
    /** "Archivo" o "Grabación". */
    val sourceLabel: String,
    /** Modelo usado, por ejemplo "tiny". */
    val model: String,
    val preview: String
)