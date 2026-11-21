package com.tan.presentation.loadfile

sealed interface LoadFileUiState {
    data object Empty : LoadFileUiState                        // sin archivo elegido
    data class Selected(
        val fileName: String,
        val details: String,
    ) : LoadFileUiState
    data class Converting(                                      // decodificando a WAV 16 kHz mono
        val fileName: String,
        val details: String,
        val percent: Int,
    ) : LoadFileUiState
    data class Converted(val wavPath: String) : LoadFileUiState // listo: la pantalla navega a Transcribiendo
    data object UnsupportedFormat : LoadFileUiState            // no es audio que Android sepa decodificar
    data class Failed(val message: String) : LoadFileUiState   // no se pudo leer o convertir
}
