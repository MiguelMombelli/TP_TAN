package com.tan.domain.engine

import com.tan.domain.model.Segment

/**
 * Lo único que los ViewModels saben del motor de transcripción. La implementación real usa
 * whisper.cpp (ver WhisperEngine); en los tests se reemplaza por un motor falso.
 *
 * Todas las funciones `suspend` se pueden llamar desde el hilo principal: el trabajo pesado
 * corre en segundo plano.
 */
interface TranscriptionEngine {

    /**
     * Carga el modelo en memoria. Si ya está cargado el mismo archivo, no hace nada.
     * Puede tardar segundos en los modelos grandes.
     *
     * @throws ModelLoadException si el archivo no existe o está dañado.
     */
    suspend fun loadModel(modelPath: String)

    /** Libera la memoria del modelo. Si hay una transcripción en curso, espera a que termine. */
    suspend fun unloadModel()

    /**
     * Transcribe un WAV PCM de 16 kHz, mono, 16 bits y devuelve los segmentos con sus tiempos.
     * Devuelve una lista vacía si no se detectó voz (o el audio dura menos de un segundo).
     *
     * @param language código de idioma de Whisper ("es", "en"...) o "auto" para detectarlo.
     * @param onProgress porcentaje 0-100. Se llama desde un hilo de fondo, no desde el principal.
     * @throws InvalidAudioException si el WAV no sirve.
     * @throws ModelLoadException si no hay un modelo cargado.
     * @throws TranscriptionFailedException si Whisper falla.
     * @throws TranscriptionCancelledException si se llamó a [cancel] (o se canceló la corrutina).
     */
    suspend fun transcribe(
        wavPath: String,
        language: String = "es",
        threads: Int = 4,
        onProgress: (Int) -> Unit = {},
    ): List<Segment>

    /** Pide cortar la transcripción en curso. La llamada a [transcribe] termina con [TranscriptionCancelledException]. */
    fun cancel()
}
