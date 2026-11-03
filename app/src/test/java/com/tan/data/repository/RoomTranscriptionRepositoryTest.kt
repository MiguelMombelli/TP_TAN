package com.tan.data.repository

import com.tan.data.local.FakeTranscriptionDao
import com.tan.domain.model.Segment
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomTranscriptionRepositoryTest {

    private val repository = RoomTranscriptionRepository(FakeTranscriptionDao())

    private fun sample(title: String = "Clase", createdAtMs: Long = 1_000) = Transcription(
        title = title,
        createdAtMs = createdAtMs,
        source = TranscriptionSource.FILE,
        model = "tiny",
        durationMs = 7800,
        segments = listOf(Segment(0, 3200, "Buenas tardes."), Segment(3200, 7800, "Empecemos."))
    )

    @Test fun saveThenGetById_returnsSegmentsInOrder() = runBlocking {
        val id = repository.save(sample())
        val loaded = repository.getById(id)!!
        assertEquals(id, loaded.id)
        assertEquals("Clase", loaded.title)
        assertEquals(listOf("Buenas tardes.", "Empecemos."), loaded.segments.map { it.text })
        assertEquals(3200L, loaded.segments[1].startMs)
    }

    @Test fun getById_unknownReturnsNull() = runBlocking {
        assertNull(repository.getById(42))
    }

    @Test fun save_ignoresIncomingIdAndCreatesNewRow() = runBlocking {
        val first = repository.save(sample())
        val second = repository.save(sample().copy(id = first))
        assertNotEquals(first, second)
        assertEquals(2, repository.observeAll().first().size)
    }

    @Test fun observeAll_newestFirst() = runBlocking {
        repository.save(sample(title = "Vieja", createdAtMs = 1_000))
        repository.save(sample(title = "Nueva", createdAtMs = 2_000))
        assertEquals(listOf("Nueva", "Vieja"), repository.observeAll().first().map { it.title })
    }

    @Test fun observeAll_previewIsTextAdelanto() = runBlocking {
        repository.save(sample())
        assertEquals("Buenas tardes.\nEmpecemos.", repository.observeAll().first().single().preview)
    }

    @Test fun delete_removesTranscriptionAndSegments() = runBlocking {
        val id = repository.save(sample())
        repository.delete(id)
        assertNull(repository.getById(id))
        assertEquals(emptyList<Any>(), repository.observeAll().first())
    }
}
