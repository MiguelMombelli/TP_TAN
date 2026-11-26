package com.tan.presentation.transcribing

sealed interface TranscribingUiState {
    data class InProgress(
        val progress: Float,
        val minutesLeft: Int,
        /**
         * true si no se puede informar un porcentaje real: Whisper avisa el avance recién al terminar
         * cada ventana de 30 s, así que en un audio corto saltaría de 0 % a 100 %.
         */
        val indeterminate: Boolean = false,
    ) : TranscribingUiState

    data class Error(
        val progress: Float,
        val minutesLeft: Int,
        val message: String = "Error en el proceso de transcripción",
        /** true si el error se arregla descargando un modelo: el diálogo lleva directo a Modelos. */
        val needsModel: Boolean = false,
    ) : TranscribingUiState

    /** La transcripción terminó y quedó guardada. El aviso para navegar va aparte, como evento. */
    data object Done : TranscribingUiState
}
