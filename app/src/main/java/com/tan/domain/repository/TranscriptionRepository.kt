package com.tan.domain.repository

import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSummary
import kotlinx.coroutines.flow.Flow

interface TranscriptionRepository {

    /** Lista para el Historial, la más nueva primero. Se actualiza sola al guardar o borrar. */
    fun observeAll(): Flow<List<TranscriptionSummary>>

    /** Devuelve la transcripción con sus segmentos en orden, o null si no existe. */
    suspend fun getById(id: Long): Transcription?

    /** Guarda una transcripción nueva (ignora el `id` que traiga) y devuelve el id asignado. */
    suspend fun save(transcription: Transcription): Long

    /** Borra la transcripción y todos sus segmentos. */
    suspend fun delete(id: Long)
}
