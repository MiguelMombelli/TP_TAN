package com.tan.data.audio

import android.content.Context
import androidx.core.content.ContextCompat
import com.tan.domain.audio.AudioRecorder
import com.tan.domain.audio.RecorderState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada de la grabación: arranca y detiene [RecordingService] y publica su estado. Es un singleton
 * para que el estado sobreviva a los ViewModels (se puede salir de la pantalla y volver mientras graba).
 */
@Singleton
class ForegroundAudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context,
) : AudioRecorder {

    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    override val state: StateFlow<RecorderState> = _state.asStateFlow()

    override fun start() {
        if (_state.value is RecorderState.Recording) return
        deleteFinishedWav() // una grabación anterior sin usar no se acumula en la caché
        _state.value = RecorderState.Recording(0)
        ContextCompat.startForegroundService(context, RecordingService.intent(context, RecordingService.ACTION_START))
    }

    override fun stop() {
        if (_state.value !is RecorderState.Recording) return
        context.startService(RecordingService.intent(context, RecordingService.ACTION_STOP))
    }

    override fun reset() {
        if (_state.value is RecorderState.Recording) return
        deleteFinishedWav()
        _state.value = RecorderState.Idle
    }

    /** Lo llama el servicio para informar el avance. */
    internal fun publish(state: RecorderState) {
        _state.value = state
    }

    private fun deleteFinishedWav() {
        (_state.value as? RecorderState.Finished)?.let { File(it.wavPath).delete() }
    }
}
