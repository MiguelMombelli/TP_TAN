package com.tan.presentation.result

sealed interface ResultUiState {
    data object Loading : ResultUiState
    data class Content(val title: String, val text: String) : ResultUiState
    data class Error(val message: String) : ResultUiState
}
