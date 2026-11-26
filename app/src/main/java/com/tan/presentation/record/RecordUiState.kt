package com.tan.presentation.record

sealed interface RecordUiState {
    data object Idle : RecordUiState                       // "Toca para grabar"
    data class Recording(val seconds: Int) : RecordUiState // 00:10
    data class Saved(val wavPath: String) : RecordUiState  // "Grabación guardada"; wavPath es el WAV temporal
    data object PermissionDenied : RecordUiState           // no está en el Figma
    data class Failed(val message: String) : RecordUiState // no está en el Figma
}