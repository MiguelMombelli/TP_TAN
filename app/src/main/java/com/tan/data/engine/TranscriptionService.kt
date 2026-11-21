package com.tan.data.engine

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.tan.R

/**
 * Servicio en primer plano que NO hace el trabajo: solo existe para que, mientras Whisper
 * transcribe, se convierte un audio o se descarga un modelo, Android considere a la app "en uso"
 * (por la notificación) y no la mate. El WakeLock
 * evita que el CPU se duerma con la pantalla apagada. La transcripción sigue viviendo en el
 * ViewModel; si el usuario sale de la pantalla con "atrás", se cancela y este servicio se detiene.
 */
class TranscriptionService : Service() {

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    // startForeground va en onCreate (no en onStartCommand) para que se cumpla aunque el servicio
    // se detenga enseguida; si no, Android tira ForegroundServiceDidNotStartInTimeException.
    override fun onCreate() {
        super.onCreate()
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tan:transcribing")
            .apply { acquire(MAX_TRANSCRIPTION_MS) } // tope de seguridad: se libera solo
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Trabajo en segundo plano", NotificationManager.IMPORTANCE_LOW)
        )
        // El intent de lanzamiento trae la app existente al frente en vez de abrir otra instancia.
        val openApp = PendingIntent.getActivity(
            this, 0, packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("TAN")
            .setContentText("Trabajando en segundo plano…")
            .setOngoing(true)
            .setUsesChronometer(true)
            .setContentIntent(openApp)
            .build()
    }

    private companion object {
        const val NOTIFICATION_ID = 2 // el 1 es el de la grabación
        const val CHANNEL_ID = "transcribing"
        const val MAX_TRANSCRIPTION_MS = 3 * 60 * 60 * 1000L
    }
}
