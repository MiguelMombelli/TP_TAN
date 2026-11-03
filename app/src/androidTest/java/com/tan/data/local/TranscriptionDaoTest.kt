package com.tan.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tan.data.repository.RoomTranscriptionRepository
import com.tan.domain.model.Segment
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Prueba las consultas reales de Room contra una base en memoria (corre en el emulador). */
@RunWith(AndroidJUnit4::class)
class TranscriptionDaoTest {

    private lateinit var db: TanDatabase
    private lateinit var repository: RoomTranscriptionRepository

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, TanDatabase::class.java).build()
        repository = RoomTranscriptionRepository(db.transcriptionDao())
    }

    @After fun tearDown() = db.close()

    private fun sample(title: String, createdAtMs: Long) = Transcription(
        title = title,
        createdAtMs = createdAtMs,
        source = TranscriptionSource.RECORDING,
        model = "tiny",
        durationMs = 5000,
        segments = listOf(Segment(0, 2000, "uno"), Segment(2000, 5000, "dos"))
    )

    @Test fun roundTrip_keepsSegmentsInOrder() = runBlocking {
        val id = repository.save(sample("A", 1))
        val loaded = repository.getById(id)!!
        assertEquals(listOf("uno", "dos"), loaded.segments.map { it.text })
        assertEquals(TranscriptionSource.RECORDING, loaded.source)
    }

    @Test fun summaries_newestFirst() = runBlocking {
        repository.save(sample("Vieja", 1))
        repository.save(sample("Nueva", 2))
        assertEquals(listOf("Nueva", "Vieja"), repository.observeAll().first().map { it.title })
    }

    @Test fun delete_cascadesToSegments() = runBlocking {
        val id = repository.save(sample("A", 1))
        repository.delete(id)
        assertNull(repository.getById(id))
        assertEquals(emptyList<Any>(), db.transcriptionDao().getSegments(id))
    }
}
