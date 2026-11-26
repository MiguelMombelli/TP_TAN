package com.tan.data.repository

import android.content.Context
import com.tan.data.remote.ModelDownloadApi
import com.tan.domain.engine.TranscriptionKeepAlive
import com.tan.domain.model.ModelCatalog
import com.tan.domain.model.WhisperModel
import com.tan.domain.repository.DownloadFailure
import com.tan.domain.repository.DownloadState
import com.tan.domain.repository.ModelRepository
import com.tan.domain.repository.PreferencesRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Guarda los modelos en la carpeta privada de la app (filesDir/models), que el motor nativo sí puede leer.
 * Es la única fuente de verdad de "qué modelos hay descargados": si el archivo existe, el modelo está.
 *
 * Es un singleton con su propio scope: así la descarga sigue aunque el usuario salga de la pantalla.
 * (Si el sistema mata el proceso, la descarga se pierde; en v1 no se retoma.)
 */
@Singleton
class FileModelRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val prefs: PreferencesRepository,
    private val api: ModelDownloadApi,
    private val keepAlive: TranscriptionKeepAlive,
) : ModelRepository {

    private val modelsDir = File(context.filesDir, "models").apply { mkdirs() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    private val _downloadedIds = MutableStateFlow(scanDownloaded())
    override val downloadedIds: StateFlow<Set<String>> = _downloadedIds.asStateFlow()

    private val _downloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    override val downloads: StateFlow<Map<String, DownloadState>> = _downloads.asStateFlow()

    override fun startDownload(id: String) {
        val model = ModelCatalog.byId(id) ?: return
        synchronized(jobs) {
            if (jobs[id]?.isActive == true || id in _downloadedIds.value) return
            jobs[id] = scope.launch { download(model) }
        }
    }

    override suspend fun delete(id: String) {
        val model = ModelCatalog.byId(id) ?: return
        withContext(Dispatchers.IO) {
            synchronized(jobs) { jobs.remove(id) }?.cancelAndJoin()
            File(modelsDir, model.fileName).delete()
            partFile(model).delete()
            _downloadedIds.update { it - id }
            setDownload(id, null)
            if (prefs.activeModelId.first() == id) prefs.setActiveModelId(null)
        }
    }

    override fun clearFailures() {
        _downloads.update { current -> current.filterValues { it !is DownloadState.Failed } }
    }

    override suspend fun activeModelPath(): String? {
        val id = prefs.activeModelId.first() ?: return null
        val model = ModelCatalog.byId(id) ?: return null
        return if (id in _downloadedIds.value) File(modelsDir, model.fileName).absolutePath else null
    }

    private suspend fun download(model: WhisperModel) {
        val part = partFile(model)
        setDownload(model.id, DownloadState.Progress(0f))
        // Con la pantalla apagada o en otra app, Android puede cortar la red a una app sin servicio.
        keepAlive.start()
        try {
            downloadToFile(model.fileName, part) { setDownload(model.id, DownloadState.Progress(it)) }
            // Solo con la descarga completa el archivo pasa a tener su nombre definitivo.
            if (!part.renameTo(File(modelsDir, model.fileName))) {
                throw DownloadFailureException(DownloadFailure.OTHER, "No se pudo renombrar ${part.name}")
            }
            _downloadedIds.update { it + model.id }
            // El primer modelo que se baja queda activo solo; así no hay un paso extra para empezar.
            if (prefs.activeModelId.first() == null) prefs.setActiveModelId(model.id)
            setDownload(model.id, null)
        } catch (e: CancellationException) {
            part.delete()
            throw e
        } catch (e: IOException) {
            part.delete()
            setDownload(model.id, DownloadState.Failed(classify(e)))
        } finally {
            keepAlive.stop() // termine bien, falle o se cancele
        }
    }

    /** Distingue por qué falló para no culpar siempre a la conexión. */
    private fun classify(e: IOException): DownloadFailure = when {
        e is DownloadFailureException -> e.reason
        e.message?.contains("ENOSPC") == true -> DownloadFailure.NO_SPACE // disco lleno al escribir
        else -> DownloadFailure.NO_CONNECTION // sin red, red cortada, tiempo agotado, descarga incompleta
    }

    private suspend fun downloadToFile(fileName: String, dest: File, onProgress: (Float) -> Unit) {
        // Retrofit lanza IOException si no hay red; un error del servidor (404, 500...) vuelve como
        // respuesta no exitosa y lo convertimos a IOException para tratarlo igual que los demás fallos.
        val response = api.download(fileName)
        if (!response.isSuccessful) {
            response.errorBody()?.close()
            throw DownloadFailureException(DownloadFailure.OTHER, "El servidor respondió HTTP ${response.code()}")
        }
        val body = response.body()
            ?: throw DownloadFailureException(DownloadFailure.OTHER, "El servidor respondió sin contenido")
        val total = body.contentLength() // -1 si el servidor no informa el tamaño
        if (total > 0 && modelsDir.usableSpace < total) {
            body.close()
            throw DownloadFailureException(DownloadFailure.NO_SPACE, "Faltan ${total - modelsDir.usableSpace} bytes")
        }
        var done = 0L
        var lastReported = 0f
        body.use { // cierra la conexión al terminar, también si hay error o cancelación
            it.byteStream().use { input ->
                dest.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        currentCoroutineContext().ensureActive() // permite cancelar entre bloque y bloque
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) {
                            val fraction = done.toFloat() / total
                            // Se avisa cada 1 %: más seguido solo gastaría batería redibujando la barra.
                            if (fraction - lastReported >= 0.01f) {
                                lastReported = fraction
                                onProgress(fraction)
                            }
                        }
                    }
                }
            }
        }
        if (total > 0 && done != total) throw IOException("Descarga incompleta: $done de $total bytes")
    }

    /** Falla con causa conocida; cualquier otra IOException se toma como problema de red. */
    private class DownloadFailureException(val reason: DownloadFailure, message: String) : IOException(message)

    private fun partFile(model: WhisperModel) = File(modelsDir, model.fileName + ".part")

    private fun setDownload(id: String, state: DownloadState?) {
        _downloads.update { current -> if (state == null) current - id else current + (id to state) }
    }

    private fun scanDownloaded(): Set<String> {
        // Un .part que quedó de una corrida anterior (proceso muerto a mitad) no sirve: se descarta.
        modelsDir.listFiles { f -> f.name.endsWith(".part") }?.forEach { it.delete() }
        return ModelCatalog.all
            .filter { m -> File(modelsDir, m.fileName).let { it.isFile && it.length() > 0 } }
            .map { it.id }
            .toSet()
    }
}
