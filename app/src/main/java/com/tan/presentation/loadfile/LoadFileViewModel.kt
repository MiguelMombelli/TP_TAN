package com.tan.presentation.loadfile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.audio.AudioFileInfo
import com.tan.domain.audio.AudioImportException
import com.tan.domain.audio.AudioImporter
import com.tan.domain.audio.UnsupportedAudioException
import com.tan.domain.engine.TranscriptionKeepAlive
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/**
 * Importar: el usuario elige un archivo, se muestran sus datos y, al continuar, se convierte a un
 * WAV temporal 16 kHz mono (el formato del motor). Con el WAV listo la pantalla navega a
 * Transcribiendo, que es la misma que usa la grabación.
 */
@HiltViewModel
class LoadFileViewModel @Inject constructor(
    private val importer: AudioImporter,
    private val keepAlive: TranscriptionKeepAlive,
) : ViewModel() {

    private val _state = MutableStateFlow<LoadFileUiState>(LoadFileUiState.Empty)
    val state: StateFlow<LoadFileUiState> = _state.asStateFlow()

    private var uri: String? = null
    private var job: Job? = null

    /** El selector de archivos devolvió [uri], o null si el usuario lo cerró sin elegir. */
    fun onFilePicked(uri: String?) {
        if (uri == null) return
        job?.cancel()
        this.uri = uri
        job = viewModelScope.launch {
            try {
                val info = importer.inspect(uri)
                _state.value = LoadFileUiState.Selected(info.name, describe(info))
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnsupportedAudioException) {
                _state.value = LoadFileUiState.UnsupportedFormat
            } catch (e: AudioImportException) {
                _state.value = LoadFileUiState.Failed(e.message ?: GENERIC_ERROR)
            }
        }
    }

    fun onContinue() {
        val selected = _state.value as? LoadFileUiState.Selected ?: return
        val uri = uri ?: return
        job = viewModelScope.launch {
            _state.value = LoadFileUiState.Converting(selected.fileName, selected.details, 0)
            // Una conversión larga también tiene que sobrevivir a la pantalla apagada.
            keepAlive.start()
            try {
                val wavPath = importer.convertToWav(uri) { percent ->
                    _state.value = LoadFileUiState.Converting(selected.fileName, selected.details, percent)
                }
                _state.value = LoadFileUiState.Converted(wavPath)
            } catch (e: CancellationException) {
                throw e // salir de la pantalla o cancelar: el importador ya borró el WAV parcial
            } catch (e: UnsupportedAudioException) {
                _state.value = LoadFileUiState.UnsupportedFormat
            } catch (e: AudioImportException) {
                _state.value = LoadFileUiState.Failed(e.message ?: GENERIC_ERROR)
            } finally {
                keepAlive.stop()
            }
        }
    }

    /** Botón Cancelar durante la conversión: vuelve a mostrar el archivo elegido. */
    fun onCancelConversion() {
        val converting = _state.value as? LoadFileUiState.Converting ?: return
        job?.cancel()
        _state.value = LoadFileUiState.Selected(converting.fileName, converting.details)
    }

    /** La pantalla ya navegó a Transcribiendo: se limpia para que al volver atrás no navegue de nuevo. */
    fun onNavigated() {
        uri = null
        _state.value = LoadFileUiState.Empty
    }

    private fun describe(info: AudioFileInfo): String {
        val parts = mutableListOf<String>()
        if (info.sizeBytes >= 0) {
            parts += String.format(Locale.getDefault(), "%.1f MB", info.sizeBytes / 1_048_576.0)
        }
        if (info.durationMs > 0) {
            val minutes = info.durationMs / 60_000
            parts += if (minutes >= 1) "$minutes min" else "${info.durationMs / 1000} s"
        }
        return parts.joinToString(" · ")
    }

    private companion object {
        const val GENERIC_ERROR = "No se pudo importar el archivo."
    }
}
