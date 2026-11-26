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
        synchronized(TranscriptionService.lock) {
            if (holders++ == 0) {
                TranscriptionService.wanted = true
                // Si Android no deja arrancar el servicio, el trabajo sigue igual (solo que sin la
                // protección en segundo plano): no vale la pena romper la pantalla por esto.
                runCatching { ContextCompat.startForegroundService(context, intent) }
            }
        }
    }

    override fun stop() {
        synchronized(TranscriptionService.lock) {
            if (holders > 0 && --holders == 0) {
                TranscriptionService.wanted = false
                // Si el servicio todavía no arrancó (un trabajo que falla al instante, como una
                // descarga en modo avión), detenerlo ahora hace que Android cierre la app. En ese
                // caso no se lo detiene desde acá: se detiene solo en onStartCommand.
                if (TranscriptionService.running) context.stopService(intent)
            }
        }
    }
}
