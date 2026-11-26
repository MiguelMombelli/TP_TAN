package com.tan.export

import com.tan.domain.model.Segment
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptExporterTest {

    private val sample = listOf(
        Segment(0, 3200, "Buenas tardes, empecemos la clase."),
        Segment(3200, 7800, "Hoy vemos el modelo de Piaget.")
    )

    // ---- tiempo ----

    @Test fun time_zero() = assertEquals("00:00:00,000", formatSrtTime(0))
    @Test fun time_millis() = assertEquals("00:00:01,500", formatSrtTime(1500))
    @Test fun time_minutes() = assertEquals("00:01:05,007", formatSrtTime(65_007))
    @Test fun time_sixtyMinutes() = assertEquals("01:00:00,000", formatSrtTime(3_600_000))
    @Test fun time_overOneHour() = assertEquals("02:03:04,567", formatSrtTime(7_384_567))
    @Test fun time_negativeIsZero() = assertEquals("00:00:00,000", formatSrtTime(-5))

    // ---- TXT ----

    @Test fun txt_joinsLines() = assertEquals(
        "Buenas tardes, empecemos la clase.\nHoy vemos el modelo de Piaget.",
        TranscriptExporter.toTxt(sample)
    )

    @Test fun txt_emptyList() = assertEquals("", TranscriptExporter.toTxt(emptyList()))

    @Test fun txt_skipsBlankAndTrims() = assertEquals(
        "hola\nchau",
        TranscriptExporter.toTxt(listOf(Segment(0, 1, "  hola "), Segment(1, 2, "   "), Segment(2, 3, "chau")))
    )

    // ---- SRT ----

    @Test fun srt_format() = assertEquals(
        "1\n00:00:00,000 --> 00:00:03,200\nBuenas tardes, empecemos la clase.\n" +
                "\n" +
                "2\n00:00:03,200 --> 00:00:07,800\nHoy vemos el modelo de Piaget.\n",
        TranscriptExporter.toSrt(sample)
    )

    @Test fun srt_emptyList() = assertEquals("", TranscriptExporter.toSrt(emptyList()))

    @Test fun srt_numberingHasNoGapsWhenSkippingBlank() = assertEquals(
        "1\n00:00:00,000 --> 00:00:01,000\na\n\n2\n00:00:02,000 --> 00:00:03,000\nb\n",
        TranscriptExporter.toSrt(listOf(Segment(0, 1000, "a"), Segment(1000, 2000, " "), Segment(2000, 3000, "b")))
    )

    @Test fun srt_endBeforeStartIsClamped() = assertEquals(
        "1\n00:00:05,000 --> 00:00:05,000\nx\n",
        TranscriptExporter.toSrt(listOf(Segment(5000, 4000, "x")))
    )

    @Test fun srt_blankLinesInsideTextAreCollapsed() = assertEquals(
        "1\n00:00:00,000 --> 00:00:01,000\nuno\ndos\n",
        TranscriptExporter.toSrt(listOf(Segment(0, 1000, "uno\n\n  dos ")))
    )

    @Test fun srt_longAudio() = assertEquals(
        "1\n01:00:00,000 --> 01:00:02,500\nfin\n",
        TranscriptExporter.toSrt(listOf(Segment(3_600_000, 3_602_500, "fin")))
    )
}
