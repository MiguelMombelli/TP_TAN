package com.tan.domain.audio

import kotlinx.coroutines.flow.StateFlow

sealed interface RecorderState {
    data object Idle : RecorderState

    /** Grabando; [seconds] sale de la cantidad de audio capturado, no de un reloj. */
    data class Recording(val seconds: Int) : RecorderState

    /** Terminó: [wavPath] es un WAV 16 kHz mono 16 bits, temporal y propiedad de la app. */
    data class Finished(val wavPath: String, val durationMs: Long) : RecorderState

    /** No se pudo grabar (micrófono ocupado, sin espacio, etc.). */
    data class Failed(val message: String) : RecorderState
}

/**
 * Graba audio del micrófono a un WAV temporal. La grabación sigue con la pantalla apagada o la
 * app en segundo plano, por eso el estado vive acá y no en un ViewModel.
 */
interface AudioRecorder {
    val state: StateFlow<RecorderState>

    /** Requiere el permiso RECORD_AUDIO ya concedido. */
    fun start()

    /** Pide terminar; el estado pasa a [RecorderState.Finished] en una fracción de segundo. */
    fun stop()

    /** Descarta el resultado anterior (borra su WAV) y vuelve a [RecorderState.Idle]. */
    fun reset()
}
