package com.tan.presentation.navigation

import android.content.Context
import java.io.File

/**
 * TEMPORAL: audio de muestra (jfk.wav, en inglés) que solo existe en las compilaciones debug
 * (app/src/debug/assets). Sirve de entrada para probar la transcripción hasta que existan la
 * grabación real (T6) y la importación (T5). Cuando estén, se borra este archivo y el asset.
 */
internal object SampleAudio {
    /** Copia el audio a la caché (el ViewModel lo borra al terminar) y devuelve su ruta, o null si no existe. */
    fun copyToCache(context: Context): String? = runCatching {
        val out = File(context.cacheDir, "sample_${System.nanoTime()}.wav")
        context.assets.open("jfk.wav").use { input -> out.outputStream().use { input.copyTo(it) } }
        out.absolutePath
    }.getOrNull()
}
