package com.tan.data.engine

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.tan.domain.engine.TranscriptionKeepAlive
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ForegroundTranscriptionKeepAlive @Inject constructor(
    @ApplicationContext private val context: Context,
) : TranscriptionKeepAlive {

    private val intent get() = Intent(context, TranscriptionService::class.java)

    // Cuántos trabajos largos hay en curso (transcribir, convertir, descargar). El servicio arranca
    // con el primero y se detiene recién cuando termina el último: si no, una descarga que termina
    // apagaría la protección de una transcripción que sigue.
    private var holders = 0

    override fun start() {
        synchronized(this) {
            if (holders++ == 0) {
                // Si Android no deja arrancar el servicio, el trabajo sigue igual (solo que sin la
                // protección en segundo plano): no vale la pena romper la pantalla por esto.
                runCatching { ContextCompat.startForegroundService(context, intent) }
            }
        }
    }

    override fun stop() {
        synchronized(this) {
            if (holders > 0 && --holders == 0) context.stopService(intent)
        }
    }
}
