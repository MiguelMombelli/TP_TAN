package com.tan.presentation.detail

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
    val text: String
)
