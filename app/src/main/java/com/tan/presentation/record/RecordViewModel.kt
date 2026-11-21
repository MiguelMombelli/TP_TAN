package com.tan.presentation.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.audio.AudioRecorder
import com.tan.domain.audio.RecorderState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val recorder: AudioRecorder,
) : ViewModel() {

    private val permissionDenied = MutableStateFlow(false)

    // El estado real de la grabación vive en el AudioRecorder (un singleton), no acá: así, si salís
    // de la pantalla mientras graba y volvés, la pantalla muestra lo que está pasando de verdad.
    val state: StateFlow<RecordUiState> = combine(recorder.state, permissionDenied) { rec, denied ->
        when {
            rec is RecorderState.Recording -> RecordUiState.Recording(rec.seconds)
            // Si el WAV ya no existe (por ejemplo se transcribió y se borró), se vuelve al inicio.
            rec is RecorderState.Finished && File(rec.wavPath).isFile -> RecordUiState.Saved(rec.wavPath)
            rec is RecorderState.Failed -> RecordUiState.Failed(rec.message)
            denied -> RecordUiState.PermissionDenied
            else -> RecordUiState.Idle
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RecordUiState.Idle)

    /** La pantalla pide el permiso del micrófono y avisa el resultado; recién con permiso se graba. */
    fun onPermissionResult(granted: Boolean) {
        permissionDenied.value = !granted
        if (granted) recorder.start()
    }

    fun onStop() = recorder.stop()

    fun onRecordAgain() {
        permissionDenied.value = false
        recorder.reset()
    }
}
