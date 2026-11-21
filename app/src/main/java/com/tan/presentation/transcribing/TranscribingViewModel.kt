package com.tan.presentation.transcribing

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tan.domain.engine.InvalidAudioException
import com.tan.domain.engine.ModelLoadException
import com.tan.domain.engine.TranscriptionCancelledException
import com.tan.domain.engine.TranscriptionEngine
import com.tan.domain.engine.TranscriptionFailedException
import com.tan.domain.engine.TranscriptionKeepAlive
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import com.tan.domain.repository.ModelRepository
import com.tan.domain.repository.PreferencesRepository
import com.tan.domain.repository.TranscriptionRepository
import com.tan.presentation.format.defaultTitle
import com.tan.presentation.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import kotlin.math.ceil

/**
 * Transcribe el WAV que le llega por argumento de navegación con el modelo activo y guarda el
 * resultado en Room. El WAV tiene que ser una copia temporal de la app (nunca el archivo original
 * del usuario): se borra al guardar bien y se conserva si algo falla.
 */
@HiltViewModel
class TranscribingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val engine: TranscriptionEngine,
    private val models: ModelRepository,
    private val prefs: PreferencesRepository,
    private val repository: TranscriptionRepository,
    private val keepAlive: TranscriptionKeepAlive,
) : ViewModel() {

    private val wavPath: String? = savedStateHandle[Routes.ARG_PATH]
    private val source: TranscriptionSource =
        savedStateHandle.get<String>(Routes.ARG_SOURCE)
            ?.let { name -> TranscriptionSource.entries.firstOrNull { it.name == name } }
            ?: TranscriptionSource.RECORDING
    private val language: String = savedStateHandle[Routes.ARG_LANG] ?: "es"

    private val _state = MutableStateFlow<TranscribingUiState>(
        TranscribingUiState.InProgress(progress = 0f, minutesLeft = 1)
    )
    val state: StateFlow<TranscribingUiState> = _state.asStateFlow()

    private var job: Job? = null

    init {
        job = viewModelScope.launch { transcribe() }
    }

    /**
     * Cancela la corrutina; WhisperEngine le avisa al código nativo, que corta en cuanto termina
     * su bloque de cálculo actual (con el modelo tiny, ~2 s). Salir de la pantalla también cancela.
     */
    fun onCancel() {
        job?.cancel()
    }

    private suspend fun transcribe() {
        val wav = wavPath?.let { File(it) }
        if (wav == null || !wav.isFile) {
            fail("No se encontró el audio a transcribir.")
            return
        }
        val modelId = prefs.activeModelId.first()
        val modelPath = models.activeModelPath()
        if (modelId == null || modelPath == null) {
            fail("Todavía no hay un modelo descargado. Elegí uno para poder transcribir.", needsModel = true)
            return
        }

        val durationMs = wavDurationMs(wav)
        val startedAt = SystemClock.elapsedRealtime()
        // Con un audio de una sola ventana de Whisper no hay avance intermedio que mostrar.
        val indeterminate = durationMs <= WHISPER_WINDOW_MS
        _state.value = TranscribingUiState.InProgress(0f, minutesLeft(0f, durationMs, 0L), indeterminate)

        // Mientras se transcribe, la app queda protegida en segundo plano (servicio + WakeLock).
        keepAlive.start()
        try {
            engine.loadModel(modelPath)
            val segments = engine.transcribe(wav.absolutePath, language, THREADS) { percent ->
                val progress = percent / 100f
                val elapsed = SystemClock.elapsedRealtime() - startedAt
                _state.value = TranscribingUiState.InProgress(
                    progress, minutesLeft(progress, durationMs, elapsed), indeterminate,
                )
            }
            if (segments.isEmpty()) {
                fail("No se detectó voz en el audio.")
                return
            }
            val now = System.currentTimeMillis()
            val id = repository.save(
                Transcription(
                    title = defaultTitle(source, now),
                    createdAtMs = now,
                    source = source,
                    model = modelId,
                    durationMs = durationMs,
                    segments = segments,
                )
            )
            wav.delete() // el audio no se guarda: solo el texto
            _state.value = TranscribingUiState.Done(id)
        } catch (e: CancellationException) {
            throw e // cancelación de la corrutina: no es un error, no se muestra nada
        } catch (e: TranscriptionCancelledException) {
            // Cancelación pedida con engine.cancel() desde otro lado: tampoco es un error.
        } catch (e: InvalidAudioException) {
            fail("El archivo de audio no es válido o está dañado.")
        } catch (e: ModelLoadException) {
            fail("No se pudo cargar el modelo. Probá descargarlo de nuevo.")
        } catch (e: TranscriptionFailedException) {
            fail("La transcripción falló. Intentá de nuevo.")
        } catch (e: Exception) {
            fail("Error en el proceso de transcripción")
        } finally {
            keepAlive.stop() // termine bien, mal o cancelado
        }
    }

    /**
     * Un audio importado es una copia temporal que nadie más usa: si se sale de la pantalla sin
     * terminar (cancelar, aceptar un error, volver atrás) se borra. Una grabación se conserva,
     * porque la pantalla Grabar la sigue mostrando y permite reintentar.
     */
    override fun onCleared() {
        if (source == TranscriptionSource.FILE) wavPath?.let { File(it).delete() }
    }

    private fun fail(message: String, needsModel: Boolean = false) {
        val progress = (_state.value as? TranscribingUiState.InProgress)?.progress ?: 0f
        _state.value = TranscribingUiState.Error(
            progress = progress, minutesLeft = 0, message = message, needsModel = needsModel,
        )
    }

    /** WAV PCM 16 kHz mono 16 bits = 32 bytes por milisegundo, más ~44 bytes de cabecera. */
    private fun wavDurationMs(wav: File): Long = (wav.length() - 44).coerceAtLeast(0) / 32

    /**
     * Estimación para el cartel "quedan X minutos". Al principio no hay datos: se supone una
     * velocidad de x3 tiempo real (la medida con tiny en el Motorola fue x3,7). Desde el 5 % se
     * extrapola con lo que ya tardó.
     */
    private fun minutesLeft(progress: Float, durationMs: Long, elapsedMs: Long): Int {
        val remainingMs =
            if (progress >= 0.05f) (elapsedMs * (1 - progress) / progress).toLong()
            else durationMs / ASSUMED_SPEED
        return ceil(remainingMs / 60_000.0).toInt().coerceAtLeast(1)
    }

    private companion object {
        const val THREADS = 4 // medido en el Motorola Edge 30 Pro
        const val ASSUMED_SPEED = 3
        const val WHISPER_WINDOW_MS = 30_000L // whisper.cpp procesa y avisa de a 30 s de audio
    }
}
