package com.tan.data.local

import com.tan.data.local.entity.SegmentEntity
import com.tan.data.local.entity.TranscriptionEntity
import com.tan.data.local.entity.TranscriptionSummaryRow
import com.tan.data.local.mapper.toDomain
import com.tan.data.local.mapper.toEntities
import com.tan.data.local.mapper.toEntity
import com.tan.domain.model.Segment
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptionMappersTest {

    private val segments = listOf(Segment(0, 3200, "Hola."), Segment(3200, 7800, "Chau."))

    private val transcription = Transcription(
        id = 99,
        title = "Clase",
        createdAtMs = 1_000,
        source = TranscriptionSource.RECORDING,
        model = "tiny",
        durationMs = 7800,
        segments = segments
    )

    @Test fun toEntity_copiesFieldsAndBuildsText() {
        val entity = transcription.toEntity()
        assertEquals(0L, entity.id)
        assertEquals("Clase", entity.title)
        assertEquals("RECORDING", entity.source)
        assertEquals("tiny", entity.model)
        assertEquals(7800L, entity.durationMs)
        assertEquals("Hola.\nChau.", entity.text)
    }

    @Test fun toEntities_numbersPositionsFromZero() {
        val entities = segments.toEntities()
        assertEquals(listOf(0, 1), entities.map { it.position })
        assertEquals(listOf(0L, 3200L), entities.map { it.startMs })
        assertEquals(listOf("Hola.", "Chau."), entities.map { it.text })
    }

    @Test fun entityToDomain_ordersSegmentsByPosition() {
        val entity = TranscriptionEntity(1, "t", 5, "FILE", "base", 10, "x")
        val unordered = listOf(
            SegmentEntity(2, 1, position = 1, startMs = 10, endMs = 20, text = "segundo"),
            SegmentEntity(1, 1, position = 0, startMs = 0, endMs = 10, text = "primero")
        )
        val result = entity.toDomain(unordered)
        assertEquals(TranscriptionSource.FILE, result.source)
        assertEquals(listOf("primero", "segundo"), result.segments.map { it.text })
    }

    @Test fun unknownSource_fallsBackToFile() {
        val entity = TranscriptionEntity(1, "t", 5, "OTRA_COSA", "base", 10, "x")
        assertEquals(TranscriptionSource.FILE, entity.toDomain(emptyList()).source)
    }

    @Test fun summaryRowToDomain() {
        val row = TranscriptionSummaryRow(3, "Entrevista", 7, "RECORDING", "tiny", 60_000, "adelanto")
        val summary = row.toDomain()
        assertEquals(3L, summary.id)
        assertEquals(TranscriptionSource.RECORDING, summary.source)
        assertEquals("adelanto", summary.preview)
    }
}
