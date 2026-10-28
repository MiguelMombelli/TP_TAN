package com.tan.presentation.loadfile

sealed interface LoadFileUiState {
    data object Empty : LoadFileUiState                        // sin archivo elegido
    data class Selected(
        val fileName: String,
        val details: String,
    ) : LoadFileUiState
    data object UnsupportedFormat : LoadFileUiState            // no está en el Figma
}