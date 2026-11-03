package com.tan.data.local

import com.tan.data.local.dao.TranscriptionDao
import com.tan.data.local.entity.SegmentEntity
import com.tan.data.local.entity.TranscriptionEntity
import com.tan.data.local.entity.TranscriptionSummaryRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** DAO en memoria para probar el repositorio sin Android ni base de datos real. */
class FakeTranscriptionDao : TranscriptionDao() {
    private val transcriptions = MutableStateFlow<List<TranscriptionEntity>>(emptyList())
    private val segments = mutableListOf<SegmentEntity>()
    private var nextId = 1L

    override fun observeSummaries(): Flow<List<TranscriptionSummaryRow>> =
        transcriptions.map { list ->
            list.sortedWith(compareByDescending<TranscriptionEntity> { it.createdAtMs }.thenByDescending { it.id })
                .map {
                    TranscriptionSummaryRow(it.id, it.title, it.createdAtMs, it.source, it.model, it.durationMs, it.text.take(120))
                }
        }

    override suspend fun getTranscription(id: Long): TranscriptionEntity? =
        transcriptions.value.firstOrNull { it.id == id }

    override suspend fun getSegments(transcriptionId: Long): List<SegmentEntity> =
        segments.filter { it.transcriptionId == transcriptionId }.sortedBy { it.position }

    override suspend fun insertTranscription(transcription: TranscriptionEntity): Long {
        val id = nextId++
        transcriptions.value = transcriptions.value + transcription.copy(id = id)
        return id
    }

    override suspend fun insertSegments(segments: List<SegmentEntity>) {
        this.segments += segments.map { it.copy(id = nextId++) }
    }

    override suspend fun deleteById(id: Long) {
        transcriptions.value = transcriptions.value.filterNot { it.id == id }
        segments.removeAll { it.transcriptionId == id }
    }
}
