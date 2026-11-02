package com.tan.presentation.record

sealed interface RecordUiState {
    data object Idle : RecordUiState                       // "Toca para grabar"
    data class Recording(val seconds: Int) : RecordUiState // 00:10
    data object Saved : RecordUiState                      // "Grabación guardada"
    data object PermissionDenied : RecordUiState           // no está en el Figma
}