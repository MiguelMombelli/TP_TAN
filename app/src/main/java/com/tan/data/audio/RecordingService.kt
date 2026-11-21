package com.tan.data.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.tan.MainActivity
import com.tan.R
import com.tan.domain.audio.RecorderState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Servicio en primer plano (tipo micrófono) que captura audio. Mientras corre, Android no mata el
 * proceso ni corta el micrófono con la pantalla apagada; la notificación es el aviso obligatorio
 * al usuario. El WakeLock evita que el CPU se duerma en grabaciones largas.
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject lateinit var recorder: ForegroundAudioRecorder

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    @Volatile private var stopRequested = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCapture()
            ACTION_STOP -> stopRequested = true
        }
        return START_NOT_STICKY // si el sistema mata el servicio no se reinicia solo con una grabación a medias
    }

    private fun startCapture() {
        if (job?.isActive == true) return
        // Hay que llamar a startForeground enseguida (Android da 5 segundos), antes que cualquier otra cosa.
        ServiceCompat.startForeground(
            this, NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE,
        )
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "tan:recording")
            .apply { acquire(MAX_RECORDING_MS) } // tope de seguridad: se libera solo
        stopRequested = false
        job = scope.launch { capture() }
    }

    private fun capture() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)
        if (minBuffer <= 0) return finishWithFailure("Este dispositivo no permite grabar a 16 kHz.")
        val record = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL, ENCODING, maxOf(minBuffer, BYTES_PER_SECOND))
        } catch (e: SecurityException) {
            return finishWithFailure("Falta el permiso del micrófono.")
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return finishWithFailure("No se pudo acceder al micrófono. ¿Lo está usando otra app?")
        }

        val file = File(cacheDir, "recording_${System.currentTimeMillis()}.wav")
        var bytes = 0L
        try {
            WavWriter(file).use { wav ->
                record.startRecording()
                val buffer = ByteArray(BYTES_PER_SECOND / 10) // bloques de 100 ms
                var lastSecond = -1
                while (!stopRequested) {
                    val read = record.read(buffer, 0, buffer.size) // espera hasta que haya audio
                    if (read < 0) throw IOException("AudioRecord devolvió el error $read")
                    wav.write(buffer, 0, read)
                    bytes += read
                    val second = (bytes / BYTES_PER_SECOND).toInt()
                    if (second != lastSecond) {
                        lastSecond = second
                        recorder.publish(RecorderState.Recording(second))
                    }
                }
            }
            recorder.publish(RecorderState.Finished(file.absolutePath, bytes * 1000 / BYTES_PER_SECOND))
        } catch (e: IOException) {
            file.delete()
            recorder.publish(RecorderState.Failed("No se pudo guardar la grabación. ¿Falta espacio en el dispositivo?"))
        } finally {
            runCatching { record.stop() }
            record.release()
            shutDown()
        }
    }

    private fun finishWithFailure(message: String) {
        recorder.publish(RecorderState.Failed(message))
        shutDown()
    }

    private fun shutDown() {
        wakeLock?.takeIf { it.isHeld }?.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        wakeLock?.takeIf { it.isHeld }?.release()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Grabación de audio", NotificationManager.IMPORTANCE_LOW)
        )
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mic)
            .setContentTitle("TAN")
            .setContentText("Grabando audio…")
            .setOngoing(true)
            .setUsesChronometer(true)
            .setContentIntent(openApp)
            .build()
    }

    companion object {
        const val ACTION_START = "com.tan.action.START_RECORDING"
        const val ACTION_STOP = "com.tan.action.STOP_RECORDING"

        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "recording"
        private const val SAMPLE_RATE = 16_000 // lo que necesita Whisper
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val BYTES_PER_SECOND = SAMPLE_RATE * 2
        private const val MAX_RECORDING_MS = 4 * 60 * 60 * 1000L

        fun intent(context: Context, action: String) =
            Intent(context, RecordingService::class.java).setAction(action)
    }
}
