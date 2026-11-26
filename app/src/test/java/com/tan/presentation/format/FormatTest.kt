package com.tan.presentation.format

import com.tan.domain.model.Segment
import com.tan.domain.model.Transcription
import com.tan.domain.model.TranscriptionSource
import com.tan.domain.model.TranscriptionSummary
import com.tan.presentation.history.HistoryUiState
import java.time.Instant
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FormatTest {

    private val zone = ZoneId.of("America/Argentina/Buenos_Aires")

    // 17/04/2026 21:15 en Buenos Aires (UTC-3) = 18/04/2026 00:15 UTC
    private val ms = Instant.parse("2026-04-18T00:15:00Z").toEpochMilli()

    @Test fun formatDate_usesZone() = assertEquals("17/04/2026", formatDate(ms, zone))

    @Test fun formatDateTime_usesZone() = assertEquals("17/04/2026 21:15", formatDateTime(ms, zone))

    @Test fun label_bySource() {
        assertEquals("Grabación", TranscriptionSource.RECORDING.label())
        assertEquals("Archivo", TranscriptionSource.FILE.label())
    }

    @Test fun defaultTitle_includesSourceAndDateTime() =
        assertEquals("Grabación 17/04/2026 21:15", defaultTitle(TranscriptionSource.RECORDING, ms, zone))

    @Test fun emptyList_isEmptyState() =
        assertEquals(HistoryUiState.Empty, emptyList<TranscriptionSummary>().toHistoryState(zone))

    @Test fun summary_mapsToHistoryItem() {
        val state = listOf(
            TranscriptionSummary(7, "Clase", ms, TranscriptionSource.FILE, "base", 5_000, "adelanto")
        ).toHistoryState(zone)
        val item = (state as HistoryUiState.Content).items.single()
        assertEquals(7L, item.id)
        assertEquals("17/04/2026", item.date)
        assertEquals("Archivo", item.sourceLabel)
        assertEquals("base", item.model)
        assertEquals("adelanto", item.preview)
    }

    private val transcription = Transcription(
        id = 3, title = "Clase", createdAtMs = ms, source = TranscriptionSource.RECORDING,
        model = "tiny", durationMs = 7_800,
        segments = listOf(Segment(0, 3_200, "Hola."), Segment(3_200, 7_800, "Chau."))
    )

    @Test fun transcription_mapsToDetailUi() {
        val detail = transcription.toDetailUi(zone)
        assertEquals("Clase", detail.title)
        assertEquals("17/04/2026", detail.date)
        assertEquals("Grabación", detail.sourceLabel)
        assertEquals("Hola.\nChau.", detail.text)
    }

    @Test fun transcription_mapsToResultContent() {
        val result = transcription.toResultContent()
        assertEquals("Clase", result.title)
        assertEquals(2, result.segments.size)
        assertTrue(result.text.startsWith("Hola."))
    }
}
