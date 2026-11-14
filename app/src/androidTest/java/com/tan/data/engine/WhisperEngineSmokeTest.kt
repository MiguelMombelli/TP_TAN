package com.tan.data.engine

import android.os.SystemClock
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.tan.domain.engine.InvalidAudioException
import com.tan.domain.engine.ModelLoadException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Prueba de humo del motor real en el celular: carga whisper.cpp de verdad.
 *
 * Para la prueba con audio hace falta el modelo tiny dentro del APK de test. Copiar (una sola vez)
 * ggml-tiny.bin a app/src/androidTest/assets/ (el .gitignore evita que se suba al repositorio).
 * Se usa un asset y no adb porque Gradle desinstala la app al terminar cada corrida, y con ella
 * se borran sus carpetas de datos.
 *
 * El resultado (tiempos, hilos, flags de CPU) queda en Logcat con el filtro "TanSmoke" y "TanWhisper".
 */
@RunWith(AndroidJUnit4::class)
class WhisperEngineSmokeTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val targetContext = instrumentation.targetContext

    /** No necesita modelo: comprueba que la librería nativa carga y que los errores llegan como excepciones. */
    @Test
    fun nativeLibraryLoadsAndErrorsBecomeExceptions() = runBlocking<Unit> {
        val engine = WhisperEngine()
        Log.i(TAG, "Motor: ${engine.systemInfo()}")

        try {
            engine.loadModel(File(targetContext.cacheDir, "no-existe.bin").absolutePath)
            throw AssertionError("Debió lanzar ModelLoadException")
        } catch (expected: ModelLoadException) {
            Log.i(TAG, "OK, ModelLoadException: ${expected.message}")
        }

        try {
            engine.transcribe(File(targetContext.cacheDir, "no-existe.wav").absolutePath)
            throw AssertionError("Debió lanzar una excepción")
        } catch (expected: InvalidAudioException) {
            Log.i(TAG, "OK, InvalidAudioException: ${expected.message}")
        } catch (expected: ModelLoadException) {
            Log.i(TAG, "OK, ModelLoadException (sin modelo cargado): ${expected.message}")
        }
    }

    /** Criterio de cierre de la Etapa 1: texto real de un WAV fijo, con tiny, en el celular. */
    @Test
    fun transcribesJfkWithTiny() = runBlocking<Unit> {
        val found = findModel()
        assumeTrue(
            "Falta el modelo: copiar ggml-tiny.bin a app/src/androidTest/assets/ (ver el comentario de la clase).",
            found != null,
        )
        val modelFile = found!!
        Log.i(TAG, "Modelo: ${modelFile.absolutePath} (${modelFile.length()} bytes)")

        val wav = File(targetContext.cacheDir, "jfk.wav")
        instrumentation.context.assets.open("jfk.wav").use { input ->
            wav.outputStream().use { input.copyTo(it) }
        }

        val engine = WhisperEngine()
        Log.i(TAG, "Motor: ${engine.systemInfo()}")

        val loadStart = SystemClock.elapsedRealtime()
        engine.loadModel(modelFile.absolutePath)
        Log.i(TAG, "Carga del modelo: ${SystemClock.elapsedRealtime() - loadStart} ms")

        val progress = mutableListOf<Int>()
        val start = SystemClock.elapsedRealtime()
        val segments = engine.transcribe(wav.absolutePath, language = "en", threads = 4) { progress += it }
        val tookMs = SystemClock.elapsedRealtime() - start

        val text = segments.joinToString(" ") { it.text }
        Log.i(TAG, "jfk.wav (11 s de audio) transcripto en $tookMs ms, ${segments.size} segmentos")
        segments.forEach { Log.i(TAG, "  [${it.startMs}-${it.endMs}] ${it.text}") }
        Log.i(TAG, "Progreso: $progress")

        assertTrue("No devolvió segmentos", segments.isNotEmpty())
        assertTrue("Texto inesperado: $text", text.contains("country", ignoreCase = true))
        assertTrue("El progreso debería avanzar", progress.isNotEmpty())
    }

    /** Cancelar a mitad de camino debe cortar rápido y dejar el motor usable para la siguiente transcripción. */
    @Test
    fun cancelStopsTranscriptionAndEngineStaysUsable() = runBlocking<Unit> {
        val found = findModel()
        assumeTrue("Falta el modelo (ver el comentario de la clase).", found != null)
        val modelFile = found!!

        val wav = File(targetContext.cacheDir, "jfk.wav")
        instrumentation.context.assets.open("jfk.wav").use { input ->
            wav.outputStream().use { input.copyTo(it) }
        }

        val engine = WhisperEngine()
        engine.loadModel(modelFile.absolutePath)

        // Lanzamos la transcripción y la cancelamos a los 500 ms (en el celular tarda ~3 s).
        val job = launch { engine.transcribe(wav.absolutePath, language = "en", threads = 4) }
        delay(500)
        val cancelStart = SystemClock.elapsedRealtime()
        job.cancelAndJoin()
        val cancelMs = SystemClock.elapsedRealtime() - cancelStart
        Log.i(TAG, "Cancelación: la corrutina terminó ${cancelMs} ms después de cancelar (isCancelled=${job.isCancelled})")
        assertTrue("La tarea debería figurar como cancelada", job.isCancelled)
        assertTrue("Cancelar tardó demasiado: $cancelMs ms", cancelMs < 3000)

        // Después de cancelar, el motor tiene que seguir funcionando.
        val segments = engine.transcribe(wav.absolutePath, language = "en", threads = 4)
        val text = segments.joinToString(" ") { it.text }
        Log.i(TAG, "Tras cancelar, transcripción normal OK: $text")
        assertTrue("Tras cancelar no transcribió bien: $text", text.contains("country", ignoreCase = true))
    }

    /** Copia el modelo desde los assets del APK de test a la caché de la app, que es donde el motor puede leerlo. */
    private fun findModel(): File? {
        val assets = instrumentation.context.assets
        if (assets.list("")?.contains(MODEL_ASSET) != true) return null
        val copy = File(targetContext.cacheDir, MODEL_ASSET)
        assets.open(MODEL_ASSET).use { input -> copy.outputStream().use { input.copyTo(it) } }
        return if (copy.length() > 0) copy else null  // un archivo vacío cuenta como "falta el modelo"
    }

    private companion object {
        const val MODEL_ASSET = "ggml-tiny.bin"
        const val TAG = "TanSmoke"
    }
}
