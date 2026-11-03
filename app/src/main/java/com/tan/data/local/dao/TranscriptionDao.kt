package com.tan.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.tan.data.local.entity.SegmentEntity
import com.tan.data.local.entity.TranscriptionEntity
import com.tan.data.local.entity.TranscriptionSummaryRow
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TranscriptionDao {

    @Query(
        "SELECT id, title, createdAtMs, source, model, durationMs, " +
            "substr(text, 1, 120) AS preview " +
            "FROM transcriptions ORDER BY createdAtMs DESC, id DESC"
    )
    abstract fun observeSummaries(): Flow<List<TranscriptionSummaryRow>>

    @Query("SELECT * FROM transcriptions WHERE id = :id")
    abstract suspend fun getTranscription(id: Long): TranscriptionEntity?

    @Query("SELECT * FROM segments WHERE transcriptionId = :transcriptionId ORDER BY position")
    abstract suspend fun getSegments(transcriptionId: Long): List<SegmentEntity>

    @Insert
    abstract suspend fun insertTranscription(transcription: TranscriptionEntity): Long

    @Insert
    abstract suspend fun insertSegments(segments: List<SegmentEntity>)

    @Query("DELETE FROM transcriptions WHERE id = :id")
    abstract suspend fun deleteById(id: Long)

    /** Guarda la transcripción y sus segmentos en una sola transacción: o entra todo o nada. */
    @Transaction
    open suspend fun insertWithSegments(
        transcription: TranscriptionEntity,
        segments: List<SegmentEntity>
    ): Long {
        val id = insertTranscription(transcription)
        insertSegments(segments.map { it.copy(transcriptionId = id) })
        return id
    }
}
