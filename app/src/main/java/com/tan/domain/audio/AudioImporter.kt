package com.tan.domain.audio

/** Lo que se muestra del archivo elegido antes de convertirlo. */
data class AudioFileInfo(val name: String, val sizeBytes: Long, val durationMs: Long)

/** El archivo no es un audio (o un video con audio) que Android sepa decodificar. */
class UnsupportedAudioException : Exception("Formato de audio no soportado")

/** Se pudo abrir el archivo pero falló al leerlo o decodificarlo (dañado, sin espacio, etc.). */
class AudioImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Convierte un archivo de audio o video elegido por el usuario al WAV 16 kHz mono 16 bits que
 * entiende el motor. Los archivos se identifican con un Uri en texto para que el dominio siga
 * siendo Kotlin puro.
 */
interface AudioImporter {
    /** Abre el archivo y devuelve sus datos; lanza [UnsupportedAudioException] si no tiene audio decodificable. */
    suspend fun inspect(uri: String): AudioFileInfo

    /**
     * Decodifica el audio a un WAV temporal propiedad de la app y devuelve su ruta. [onProgress]
     * recibe 0..100. Si la corrutina se cancela, el WAV parcial se borra.
     */
    suspend fun convertToWav(uri: String, onProgress: (Int) -> Unit): String
}
