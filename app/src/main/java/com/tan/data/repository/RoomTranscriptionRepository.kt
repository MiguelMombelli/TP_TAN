package com.tan.data.repository

import com.tan.data.local.dao.TranscriptionDao
import com.tan.data.local.mapper.toDomain
import com.tan.data.local.mapper.toEntities
import com.tan.data.local.mapper.toEntity
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSummary
import com.tan.domain.repository.TranscriptionRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTranscriptionRepository @Inject constructor(
    private val dao: TranscriptionDao
) : TranscriptionRepository {

    override fun observeAll(): Flow<List<TranscriptionSummary>> =
        dao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    override suspend fun getById(id: Long): Transcription? {
        val transcription = dao.getTranscription(id) ?: return null
        return transcription.toDomain(dao.getSegments(id))
    }

    override suspend fun save(transcription: Transcription): Long =
        dao.insertWithSegments(transcription.toEntity(), transcription.segments.toEntities())

    override suspend fun delete(id: Long) = dao.deleteById(id)
}
