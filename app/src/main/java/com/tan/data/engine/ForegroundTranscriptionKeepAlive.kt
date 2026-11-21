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

    override fun start() {
        // Si Android no deja arrancar el servicio, la transcripción sigue igual (solo que sin la
        // protección en segundo plano): no vale la pena romper la pantalla por esto.
        runCatching { ContextCompat.startForegroundService(context, intent) }
    }

    override fun stop() {
        context.stopService(intent)
    }
}
