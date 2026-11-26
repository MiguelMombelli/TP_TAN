package com.tan.data.engine

import com.tan.domain.engine.TranscriptionCancelledException
import com.tan.domain.engine.TranscriptionEngine
import com.tan.domain.model.Segment
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementación de [TranscriptionEngine] con whisper.cpp. Es un singleton: hay un solo modelo
 * cargado en memoria para toda la app.
 */
@Singleton
class WhisperEngine @Inject constructor() : TranscriptionEngine {

    override suspend fun loadModel(modelPath: String) = withContext(Dispatchers.Default) {
        WhisperNative.nativeLoadModel(modelPath)
    }

    override suspend fun unloadModel() = withContext(Dispatchers.Default) {
        WhisperNative.nativeFreeModel()
    }

    override suspend fun transcribe(
        wavPath: String,
        language: String,
        threads: Int,
        onProgress: (Int) -> Unit,
    ): List<Segment> = coroutineScope {
        val work = async(Dispatchers.Default) {
            try {
                Result.success(
                    WhisperNative
                        .nativeTranscribe(wavPath, threads, language, ProgressListener { onProgress(it) })
                        .map { Segment(startMs = it.startMs, endMs = it.endMs, text = it.text) }
                        .filterNot { isNonSpeech(it.text) },
                )
            } catch (e: TranscriptionCancelledException) {
                // Se devuelve como resultado y no se lanza: si lo lanzáramos, la tarea hija fallaría
                // con un error "de verdad" y contaminaría a la corrutina que está cancelando.
                Result.failure(e)
            }
        }
        try {
            // Si cancelaron desde afuera con cancel(), acá se relanza TranscriptionCancelledException.
            work.await().getOrThrow()
        } catch (e: CancellationException) {
            // Si cancelan la corrutina (por ejemplo, el usuario sale de la pantalla), el C++ no se
            // entera solo: hay que avisarle o seguiría transcribiendo y ocupando el motor.
            WhisperNative.nativeCancel()
            throw e
        }
    }

    override fun cancel() = WhisperNative.nativeCancel()

    /** Para diagnóstico: versión de whisper.cpp y flags de CPU con los que se compiló. */
    fun systemInfo(): String = WhisperNative.nativeSystemInfo()
}

/**
 * Con silencio o ruido, Whisper suele "inventar" anotaciones como [MÚSICA], (música) o ♪ en vez
 * de devolver nada. Un segmento que solo tiene eso no es habla y se descarta; si no queda ninguno,
 * la pantalla muestra "No se detectó voz".
 */
internal fun isNonSpeech(text: String): Boolean =
    text.replace(NON_SPEECH_ANNOTATION, "").none { it.isLetterOrDigit() }

/** Texto entre corchetes, paréntesis o asteriscos: [MÚSICA], (risas), *música*. */
private val NON_SPEECH_ANNOTATION = Regex("""\[[^\]]*\]|\([^)]*\)|\*[^*]*\*""")
