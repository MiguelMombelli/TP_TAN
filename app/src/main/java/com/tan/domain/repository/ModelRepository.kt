package com.tan.domain.repository

import kotlinx.coroutines.flow.StateFlow

sealed interface DownloadState {
    /** Descargando; [fraction] va de 0f a 1f. */
    data class Progress(val fraction: Float) : DownloadState

    /** La descarga falló (sin red, servidor caído, archivo incompleto...). */
    data object Failed : DownloadState
}

/** Modelos de Whisper en el dispositivo: descargarlos, borrarlos y saber dónde están. */
interface ModelRepository {
    /** Ids de los modelos que ya están completos en el dispositivo. */
    val downloadedIds: StateFlow<Set<String>>

    /** Descargas en curso o fallidas, por id de modelo. Un modelo terminado sale de este mapa. */
    val downloads: StateFlow<Map<String, DownloadState>>

    /** Arranca la descarga y vuelve enseguida; el avance se sigue por [downloads]. */
    fun startDownload(id: String)

    /** Borra el archivo (y cancela la descarga si estaba en curso). Si era el activo, deja de serlo. */
    suspend fun delete(id: String)

    /** Olvida las descargas fallidas, para no mostrar un error viejo al volver a la pantalla. */
    fun clearFailures()

    /** Ruta absoluta del modelo activo para pasársela al motor, o null si no hay uno descargado y activo. */
    suspend fun activeModelPath(): String?
}
