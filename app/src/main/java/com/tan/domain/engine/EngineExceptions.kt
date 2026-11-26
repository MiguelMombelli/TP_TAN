package com.tan.domain.engine

/**
 * Errores del motor de transcripción. La capa nativa (C++) lanza estas clases por nombre,
 * así que si se renombran o se mueven de paquete hay que actualizar whisper_jni.cpp.
 *
 * Es `sealed`: un `when` sobre el error tiene que cubrir los cuatro casos, y la pantalla puede
 * mostrar un mensaje distinto para cada uno.
 */
sealed class EngineException(message: String) : Exception(message)

/** El audio no se puede usar: no existe, no es un WAV válido o no está en 16 kHz mono de 16 bits. */
class InvalidAudioException(message: String) : EngineException(message)

/** No hay modelo cargado, no se encuentra el archivo o está dañado. */
class ModelLoadException(message: String) : EngineException(message)

/** Whisper falló mientras transcribía (por ejemplo, falta de memoria). */
class TranscriptionFailedException(message: String) : EngineException(message)

/** El usuario canceló la transcripción. No es un fallo: la pantalla debería volver atrás sin avisar. */
class TranscriptionCancelledException(message: String = "Transcripción cancelada.") : EngineException(message)
